package com.mhyddnn.mubarokappmobile.feature.plan.domain

data class Plan(
    val id: Int,
    val invoice: String,
    val status: String,
    val invoiceDate: String?,
    val departDate: String?,
    val returnDate: String?,
    val note: String?,
    val revision: Int,
    /** Invoice plan pengganti bila plan ini sudah direvisi. */
    val revisedTo: String?,
    val vehicle: String?,
    val marketArea: String?,
    val personnel: List<PlanPersonnel>,
    val products: List<PlanProduct>,
    val kasbon: List<PlanKasbon>,
)

data class PlanPersonnel(val name: String, val position: String)

data class PlanProduct(val code: String?, val name: String, val qty: Double)

data class PlanKasbon(val name: String, val price: Long, val qty: Int)

/** Pilihan dropdown dari data master. */
data class PlanOption(val id: Int, val label: String)

data class PlanMasterData(
    val marketAreas: List<PlanOption>,
    val vehicles: List<PlanOption>,
    val employees: List<PlanOption>,
    val products: List<PlanOption>,
    val kasbonTypes: List<PlanOption>,
    /** Terisi bila diambil dari HP karena server tak terjangkau (alasannya). */
    val offlineMessage: String? = null,
)

/** Hasil simpan plan baru. */
sealed interface CreatePlanOutcome {
    /** Diterima server langsung. */
    data class Created(val plan: Plan) : CreatePlanOutcome

    /** Tidak ada sinyal → masuk antrean, dikirim otomatis (lihat [PendingPlan]). */
    data object Queued : CreatePlanOutcome
}

/** Plan baru yang dibuat di HP tapi belum diterima server — belum punya id & invoice. */
data class PendingPlan(
    /** Id antrean sinkron (= Idempotency-Key). */
    val id: String,
    val departDate: String,
    val marketArea: String?,
    val vehicle: String?,
    val note: String?,
    /** null = menunggu sinyal; terisi = ditolak server. */
    val failedMessage: String?,
)

data class NewPlan(
    val invoiceDate: String,
    val departDate: String,
    val returnDate: String?,
    val marketAreaId: Int,
    val vehicleId: Int,
    val note: String?,
    val personnel: List<NewPlanPersonnel>,
    val products: List<NewPlanProduct>,
    val kasbon: List<NewPlanKasbon>,
)

data class NewPlanPersonnel(val employeeId: Int, val position: String)

data class NewPlanProduct(val productId: Int, val qty: Int)

data class NewPlanKasbon(val typeId: Int, val price: Long, val qty: Int)

/** Perubahan status yang dibuat di HP tapi belum diterima server. */
data class PendingPlanStatus(
    val id: String,
    val planId: Int,
    val status: String,
    /** null = menunggu sinyal; terisi = ditolak server. */
    val failedMessage: String?,
)

object PlanStatus {
    const val DRAFT = "DRAFT"
    const val APPROVED = "DISETUJUI"
    const val LOADING = "LOADING"
    const val COMPLETE = "KOMPLIT"
    const val CANCELLED = "BATAL"

    /** `REVISI` sengaja tidak ada: tidak dipakai BE. BE tidak membatasi urutan transisi. */
    val selectable = listOf(DRAFT, APPROVED, LOADING, COMPLETE, CANCELLED)
}

/**
 * Status yang ditampilkan: perubahan terbaru yang masih menunggu sinkron, atau status server.
 * [pending] urut terbaru dulu (seperti dari repository).
 */
fun Plan.displayStatus(pending: List<PendingPlanStatus>): String =
    pending.firstOrNull { it.planId == id && it.failedMessage == null }?.status ?: status

/**
 * Perubahan status yang hilang dari antrean tanpa ditolak = sudah diterima server (item gagal
 * hanya bisa dibuang setelah ditandai gagal, jadi tidak ikut). Urutan sama dengan [displayStatus].
 */
fun deliveredStatuses(before: List<PendingPlanStatus>, after: List<PendingPlanStatus>): List<PendingPlanStatus> =
    before.filter { p -> p.failedMessage == null && after.none { it.id == p.id } }

/**
 * Pasang status yang sudah diterima server ke plan di layar, agar tidak sempat kembali ke status
 * lama (cache) selama data terbaru dimuat ulang.
 */
fun Plan.withDelivered(delivered: List<PendingPlanStatus>): Plan =
    delivered.firstOrNull { it.planId == id }?.let { copy(status = it.status) } ?: this
