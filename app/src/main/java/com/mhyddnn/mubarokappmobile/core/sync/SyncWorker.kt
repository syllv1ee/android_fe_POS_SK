
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
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.fieldErrorsOf
import com.mhyddnn.mubarokappmobile.feature.visit.data.VisitDao
import com.mhyddnn.mubarokappmobile.feature.visit.data.VisitDto
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
 * Memicu SyncWorker lewat WorkManager.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(
        policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
    ) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints(requiredNetworkType = NetworkType.CONNECTED),
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30,
                TimeUnit.SECONDS,
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "sync-queue"
    }
}

/**
 * Mengirim antrean sinkronisasi milik pengguna yang sedang login.
 *
 * - 2xx: antrean dihapus setelah rekonsiliasi cache Visit berhasil.
 * - Gangguan jaringan: worker mencoba kembali.
 * - 401: berhenti jika sesi perlu login ulang.
 * - 4xx lain: antrean ditandai FAILED.
 * - Pergantian akun: worker berhenti.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val gson = Gson()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun syncQueueDao(): SyncQueueDao
        fun syncQueueApi(): SyncQueueApi
        fun sessionManager(): SessionManager
        fun visitDao(): VisitDao
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(
            applicationContext,
            Deps::class.java,
        )

        val sessionManager = deps.sessionManager()
        val dao = deps.syncQueueDao()
        val visitDao = deps.visitDao()

        val owner = sessionManager.session.first()?.username
            ?: return Result.success()

        while (true) {
            if (sessionManager.session.first()?.username != owner) {
                return Result.success()
            }

            if (sessionManager.needsRelogin.first()) {
                return Result.success()
            }

            val item = dao.nextPending(owner)
                ?: return Result.success()

            val response = try {
                val body = item.body.toRequestBody(JSON)

                if (item.method == SyncQueueEntity.METHOD_PATCH) {
                    deps.syncQueueApi().patch(
                        item.path,
                        item.id,
                        body,
                    )
                } else {
                    deps.syncQueueApi().post(
                        item.path,
                        item.id,
                        body,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                return Result.retry()
            } catch (e: Exception) {
                dao.markFailed(
                    item.id,
                    "Data tidak bisa dikirim: ${e.javaClass.simpleName}",
                )
                continue
            }

            when (outcomeOf(response.code())) {
                SendOutcome.DONE -> {
                    /*
                     * Visit memerlukan pembaruan cache setelah server menerima data.
                     * Antrean baru dihapus setelah proses tersebut berhasil.
                     *
                     * Fitur selain Visit tetap menggunakan alur lama.
                     */
                    if (
                        item.kind == KIND_VISIT_CREATE ||
                        item.kind == KIND_VISIT_STATUS
                    ) {
                        val rawBody = response.body()?.string()

                        val reconciled = reconcileVisitSuccess(
                            visitDao = visitDao,
                            owner = owner,
                            item = item,
                            rawBody = rawBody,
                        )

                        if (!reconciled) {
                            return Result.retry()
                        }
                    }

                    dao.delete(item)
                }

                SendOutcome.AUTH -> {
                    if (!sessionManager.needsRelogin.first()) {
                        return Result.retry()
                    }
                }

                SendOutcome.RETRY -> return Result.retry()

                SendOutcome.REJECTED -> {
                    dao.markFailed(
                        item.id,
                        errorMessage(
                            response.errorBody()?.string(),
                            response.code(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Memperbarui cache Visit berdasarkan respons sukses dari server.
     *
     * Kunjungan yang dibuat offline tetap menggunakan localId negatif.
     * ID dari server disimpan pada serverId agar halaman detail tetap
     * bisa menggunakan ID lokal yang sudah dipakai aplikasi.
     */
    private suspend fun reconcileVisitSuccess(
        visitDao: VisitDao,
        owner: String,
        item: SyncQueueEntity,
        rawBody: String?,
    ): Boolean {
        val dto = parseVisitResponse(rawBody)
            ?: return false

        when (item.kind) {
            KIND_VISIT_CREATE -> {
                val cached = visitDao.getByIdempotencyKey(
                    owner = owner,
                    key = item.id,
                ) ?: return true

                visitDao.markAsSynced(
                    owner = owner,
                    localId = cached.localId,
                    serverId = dto.id,
                    json = gson.toJson(dto),
                    syncStatus = "SYNCED",
                )
            }

            KIND_VISIT_STATUS -> {
                val cached = visitDao.getByServerId(
                    owner = owner,
                    serverId = dto.id,
                ) ?: return true

                visitDao.markAsSynced(
                    owner = owner,
                    localId = cached.localId,
                    serverId = dto.id,
                    json = gson.toJson(dto),
                    syncStatus = "SYNCED",
                )
            }
        }

        return true
    }

    /**
     * Membaca VisitDto dari envelope API:
     * {
     *   "success": true,
     *   "data": { ... }
     * }
     */
    private fun parseVisitResponse(rawBody: String?): VisitDto? {
        if (rawBody.isNullOrBlank()) return null

        return runCatching {
            val root = JsonParser.parseString(rawBody).asJsonObject
            val data = root.get("data")

            if (data == null || data.isJsonNull || !data.isJsonObject) {
                return null
            }

            gson.fromJson(
                data,
                VisitDto::class.java,
            )
        }.getOrNull()
    }

    internal enum class SendOutcome {
        DONE,
        RETRY,
        AUTH,
        REJECTED,
    }

    internal companion object {
        const val KIND_VISIT_CREATE = "visit-create"
        const val KIND_VISIT_STATUS = "visit-status"

        fun outcomeOf(code: Int): SendOutcome = when (code) {
            in 200..299 -> SendOutcome.DONE
            401 -> SendOutcome.AUTH
            408, 429, in 500..599 -> SendOutcome.RETRY
            else -> SendOutcome.REJECTED
        }

        val JSON = "application/json; charset=utf-8".toMediaType()

        fun errorMessage(
            raw: String?,
            code: Int,
        ): String {
            val fallback = "Ditolak server (kode $code)"

            val json = runCatching {
                JsonParser.parseString(raw.orEmpty()).asJsonObject
            }.getOrNull() ?: return fallback

            val message = json.get("message")
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                ?.ifBlank { null }
                ?: fallback

            val fields = fieldErrorsOf(json.get("errors"))
                .orEmpty()
                .map { (field, msg) -> "$field: $msg" }

            return (listOf(message) + fields).joinToString("\n")
        }
    }
}
