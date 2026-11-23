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

data class VisitListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val visits: List<Visit> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class VisitListViewModel @Inject constructor(
    private val repository: VisitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitListUiState())
    val uiState: StateFlow<VisitListUiState> = _uiState.asStateFlow()

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

    fun search(query: String) {
        _uiState.update {
            it.copy(searchQuery = query)
        }

        viewModelScope.launch {
            fetch()
        }
    }

    private suspend fun fetch() {
        val query = _uiState.value.searchQuery

        when (
            val result = repository.getVisits(
                page = 1,
                limit = 50,
                search = query,
            )
        ) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        visits = result.data.items,
                        errorMessage = null,
                    )
                }
            }

            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.message,
                    )
                }
            }

            Result.Loading -> Unit
        }
    }
}