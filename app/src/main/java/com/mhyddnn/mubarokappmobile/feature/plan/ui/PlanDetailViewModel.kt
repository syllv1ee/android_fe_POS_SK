package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import com.mhyddnn.mubarokappmobile.feature.plan.domain.deliveredStatuses
import com.mhyddnn.mubarokappmobile.feature.plan.domain.withDelivered
import com.mhyddnn.mubarokappmobile.feature.salescanvas.SalesCanvasRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanDetailUiState(
    val isLoading: Boolean = true,
    val plan: Plan? = null,
    val errorMessage: String? = null,
    /** Terisi bila detail dari cache HP karena server tak terjangkau. */
    val offlineMessage: String? = null,
    /** Perubahan status plan ini yang belum diterima server, terbaru dulu. */
    val pending: List<PendingPlanStatus> = emptyList(),
)

/** Tampilkan cache dulu (instan & offline), lalu perbarui dari server bila bisa. */
@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlanRepository,
) : ViewModel() {

    private val planId = savedStateHandle.toRoute<SalesCanvasRoutes.PlanDetail>().id

    private val _uiState = MutableStateFlow(PlanDetailUiState())
    val uiState: StateFlow<PlanDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        repository.pendingStatuses
            .onEach { all ->
                val list = all.filter { p -> p.planId == planId }
                val delivered = deliveredStatuses(before = _uiState.value.pending, after = list)
                _uiState.update { it.copy(pending = list, plan = it.plan?.withDelivered(delivered)) }
                // Sudah diterima server → ambil detail terbaru (juga memperbarui cache untuk offline).
                if (delivered.isNotEmpty()) load()
            }
            .launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            // Cache hanya untuk tampilan pertama: data di layar selalu sama/lebih baru dari cache.
            val cached = if (_uiState.value.plan == null) repository.cachedPlan(planId) else null
            _uiState.update {
                val plan = it.plan ?: cached
                it.copy(isLoading = plan == null, plan = plan, errorMessage = null)
            }
            when (val result = repository.getPlan(planId)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, plan = result.data, offlineMessage = null)
                }
                is Result.Error -> _uiState.update {
                    if (it.plan != null) it.copy(isLoading = false, offlineMessage = result.message)
                    else it.copy(isLoading = false, errorMessage = result.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch { repository.updateStatus(planId, status) }
    }

    fun retryPending(id: String) {
        viewModelScope.launch { repository.retryPending(id) }
    }

    fun discardPending(id: String) {
        viewModelScope.launch { repository.discardPending(id) }
    }
}
