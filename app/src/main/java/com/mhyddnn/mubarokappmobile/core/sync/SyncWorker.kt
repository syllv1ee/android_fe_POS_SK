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
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Memicu [SyncWorker]. Aman dipanggil berkali-kali (unique work). */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            // WorkManager menunggu sampai ada jaringan — tidak perlu cek sinyal manual.
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            // ponytail: linear 30s, 60s, 90s… (maks 5 jam bawaan WorkManager). Eksponensial
            // terlalu cepat membesar untuk sinyal yang putus-nyambung.
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        // APPEND: item baru yang masuk saat worker sedang jalan tetap dapat giliran kirim.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "outbox-sync"
    }
}

/**
 * Mengirim isi outbox satu per satu (tertua dulu) milik user yang sedang login.
 *
 * - 2xx → hapus dari outbox.
 * - Tanpa jaringan / timeout / 408 / 429 / 5xx → `retry()` (data tetap aman).
 * - 401 dan refresh token ditolak → sesi ditandai `needsRelogin`, sinkron berhenti sampai
 *   user login ulang ([SyncScheduler.schedule] dipanggil lagi setelah login).
 * - 4xx lain (validasi, izin, FK) → item ditandai FAILED + pesan server, tidak diulang otomatis.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun outboxDao(): OutboxDao
        fun outboxApi(): OutboxApi
        fun sessionManager(): SessionManager
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val sessionManager = deps.sessionManager()
        val dao = deps.outboxDao()
        val owner = sessionManager.session.first()?.username ?: return Result.success()

        while (true) {
            if (sessionManager.needsRelogin.first()) return Result.success()
            val item = dao.nextPending(owner) ?: return Result.success()

            val response = try {
                deps.outboxApi().post(item.path, item.id, item.body.toRequestBody(JSON))
            } catch (_: IOException) {
                return Result.retry()
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

    internal enum class SendOutcome { DONE, RETRY, AUTH, REJECTED }

    internal companion object {
        /** Satu-satunya tempat yang memutuskan item dihapus, diulang, atau ditandai gagal. */
        fun outcomeOf(code: Int): SendOutcome = when (code) {
            in 200..299 -> SendOutcome.DONE
            401 -> SendOutcome.AUTH
            408, 429, in 500..599 -> SendOutcome.RETRY
            else -> SendOutcome.REJECTED
        }

        val JSON = "application/json; charset=utf-8".toMediaType()

        /** `message` + pesan per field (`errors`) dari envelope error backend. */
        fun errorMessage(raw: String?, code: Int): String {
            val json = runCatching { JSONObject(raw.orEmpty()) }.getOrNull()
                ?: return "Ditolak server (kode $code)"
            val fields = json.optJSONObject("errors")?.let { errors ->
                errors.keys().asSequence().map { "$it: ${errors.optString(it)}" }.toList()
            }.orEmpty()
            val message = json.optString("message").ifBlank { "Ditolak server (kode $code)" }
            return (listOf(message) + fields).joinToString("\n")
        }
    }
}
