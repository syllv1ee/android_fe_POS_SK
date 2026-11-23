
package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import com.mhyddnn.mubarokappmobile.feature.salescanvas.SalesCanvasRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanDetailUiState(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val plan: Plan? = null,
    val errorMessage: String? = null,
    val offlineMessage: String? = null,
)

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlanRepository,
) : ViewModel() {

    private val planId =
        savedStateHandle.toRoute<SalesCanvasRoutes.PlanDetail>().id

    private val _uiState = MutableStateFlow(PlanDetailUiState())
    val uiState: StateFlow<PlanDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val cached = if (_uiState.value.plan == null) {
                repository.cachedPlan(planId)
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    isLoading = it.plan == null && cached == null,
                    plan = it.plan ?: cached,
                    errorMessage = null,
                )
            }

            when (val result = repository.getPlan(planId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            plan = result.data,
                            errorMessage = null,
                            offlineMessage = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        if (it.plan != null) {
                            it.copy(
                                isLoading = false,
                                offlineMessage = result.message,
                            )
                        } else {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message,
                            )
                        }
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    fun approve() {
        performAction { repository.approvePlan(planId) }
    }

    fun cancel() {
        performAction { repository.cancelPlan(planId) }
    }

    fun complete() {
        performAction { repository.completePlan(planId) }
    }

    fun cancelLoading() {
        performAction { repository.cancelLoading(planId) }
    }

    private fun performAction(
        action: suspend () -> Result<Plan>,
    ) {
        if (_uiState.value.isUpdating) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    errorMessage = null,
                )
            }

            when (val result = action()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            plan = result.data,
                            offlineMessage = null,
                            errorMessage = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }
}
