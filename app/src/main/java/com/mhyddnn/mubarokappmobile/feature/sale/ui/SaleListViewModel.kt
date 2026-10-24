package com.mhyddnn.mubarokappmobile.feature.sale.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.sale.domain.PendingSale
import com.mhyddnn.mubarokappmobile.feature.sale.domain.Sale
import com.mhyddnn.mubarokappmobile.feature.sale.domain.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleListUiState(
    /** Loading awal layar penuh (belum ada data). */
    val isLoading: Boolean = false,
    /** Pull-to-refresh sedang berjalan. */
    val isRefreshing: Boolean = false,
    /** Memuat halaman berikutnya (footer spinner). */
    val isAppending: Boolean = false,
    val sales: List<Sale> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val errorMessage: String? = null,
    /** Terisi bila daftar diambil dari cache HP karena server tak terjangkau (alasannya). */
    val offlineMessage: String? = null,
    /** Penjualan buatan HP yang belum diterima server. */
    val pending: List<PendingSale> = emptyList(),
) {
    val canLoadMore: Boolean get() = page in 1 until totalPages
}

/**
 * Pola daftar ber-pagination: [load] (awal, layar penuh) · [refresh] (pull-to-refresh) ·
 * [loadMore] (infinite scroll). Contek ViewModel ini untuk daftar lain (retur, tagihan, dst).
 */
@HiltViewModel
class SaleListViewModel @Inject constructor(
    private val repository: SaleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleListUiState())
    val uiState: StateFlow<SaleListUiState> = _uiState.asStateFlow()

    init {
        load()
        repository.pendingSales
            .onEach { list -> _uiState.update { it.copy(pending = list) } }
            .launchIn(viewModelScope)
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

    fun retryPending(id: String) {
        viewModelScope.launch { repository.retryPending(id) }
    }

    fun discardPending(id: String) {
        viewModelScope.launch { repository.discardPending(id) }
    }

    fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isRefreshing || s.isAppending || !s.canLoadMore) return
        _uiState.update { it.copy(isAppending = true) }
        fetch(page = s.page + 1, replace = false)
    }

    private fun fetch(page: Int, replace: Boolean) {
        viewModelScope.launch {
            when (val result = repository.getSales(page = page)) {
                is Result.Success -> _uiState.update { state ->
                    val data = result.data
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isAppending = false,
                        sales = if (replace) data.items else state.sales + data.items,
                        page = data.page,
                        totalPages = data.totalPages,
                        errorMessage = null,
                        offlineMessage = null,
                    )
                }
                is Result.Error -> {
                    // Halaman awal gagal (umumnya tanpa sinyal) → tampilkan cache HP.
                    val cached = if (replace) repository.cachedSales() else emptyList()
                    _uiState.update { state ->
                        if (cached.isNotEmpty()) {
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                sales = cached,
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
                                errorMessage = if (replace && state.sales.isEmpty()) result.message
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
