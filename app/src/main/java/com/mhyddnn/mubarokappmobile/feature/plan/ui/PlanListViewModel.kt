
package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val plans: List<Plan> = emptyList(),
    val errorMessage: String? = null,
    val offlineMessage: String? = null,
)

@HiltViewModel
class PlanListViewModel @Inject constructor(
    private val repository: PlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanListUiState())
    val uiState: StateFlow<PlanListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            val cached = repository.cachedPlans()

            if (cached.isNotEmpty() && _uiState.value.plans.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = true,
                        plans = cached,
                    )
                }
            }

            fetch()
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return

        _uiState.update {
            it.copy(
                isRefreshing = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            fetch()
        }
    }

    private suspend fun fetch() {
        when (val result = repository.getPlans()) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        plans = result.data,
                        errorMessage = null,
                        offlineMessage = null,
                    )
                }
            }

            is Result.Error -> {
                val cached = repository.cachedPlans()

                _uiState.update { state ->
                    if (cached.isNotEmpty()) {
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            plans = cached,
                            errorMessage = null,
                            offlineMessage = result.message,
                        )
                    } else {
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }

            Result.Loading -> Unit
        }
    }
}
