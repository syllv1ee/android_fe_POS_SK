package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.plan.domain.CreatePlanOutcome
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlanKasbon
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlanPersonnel
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlanProduct
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanMasterData
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PersonnelRow(val employeeId: Int? = null, val position: String = "Sales")

data class ProductRow(val productId: Int? = null, val qty: Int = 1)

data class KasbonRow(val typeId: Int? = null, val price: Long = 0, val qty: Int = 1)

data class PlanFormUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val master: PlanMasterData? = null,
    val invoiceDate: String = Formatters.toApiDate(LocalDate.now()),
    val departDate: String? = null,
    val returnDate: String? = null,
    val marketAreaId: Int? = null,
    val vehicleId: Int? = null,
    val note: String = "",
    val personnel: List<PersonnelRow> = listOf(PersonnelRow(position = "Sopir")),
    val products: List<ProductRow> = listOf(ProductRow()),
    val kasbon: List<KasbonRow> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Kunci = path field BE (mis. `vehicle_id`, `salesKanvasPlanDetail.0.qty`); dari validasi lokal maupun server. */
    val fieldErrors: Map<String, String> = emptyMap(),
    /** Terisi setelah server menerima → layar pindah ke detail. */
    val createdPlanId: Int? = null,
    /** true = tanpa sinyal, plan masuk antrean → layar kembali ke daftar (plan tampil "Belum terkirim"). */
    val queued: Boolean = false,
)

private const val REQUIRED = "Wajib diisi"

/** Validasi lokal (cermin skema BE). Error kosong ⇔ [NewPlan] tidak null. */
internal fun PlanFormUiState.toNewPlan(): Pair<NewPlan?, Map<String, String>> {
    val errors = buildMap {
        if (departDate == null) put("tgl_pergi_kanvas", REQUIRED)
        if (marketAreaId == null) put("market_area_id", REQUIRED)
        if (vehicleId == null) put("vehicle_id", REQUIRED)
        personnel.forEachIndexed { i, r ->
            if (r.employeeId == null) put("salesKanvasPlanPersonil.$i.employee_id", REQUIRED)
        }
        products.forEachIndexed { i, r ->
            if (r.productId == null) put("salesKanvasPlanDetail.$i.product_id", REQUIRED)
            if (r.qty <= 0) put("salesKanvasPlanDetail.$i.qty", "Harus > 0")
        }
        kasbon.forEachIndexed { i, r ->
            if (r.typeId == null) put("salesKanvasKasbonMaterial.$i.kasbon_material_type_id", REQUIRED)
            if (r.price <= 0) put("salesKanvasKasbonMaterial.$i.price", "Harus > 0")
            if (r.qty <= 0) put("salesKanvasKasbonMaterial.$i.qty", "Harus > 0")
        }
    }
    if (errors.isNotEmpty()) return null to errors
    return NewPlan(
        invoiceDate = invoiceDate,
        departDate = departDate!!,
        returnDate = returnDate,
        marketAreaId = marketAreaId!!,
        vehicleId = vehicleId!!,
        note = note.trim().ifBlank { null },
        personnel = personnel.map { NewPlanPersonnel(it.employeeId!!, it.position) },
        products = products.map { NewPlanProduct(it.productId!!, it.qty) },
        kasbon = kasbon.map { NewPlanKasbon(it.typeId!!, it.price, it.qty) },
    ) to emptyMap()
}

/**
 * Form tambah plan. Alur: [loadMaster] (server atau cache HP) → user mengisi lewat [edit] →
 * [submit]: validasi lokal → kirim. Hasil: [PlanFormUiState.createdPlanId] (diterima server →
 * buka detail), [PlanFormUiState.queued] (offline → kembali ke daftar), atau error per field.
 * State form hidup di ViewModel, jadi isian tidak hilang saat rotasi layar.
 */
@HiltViewModel
class PlanFormViewModel @Inject constructor(
    private val repository: PlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanFormUiState())
    val uiState: StateFlow<PlanFormUiState> = _uiState.asStateFlow()

    init {
        loadMaster()
    }

    fun loadMaster() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.getMasterData()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, master = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, loadError = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    // ponytail: satu pintu edit form (state immutable) daripada satu fungsi per field.
    fun edit(transform: (PlanFormUiState) -> PlanFormUiState) = _uiState.update(transform)

    /**
     * Validasi lokal dulu (hemat request & bisa offline); lolos → [PlanRepository.createPlan].
     * Error server per field memakai kunci yang sama dengan validasi lokal, jadi tampil di field
     * yang sama. Diabaikan bila sedang menyimpan (cegah tap ganda).
     */
    fun submit() {
        val state = _uiState.value
        if (state.isSaving) return
        val (plan, errors) = state.toNewPlan()
        if (plan == null) {
            _uiState.update { it.copy(fieldErrors = errors, errorMessage = "Lengkapi isian yang ditandai.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, fieldErrors = emptyMap(), errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.createPlan(plan)) {
                is Result.Success -> _uiState.update {
                    when (val outcome = result.data) {
                        is CreatePlanOutcome.Created -> it.copy(isSaving = false, createdPlanId = outcome.plan.id)
                        // isSaving dibiarkan true agar tombol tidak bisa ditekan lagi saat layar ditutup.
                        CreatePlanOutcome.Queued -> it.copy(queued = true)
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.message,
                        fieldErrors = result.fieldErrors.orEmpty(),
                    )
                }
                Result.Loading -> Unit
            }
        }
    }
}
