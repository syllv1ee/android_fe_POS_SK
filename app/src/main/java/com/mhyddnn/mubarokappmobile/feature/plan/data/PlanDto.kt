
package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueEntity
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanMasterData
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanOption

// ============================================================
// PLAN RESPONSE DTO
// ============================================================

data class PlanDto(
    val id: Int = 0,

    @SerializedName("plan_no")
    val planNo: String? = null,

    val status: String = "DRAFT",

    @SerializedName("departure_date")
    val departureDate: String? = null,

    @SerializedName("return_date")
    val returnDate: String? = null,

    val employee: PlanEmployeeDto? = null,

    val vehicle: PlanVehicleDto? = null,

    @SerializedName("marketArea")
    val marketArea: PlanNamedDto? = null,

    val outlets: List<PlanOutletRelationDto>? = null,
)

data class PlanEmployeeDto(
    val id: Int? = null,
    val name: String? = null,
)

data class PlanVehicleDto(
    val id: Int? = null,

    @SerializedName("plate_number")
    val plateNumber: String? = null,
)

data class PlanNamedDto(
    val id: Int? = null,
    val name: String? = null,
)

data class PlanOutletRelationDto(
    val outlet: PlanNamedDto? = null,
)

// ============================================================
// MASTER DATA DTO
// ============================================================

data class MasterOptionDto(
    val id: Int = 0,
    val name: String? = null,
    val code: String? = null,

    @SerializedName("plate_number")
    val plateNumber: String? = null,
)

fun MasterOptionDto.toOption(): PlanOption {
    val label = listOfNotNull(
        code,
        name,
        plateNumber,
    ).distinct().joinToString(" · ")

    return PlanOption(
        id = id,
        label = label.ifBlank { "#$id" },
    )
}

data class PlanMasterCacheDto(
    val marketAreas: List<MasterOptionDto>? = null,
    val vehicles: List<MasterOptionDto>? = null,
    val employees: List<MasterOptionDto>? = null,
    val outlets: List<MasterOptionDto>? = null,
)

fun PlanMasterCacheDto.toDomain(
    offlineMessage: String? = null,
): PlanMasterData = PlanMasterData(
    marketAreas = marketAreas.orEmpty().map { it.toOption() },
    vehicles = vehicles.orEmpty().map { it.toOption() },
    employees = employees.orEmpty().map { it.toOption() },
    outlets = outlets.orEmpty().map { it.toOption() },
    offlineMessage = offlineMessage,
)

// ============================================================
// CREATE PLAN REQUEST
// ============================================================

data class CreatePlanRequest(
    @SerializedName("employee_id")
    val employeeId: Int,

    @SerializedName("vehicle_id")
    val vehicleId: Int?,

    @SerializedName("market_area_id")
    val marketAreaId: Int?,

    @SerializedName("departure_date")
    val departureDate: String,

    @SerializedName("return_date")
    val returnDate: String?,

    @SerializedName("outlet_ids")
    val outletIds: List<Int>,
)

fun NewPlan.toRequest(): CreatePlanRequest = CreatePlanRequest(
    employeeId = employeeId,
    vehicleId = vehicleId,
    marketAreaId = marketAreaId,
    departureDate = departureDate,
    returnDate = returnDate,
    outletIds = outletIds,
)

// ============================================================
// WORKFLOW REQUEST
// ============================================================

data class UpdatePlanStatusRequest(
    val status: String,
)

data class CompletePlanRequest(
    @SerializedName("warehouse_id")
    val warehouseId: Int? = null,
)

// ============================================================
// DTO TO DOMAIN
// ============================================================

fun PlanDto.toDomain(): Plan {
    val number = planNo
        ?.takeIf { it.isNotBlank() }
        ?: "PLAN-$id"

    return Plan(
        id = id,
        invoice = number,
        planNo = number,
        status = status,
        invoiceDate = null,
        departDate = departureDate,
        returnDate = returnDate,
        note = null,
        revision = 0,
        revisedTo = null,
        vehicle = vehicle?.plateNumber,
        marketArea = marketArea?.name,
        personnel = emptyList(),
        products = emptyList(),
        kasbon = emptyList(),
        employee = employee?.name,
        outlets = outlets.orEmpty().mapNotNull {
            it.outlet?.name
        },
    )
}

// ============================================================
// SYNC QUEUE MAPPING
// ============================================================

private val gson = Gson()

fun SyncQueueEntity.toPendingPlan(
    master: PlanMasterData?,
): PendingPlan {
    val request = gson.fromJson(
        body,
        CreatePlanRequest::class.java,
    )

    return PendingPlan(
        id = id,
        departDate = request.departureDate,
        marketArea = master?.marketAreas
            ?.firstOrNull {
                it.id == request.marketAreaId
            }
            ?.label,
        vehicle = master?.vehicles
            ?.firstOrNull {
                it.id == request.vehicleId
            }
            ?.label,
        note = null,
        failedMessage = error.takeIf {
            status == SyncQueueEntity.STATUS_FAILED
        },
    )
}

fun SyncQueueEntity.toPendingPlanStatus(): PendingPlanStatus {
    val planId = Regex("""sales-kanvas-plans/(\d+)/""")
        .find(path)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: Regex("""plan/(\d+)/""")
            .find(path)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        ?: 0

    return PendingPlanStatus(
        id = id,
        planId = planId,
        status = gson.fromJson(
            body,
            UpdatePlanStatusRequest::class.java,
        ).status,
        failedMessage = error.takeIf {
            status == SyncQueueEntity.STATUS_FAILED
        },
    )
}
