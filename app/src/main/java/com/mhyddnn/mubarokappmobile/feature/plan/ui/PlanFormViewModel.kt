
package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlan
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

data class OutletRow(
    val outletId: Int? = null,
)

data class PlanFormUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val master: PlanMasterData? = null,
    val employeeId: Int? = null,
    val departureDate: String = Formatters.toApiDate(LocalDate.now()),
    val returnDate: String? = null,
    val marketAreaId: Int? = null,
    val vehicleId: Int? = null,
    val outlets: List<OutletRow> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val createdPlanId: Int? = null,
)

private const val REQUIRED = "Wajib diisi"

internal fun PlanFormUiState.toNewPlan(): Pair<NewPlan?, Map<String, String>> {
    val selectedOutletIds = outlets.mapNotNull { it.outletId }

    val errors = buildMap {
        if (employeeId == null) {
            put("employee_id", REQUIRED)
        }

        if (departureDate.isBlank()) {
            put("departure_date", REQUIRED)
        }

        if (selectedOutletIds.size != selectedOutletIds.distinct().size) {
            put("outlet_ids", "Outlet tidak boleh dipilih dua kali")
        }
    }

    if (errors.isNotEmpty()) return null to errors

    return NewPlan(
        employeeId = employeeId!!,
        vehicleId = vehicleId,
        marketAreaId = marketAreaId,
        departureDate = departureDate,
        returnDate = returnDate?.takeIf { it.isNotBlank() },
        outletIds = selectedOutletIds,
    ) to emptyMap()
}

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
        _uiState.update {
            it.copy(
                isLoading = it.master == null,
                loadError = null,
            )
        }

        viewModelScope.launch {
            if (_uiState.value.master == null) {
                repository.cachedMasterData()?.let { cached ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            master = cached,
                        )
                    }
                }
            }

            when (val result = repository.getMasterData()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            master = result.data,
                            loadError = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    fun edit(transform: (PlanFormUiState) -> PlanFormUiState) {
        _uiState.update(transform)
    }

    fun submit() {
        val state = _uiState.value

        if (state.isSaving) return

        val (plan, errors) = state.toNewPlan()

        if (plan == null) {
            _uiState.update {
                it.copy(
                    fieldErrors = errors,
                    errorMessage = "Periksa kembali isian yang wajib diisi.",
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                fieldErrors = emptyMap(),
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            when (val result = repository.createPlan(plan)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            createdPlanId = result.data.id,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.message,
                            fieldErrors = result.fieldErrors.orEmpty(),
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }
}
