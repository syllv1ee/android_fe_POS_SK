package com.mhyddnn.mubarokappmobile.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.JsonParser
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.fieldErrorsOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memicu [SyncWorker] lewat WorkManager (unique work [WORK_NAME]) — aman dipanggil berkali-kali,
 * tetap jalan walau app ditutup, dan menunggu sendiri sampai ada jaringan.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * - `APPEND_OR_REPLACE` (default, setelah insert ke antrean sinkron): bila worker sedang jalan, work baru
     *   diantrekan SETELAHNYA, jadi item yang masuk di tengah pengiriman tetap terkirim. Bila work
     *   sebelumnya gagal/dibatalkan, diganti baru.
     * - `KEEP` (start app, tanpa item baru): work yang sudah menunggu dipakai ulang, tidak menambah
     *   rantai satu per buka app.
     */
    fun schedule(policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            // WorkManager menunggu sampai ada jaringan — tidak perlu cek sinyal manual.
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            // ponytail: linear 30s, 60s, 90s… (maks 5 jam bawaan WorkManager). Eksponensial
            // terlalu cepat membesar untuk sinyal yang putus-nyambung.
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "sync-queue"
    }
}

/**
 * Mengirim isi antrean sinkron satu per satu (tertua dulu) milik user yang sedang login.
 *
 * - 2xx → hapus dari antrean sinkron.
 * - Tanpa jaringan / timeout / 408 / 429 / 5xx → `retry()` (data tetap aman).
 * - 401 dan refresh token ditolak → sesi ditandai `needsRelogin`, sinkron berhenti sampai
 *   user login ulang ([SyncScheduler.schedule] dipanggil lagi setelah login).
 * - 4xx lain (validasi, izin, FK) → item ditandai FAILED + pesan server, tidak diulang otomatis.
 * - User yang login berganti di tengah jalan → berhenti; item hanya dikirim dengan token pemiliknya.
 * - Exception lain saat mengirim (item rusak) → FAILED, lanjut item berikutnya.
 *
 * `Result.retry()` berlaku untuk seluruh worker: WorkManager menjadwalkan ulang dengan backoff dan
 * loop mulai lagi dari item tertua. Bila WorkManager menghentikan worker di tengah kiriman (batas
 * waktu / jaringan hilang), item tetap PENDING dan dikirim ulang dengan `Idempotency-Key` yang sama.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    /**
     * Dependency diambil lewat Hilt EntryPoint, bukan `@HiltWorker`, agar tidak perlu `hilt-work`
     * dan konfigurasi WorkManager custom (`Configuration.Provider`).
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun syncQueueDao(): SyncQueueDao
        fun syncQueueApi(): SyncQueueApi
        fun sessionManager(): SessionManager
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val sessionManager = deps.sessionManager()
        val dao = deps.syncQueueDao()
        // Belum login → tidak ada yang boleh dikirim; dipicu lagi setelah login.
        val owner = sessionManager.session.first()?.username ?: return Result.success()

        while (true) {
            // Cek ulang tiap item: logout lalu login user lain saat worker jalan → item [owner]
            // jangan sampai terkirim dengan Bearer user baru. Sisanya dikirim saat pemiliknya login.
            if (sessionManager.session.first()?.username != owner) return Result.success()
            if (sessionManager.needsRelogin.first()) return Result.success()
            val item = dao.nextPending(owner) ?: return Result.success()

            val response = try {
                val body = item.body.toRequestBody(JSON)
                if (item.method == SyncQueueEntity.METHOD_PATCH) deps.syncQueueApi().patch(item.path, item.id, body)
                else deps.syncQueueApi().post(item.path, item.id, body)
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                return Result.retry()
            } catch (e: Exception) {
                // Item rusak (mis. path tidak valid) → tandai gagal agar tidak menahan antrean di belakangnya.
                dao.markFailed(item.id, "Data tidak bisa dikirim: ${e.javaClass.simpleName}")
                continue
            }

            when (outcomeOf(response.code())) {
                SendOutcome.DONE -> dao.delete(item)
                // Refresh gagal karena jaringan → coba lagi; ditolak server → needsRelogin (cek di atas).
                SendOutcome.AUTH -> if (!sessionManager.needsRelogin.first()) return Result.retry()
                SendOutcome.RETRY -> return Result.retry()
                SendOutcome.REJECTED ->
                    dao.markFailed(item.id, errorMessage(response.errorBody()?.string(), response.code()))
            }
        }
    }

    /** Keputusan per item dari kode HTTP; juga dipakai fitur (mis. plan) untuk memutuskan antre/tolak. */
    internal enum class SendOutcome {
        /** Diterima server → hapus. */
        DONE,

        /** Gangguan sementara → worker `retry()` dengan backoff. */
        RETRY,

        /** 401 setelah authenticator menyerah: needsRelogin (berhenti) atau refresh gagal jaringan (retry). */
        AUTH,

        /** Isi ditolak server → FAILED, menunggu user. */
        REJECTED,
    }

    internal companion object {
        /** Satu-satunya tempat yang memutuskan item dihapus, diulang, atau ditandai gagal. */
        fun outcomeOf(code: Int): SendOutcome = when (code) {
            in 200..299 -> SendOutcome.DONE
            401 -> SendOutcome.AUTH
            408, 429, in 500..599 -> SendOutcome.RETRY
            else -> SendOutcome.REJECTED
        }

        /** Content-Type body antrean sinkron (selalu JSON). */
        val JSON = "application/json; charset=utf-8".toMediaType()

        /**
         * Pesan untuk [SyncQueueEntity.error]: `message` envelope + satu baris per field (`errors`),
         * mis. `"Validation failed\nvehicle_id: wajib"`. Body bukan JSON → "Ditolak server (kode N)".
         */
        fun errorMessage(raw: String?, code: Int): String {
            val fallback = "Ditolak server (kode $code)"
            val json = runCatching { JsonParser.parseString(raw.orEmpty()).asJsonObject }.getOrNull()
                ?: return fallback
            val message = json.get("message")
                ?.takeIf { it.isJsonPrimitive }?.asString?.ifBlank { null }
                ?: fallback
            val fields = fieldErrorsOf(json.get("errors")).orEmpty().map { (field, msg) -> "$field: $msg" }
            return (listOf(message) + fields).joinToString("\n")
        }
    }
}
