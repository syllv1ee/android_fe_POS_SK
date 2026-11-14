package com.mhyddnn.mubarokappmobile.feature.plan.domain

import com.mhyddnn.mubarokappmobile.core.common.Page
import com.mhyddnn.mubarokappmobile.core.common.Result
import kotlinx.coroutines.flow.Flow

interface PlanRepository {

    /** Satu halaman daftar plan. Sukses → cache lokal ikut diperbarui. */
    suspend fun getPlans(page: Int = 1, limit: Int = 10): Result<Page<Plan>>

    /** Daftar plan terakhir yang tersimpan di HP (mode offline). */
    suspend fun cachedPlans(): List<Plan>

    suspend fun cachedPlan(id: Int): Plan?

    /** Detail terbaru dari server; sukses → cache plan itu diperbarui. */
    suspend fun getPlan(id: Int): Result<Plan>

    /** Data master form plan. Offline → salinan terakhir di HP ([PlanMasterData.offlineMessage]). */
    suspend fun getMasterData(): Result<PlanMasterData>

    /** Salinan master terakhir di HP tanpa ke server; null = belum pernah dimuat online. */
    suspend fun cachedMasterData(): PlanMasterData?

    /**
     * `POST /plan`: dicoba online dulu (Error = ditolak server, mis. validasi). Gagal karena
     * sinyal/server/sesi → diantrekan ([CreatePlanOutcome.Queued]) dan dikirim saat ada sinyal.
     */
    suspend fun createPlan(plan: NewPlan): Result<CreatePlanOutcome>

    /** Plan baru yang belum diterima server, terbaru dulu. */
    val pendingPlans: Flow<List<PendingPlan>>

    /** Antrekan perubahan status; dikirim saat ada sinyal. Selalu berhasil secara lokal. */
    suspend fun updateStatus(planId: Int, status: String)

    /** Perubahan status yang belum diterima server, terbaru dulu. */
    val pendingStatuses: Flow<List<PendingPlanStatus>>

    /** Kirim ulang item antrean yang ditolak (perubahan status maupun plan baru). */
    suspend fun retryPending(id: String)

    suspend fun discardPending(id: String)
}
