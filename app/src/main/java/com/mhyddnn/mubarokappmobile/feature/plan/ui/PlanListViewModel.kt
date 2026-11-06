package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import com.mhyddnn.mubarokappmobile.feature.plan.domain.deliveredStatuses
import com.mhyddnn.mubarokappmobile.feature.plan.domain.withDelivered
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val plans: List<Plan> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val errorMessage: String? = null,
    /** Terisi bila daftar diambil dari cache HP karena server tak terjangkau (alasannya). */
    val offlineMessage: String? = null,
    /** Perubahan status yang belum diterima server. */
    val pending: List<PendingPlanStatus> = emptyList(),
    /** Plan baru yang belum diterima server. */
    val pendingPlans: List<PendingPlan> = emptyList(),
) {
    val canLoadMore: Boolean get() = page in 1 until totalPages
}

/**
 * Daftar ber-pagination: load · refresh · loadMore, fallback cache saat offline.
 *
 * - [load]: muatan pertama (spinner penuh). [refresh]: tarik-untuk-muat-ulang. [loadMore]: halaman
 *   berikutnya, dipicu `PaginationEffect` saat scroll mendekati akhir. Masing-masing dijaga flag
 *   agar tidak berjalan dobel.
 * - Gagal di halaman 1 → tampilkan cache HP + [PlanListUiState.offlineMessage]; gagal di halaman
 *   berikutnya → daftar dibiarkan, coba lagi saat scroll.
 * - Mengamati antrean sinkron (status & plan baru) dan memuat ulang begitu ada yang diterima server.
 */
@HiltViewModel
class PlanListViewModel @Inject constructor(
    private val repository: PlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanListUiState())
    val uiState: StateFlow<PlanListUiState> = _uiState.asStateFlow()

    init {
        load()
        repository.pendingStatuses
            .onEach { list ->
                val delivered = deliveredStatuses(before = _uiState.value.pending, after = list)
                _uiState.update { s -> s.copy(pending = list, plans = s.plans.map { it.withDelivered(delivered) }) }
                // Sudah diterima server -> ambil data terbaru (juga memperbarui cache untuk offline).
                if (delivered.isNotEmpty()) refresh()
            }
            .launchIn(viewModelScope)
        repository.pendingPlans
            .onEach { list ->
                // Antrean berkurang = plan diterima server (atau dibuang) -> muat ulang agar plan
                // aslinya (dengan invoice) muncul di daftar.
                val synced = list.size < _uiState.value.pendingPlans.size
                _uiState.update { it.copy(pendingPlans = list) }
                if (synced) refresh()
            }
            .launchIn(viewModelScope)
    }

    fun retryPending(id: String) {
        viewModelScope.launch { repository.retryPending(id) }
    }

    fun discardPending(id: String) {
        viewModelScope.launch { repository.discardPending(id) }
    }

    fun load() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        fetch(page = 1, replace = true)
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        fetch(page = 1, replace = true)
    }

    fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isRefreshing || s.isAppending || !s.canLoadMore) return
        _uiState.update { it.copy(isAppending = true) }
        fetch(page = s.page + 1, replace = false)
    }

    private fun fetch(page: Int, replace: Boolean) {
        viewModelScope.launch {
            when (val result = repository.getPlans(page = page)) {
                is Result.Success -> _uiState.update { state ->
                    val data = result.data
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isAppending = false,
                        plans = if (replace) data.items else state.plans + data.items,
                        page = data.page,
                        totalPages = data.totalPages,
                        errorMessage = null,
                        offlineMessage = null,
                    )
                }
                is Result.Error -> {
                    val cached = if (replace) repository.cachedPlans() else emptyList()
                    _uiState.update { state ->
                        if (cached.isNotEmpty()) {
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                plans = cached,
                                page = 1,
                                totalPages = 1,
                                errorMessage = null,
                                offlineMessage = result.message,
                            )
                        } else {
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isAppending = false,
                                errorMessage = if (replace && state.plans.isEmpty()) result.message
                                else state.errorMessage,
                            )
                        }
                    }
                }
                Result.Loading -> Unit
            }
        }
    }
}
