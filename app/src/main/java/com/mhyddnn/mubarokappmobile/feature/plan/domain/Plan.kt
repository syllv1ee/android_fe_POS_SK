
package com.mhyddnn.mubarokappmobile.feature.plan.domain

data class Plan(
    val id: Int,
    val invoice: String = "",
    val status: String,
    val invoiceDate: String? = null,
    val departDate: String? = null,
    val returnDate: String? = null,
    val note: String? = null,
    val revision: Int = 0,
    val revisedTo: String? = null,
    val vehicle: String? = null,
    val marketArea: String? = null,
    val personnel: List<PlanPersonnel> = emptyList(),
    val products: List<PlanProduct> = emptyList(),
    val kasbon: List<PlanKasbon> = emptyList(),
    val employee: String? = null,
    val outlets: List<String> = emptyList(),
    val planNo: String = invoice,
)

data class PlanPersonnel(
    val name: String,
    val position: String,
)

data class PlanProduct(
    val code: String?,
    val name: String,
    val qty: Double,
)

data class PlanKasbon(
    val name: String,
    val price: Long,
    val qty: Int,
)

data class PlanOption(
    val id: Int,
    val label: String,
)

data class PlanMasterData(
    val marketAreas: List<PlanOption> = emptyList(),
    val vehicles: List<PlanOption> = emptyList(),
    val employees: List<PlanOption> = emptyList(),
    val outlets: List<PlanOption> = emptyList(),
    val offlineMessage: String? = null,
)

sealed interface CreatePlanOutcome {
    data class Created(val plan: Plan) : CreatePlanOutcome
    data object Queued : CreatePlanOutcome
}

data class PendingPlan(
    val id: String,
    val departDate: String,
    val marketArea: String?,
    val vehicle: String?,
    val note: String?,
    val failedMessage: String?,
)

data class NewPlan(
    val employeeId: Int,
    val vehicleId: Int?,
    val marketAreaId: Int?,
    val departureDate: String,
    val returnDate: String?,
    val outletIds: List<Int>,
)

data class PendingPlanStatus(
    val id: String,
    val planId: Int,
    val status: String,
    val failedMessage: String?,
)

object PlanStatus {
    const val DRAFT = "DRAFT"
    const val APPROVED = "DISETUJUI"
    const val LOADING = "LOADING"
    const val COMPLETE = "KOMPLIT"
    const val CANCELLED = "BATAL"

    val selectable = listOf(
        DRAFT,
        APPROVED,
        LOADING,
        COMPLETE,
        CANCELLED,
    )
}

fun Plan.displayStatus(
    pending: List<PendingPlanStatus>,
): String =
    pending.firstOrNull {
        it.planId == id && it.failedMessage == null
    }?.status ?: status

fun deliveredStatuses(
    before: List<PendingPlanStatus>,
    after: List<PendingPlanStatus>,
): List<PendingPlanStatus> =
    before.filter { item ->
        item.failedMessage == null &&
                after.none { it.id == item.id }
    }

fun Plan.withDelivered(
    delivered: List<PendingPlanStatus>,
): Plan =
    delivered.firstOrNull { it.planId == id }
        ?.let { copy(status = it.status) }
        ?: this
