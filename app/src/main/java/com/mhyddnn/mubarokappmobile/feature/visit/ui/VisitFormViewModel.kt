
package com.mhyddnn.mubarokappmobile.feature.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.visit.domain.NewVisit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitMarketArea
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val marketAreas: List<VisitMarketArea> = emptyList(),
    val storeName: String = "",
    val visitDate: String? = null,
    val marketAreaId: Int? = null,
    val note: String = "",
    val loadError: String? = null,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val createdVisitId: Int? = null,
)

@HiltViewModel
class VisitFormViewModel @Inject constructor(
    private val repository: VisitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitFormUiState())
    val uiState: StateFlow<VisitFormUiState> = _uiState.asStateFlow()

    init {
        loadMaster()
    }

    fun edit(transform: (VisitFormUiState) -> VisitFormUiState) {
        _uiState.update {
            transform(it).copy(
                errorMessage = null,
                fieldErrors = emptyMap(),
            )
        }
    }

    fun loadMaster() {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                loadError = null,
            )
        }

        viewModelScope.launch {
            when (val result = repository.getMarketAreas()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            marketAreas = result.data,
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

    fun submit() {
        val current = _uiState.value

        if (current.isSaving) return

        val errors = mutableMapOf<String, String>()

        if (current.storeName.isBlank()) {
            errors["store_name"] = "Nama toko wajib diisi"
        }

        if (current.storeName.length > 150) {
            errors["store_name"] = "Nama toko maksimal 150 karakter"
        }

        val date = current.visitDate
        if (date.isNullOrBlank()) {
            errors["visit_date"] = "Tanggal kunjungan wajib diisi"
        } else if (!isValidDate(date)) {
            errors["visit_date"] = "Format tanggal tidak valid"
        }

        if (current.marketAreaId == null) {
            errors["market_area_id"] = "Market area wajib dipilih"
        }

        if (current.note.length > 1000) {
            errors["note"] = "Catatan maksimal 1000 karakter"
        }

        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(fieldErrors = errors)
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessage = null,
                fieldErrors = emptyMap(),
            )
        }

        viewModelScope.launch {
            val request = NewVisit(
                storeName = current.storeName.trim(),
                visitDate = date!!,
                marketAreaId = current.marketAreaId!!,
                note = current.note.trim().takeIf { it.isNotBlank() },
            )

            when (val result = repository.createVisit(request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            createdVisitId = result.data.id,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.message,
                            fieldErrors = result.fieldErrors ?: emptyMap(),
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun isValidDate(value: String): Boolean {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(value)) {
            return false
        }

        return runCatching {
            java.time.LocalDate.parse(value)
        }.isSuccess
    }
}
