package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mhyddnn.mubarokappmobile.core.common.Page
import com.mhyddnn.mubarokappmobile.core.network.Meta
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueEntity
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanKasbon
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanMasterData
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanOption
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanPersonnel
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanProduct

data class PlanPageDto(
    val items: List<PlanDto>? = null,
    val meta: Meta? = null,
)

data class PlanDto(
    val id: Int,
    val invoice: String,
    val status: String,
    @SerializedName("tgl_invoice") val tglInvoice: String? = null,
    @SerializedName("tgl_pergi_kanvas") val tglPergiKanvas: String? = null,
    @SerializedName("tgl_kembali_kanvas") val tglKembaliKanvas: String? = null,
    val note: String? = null,
    val revision: Int? = null,
    @SerializedName("revisi_to") val revisiTo: String? = null,
    val vehicle: PlanVehicleDto? = null,
    val marketArea: PlanNamedDto? = null,
    val salesKanvasPersonil: List<PlanPersonnelDto>? = null,
    val salesKanvasPlanDetail: List<PlanProductDto>? = null,
    val salesKanvasKasbonMaterial: List<PlanKasbonDto>? = null,
)

data class PlanVehicleDto(val name: String? = null, val nopol: String? = null)

data class PlanNamedDto(val name: String? = null)

data class PlanPersonnelDto(val position: String? = null, val employee: PlanNamedDto? = null)

data class PlanProductDto(val qty: Double? = null, val product: PlanProductInfoDto? = null)

data class PlanProductInfoDto(val code: String? = null, val name: String? = null)

data class PlanKasbonDto(
    val price: String? = null,
    val qty: Int? = null,
    val kasbonMaterialType: PlanNamedDto? = null,
)

/** Satu record master (market area, kendaraan, karyawan, produk, jenis kasbon); field lain diabaikan. */
data class MasterOptionDto(
    val id: Int,
    val name: String? = null,
    val code: String? = null,
    val nopol: String? = null,
)

fun MasterOptionDto.toOption(): PlanOption =
    PlanOption(id, listOfNotNull(code, name, nopol).joinToString(" · ").ifBlank { "#$id" })

/** Isi `master_data_cache.json`: respons mentah kelima endpoint master. */
data class PlanMasterCacheDto(
    val marketAreas: List<MasterOptionDto>? = null,
    val vehicles: List<MasterOptionDto>? = null,
    val employees: List<MasterOptionDto>? = null,
    val products: List<MasterOptionDto>? = null,
    val kasbonTypes: List<MasterOptionDto>? = null,
)

fun PlanMasterCacheDto.toDomain(offlineMessage: String? = null): PlanMasterData = PlanMasterData(
    marketAreas = marketAreas.orEmpty().map(MasterOptionDto::toOption),
    vehicles = vehicles.orEmpty().map(MasterOptionDto::toOption),
    employees = employees.orEmpty().map(MasterOptionDto::toOption),
    products = products.orEmpty().map(MasterOptionDto::toOption),
    kasbonTypes = kasbonTypes.orEmpty().map(MasterOptionDto::toOption),
    offlineMessage = offlineMessage,
)

/** Body `POST /plan` (endpoint_plan.md §3). Skema BE `strict`: jangan tambah field. */
data class CreatePlanRequest(
    @SerializedName("tgl_invoice") val tglInvoice: String,
    @SerializedName("tgl_pergi_kanvas") val tglPergiKanvas: String,
    @SerializedName("tgl_kembali_kanvas") val tglKembaliKanvas: String?,
    @SerializedName("market_area_id") val marketAreaId: Int,
    @SerializedName("vehicle_id") val vehicleId: Int,
    val note: String?,
    val salesKanvasPlanPersonil: List<PersonnelRequest>,
    val salesKanvasPlanDetail: List<ProductRequest>,
    val salesKanvasKasbonMaterial: List<KasbonRequest>,
)

data class PersonnelRequest(@SerializedName("employee_id") val employeeId: Int, val position: String)

data class ProductRequest(@SerializedName("product_id") val productId: Int, val qty: Int)

data class KasbonRequest(
    @SerializedName("kasbon_material_type_id") val typeId: Int,
    val price: Long,
    val qty: Int,
)

fun NewPlan.toRequest(): CreatePlanRequest = CreatePlanRequest(
    tglInvoice = invoiceDate,
    tglPergiKanvas = departDate,
    tglKembaliKanvas = returnDate,
    marketAreaId = marketAreaId,
    vehicleId = vehicleId,
    note = note,
    salesKanvasPlanPersonil = personnel.map { PersonnelRequest(it.employeeId, it.position) },
    salesKanvasPlanDetail = products.map { ProductRequest(it.productId, it.qty) },
    salesKanvasKasbonMaterial = kasbon.map { KasbonRequest(it.typeId, it.price, it.qty) },
)

/** Body `PATCH /plan/:id/update-status`. */
data class UpdatePlanStatusRequest(val status: String)

fun PlanPageDto.toPage(): Page<PlanDto> {
    val list = items.orEmpty()
    return Page(
        items = list,
        page = meta?.page ?: 1,
        limit = meta?.limit ?: list.size,
        total = meta?.total ?: list.size,
        totalPages = meta?.totalPages ?: 1,
    )
}

fun PlanDto.toDomain(): Plan = Plan(
    id = id,
    invoice = invoice,
    status = status,
    invoiceDate = tglInvoice,
    departDate = tglPergiKanvas,
    returnDate = tglKembaliKanvas,
    note = note,
    revision = revision ?: 0,
    revisedTo = revisiTo,
    vehicle = vehicle?.let { listOfNotNull(it.name, it.nopol).joinToString(" · ") }?.ifBlank { null },
    marketArea = marketArea?.name,
    personnel = salesKanvasPersonil.orEmpty().map {
        PlanPersonnel(name = it.employee?.name ?: "-", position = it.position ?: "-")
    },
    products = salesKanvasPlanDetail.orEmpty().map {
        PlanProduct(code = it.product?.code, name = it.product?.name ?: "-", qty = it.qty ?: 0.0)
    },
    kasbon = salesKanvasKasbonMaterial.orEmpty().map {
        PlanKasbon(
            name = it.kasbonMaterialType?.name ?: "-",
            price = it.price?.toBigDecimalOrNull()?.toLong() ?: 0L,
            qty = it.qty ?: 0,
        )
    },
)

private val gson = Gson()

/** Antrean sinkron `POST plan` → ringkasan untuk daftar; label diambil dari [master] (null = tampil tanpa label). */
fun SyncQueueEntity.toPendingPlan(master: PlanMasterData?): PendingPlan {
    val req = gson.fromJson(body, CreatePlanRequest::class.java)
    return PendingPlan(
        id = id,
        departDate = req.tglPergiKanvas,
        marketArea = master?.marketAreas?.firstOrNull { it.id == req.marketAreaId }?.label,
        vehicle = master?.vehicles?.firstOrNull { it.id == req.vehicleId }?.label,
        note = req.note,
        failedMessage = error.takeIf { status == SyncQueueEntity.STATUS_FAILED },
    )
}

/**
 * Path antrean sinkron `plan/{id}/update-status` -> id plan-nya. Bergantung pada format path yang ditulis
 * `PlanRepositoryImpl.updateStatus` — ubah keduanya bersamaan.
 */
fun SyncQueueEntity.toPendingPlanStatus(): PendingPlanStatus = PendingPlanStatus(
    id = id,
    planId = path.split('/')[1].toInt(),
    status = gson.fromJson(body, UpdatePlanStatusRequest::class.java).status,
    failedMessage = error.takeIf { status == SyncQueueEntity.STATUS_FAILED },
)
