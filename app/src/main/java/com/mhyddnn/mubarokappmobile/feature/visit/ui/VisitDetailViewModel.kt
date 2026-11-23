package com.mhyddnn.mubarokappmobile.feature.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.visit.domain.Visit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitDetailUiState(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val visit: Visit? = null,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class VisitDetailViewModel @Inject constructor(
    private val repository: VisitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitDetailUiState())
    val uiState: StateFlow<VisitDetailUiState> = _uiState.asStateFlow()

    fun load(id: Int) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            when (val result = repository.getVisit(id)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            visit = result.data,
                            errorMessage = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    fun updateStatus(status: String) {
        val visit = _uiState.value.visit ?: return
        if (_uiState.value.isUpdating) return
        if (visit.status != "DIRENCANAKAN") return

        _uiState.update {
            it.copy(
                isUpdating = true,
                actionMessage = null,
            )
        }

        viewModelScope.launch {
            when (val result = repository.updateStatus(visit.id, status)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            visit = result.data,
                            actionMessage = "Status kunjungan berhasil diperbarui.",
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            actionMessage = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }
}