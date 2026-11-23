
package com.mhyddnn.mubarokappmobile.feature.visit.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIcon
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIconButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.AppSpinner
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextField
import com.mhyddnn.mubarokappmobile.core.designsystem.EmptyView
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.ListItemCard
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.RefreshableBox
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusChip
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.visit.domain.Visit
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.MubarokAppTheme

@Composable
fun VisitListScreen(
    onVisitClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: VisitListViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    AppScaffold(
        title = "Kunjungan",
        onBack = onBack,
        actions = {
            AppIconButton(onClick = onAddClick) {
                AppIcon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah kunjungan",
                    tint = AppTheme.colors.onPrimaryContainer,
                )
            }
        },
    ) {
        when {
            state.isLoading && state.visits.isEmpty() -> {
                LoadingView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                )
            }

            state.errorMessage != null && state.visits.isEmpty() -> {
                ErrorView(
                    message = state.errorMessage.orEmpty(),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                    onRetry = viewModel::load,
                )
            }

            else -> {
                VisitListContent(
                    state = state,
                    onSearch = viewModel::search,
                    onRefresh = viewModel::refresh,
                    onVisitClick = onVisitClick,
                )
            }
        }
    }
}

@Composable
private fun VisitListContent(
    state: VisitListUiState,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onVisitClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppTextField(
            value = state.searchQuery,
            onValueChange = onSearch,
            label = "Cari kunjungan",
            placeholder = "Masukkan nama toko...",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            state.visits.isEmpty() -> {
                EmptyView(
                    message = if (state.searchQuery.isBlank()) {
                        "Belum ada data kunjungan"
                    } else {
                        "Kunjungan tidak ditemukan"
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            }

            else -> {
                RefreshableBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = state.visits,
                            key = { it.id },
                        ) { visit ->
                            VisitListItem(
                                visit = visit,
                                onClick = {
                                    onVisitClick(visit.id)
                                },
                            )
                        }

                        if (state.isRefreshing) {
                            item(key = "refresh-loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AppSpinner(
                                        color = AppTheme.colors.primary,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitListItem(
    visit: Visit,
    onClick: () -> Unit,
) {
    val statusText = when (visit.status) {
        "DIRENCANAKAN" -> "Direncanakan"
        "SELESAI" -> "Selesai"
        "BATAL" -> "Dibatalkan"
        else -> visit.status
    }

    val statusTone = when (visit.status) {
        "SELESAI" -> StatusTone.Success
        "BATAL" -> StatusTone.Danger
        else -> StatusTone.Info
    }

    ListItemCard(
        title = visit.storeName,
        subtitle = visit.marketAreaName ?: "Area belum tersedia",
        trailingValue = runCatching {
            Formatters.displayIsoDate(visit.visitDate)
        }.getOrDefault(visit.visitDate),
        trailingLabel = "Tanggal kunjungan",
        chips = {
            StatusChip(
                text = statusText,
                tone = statusTone,
            )
        },
        onClick = onClick,
    )
}

@Preview(name = "Terang")
@Preview(
    name = "Gelap",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VisitListPreview() {
    val visits = listOf(
        Visit(
            id = 1,
            storeName = "Toko Maju Jaya",
            visitDate = "2026-11-22",
            marketAreaId = 1,
            marketAreaName = "Kudus Kota",
            note = "Kunjungan rutin",
            status = "DIRENCANAKAN",
        ),
        Visit(
            id = 2,
            storeName = "Toko Berkah",
            visitDate = "2026-11-21",
            marketAreaId = 2,
            marketAreaName = "Jati",
            note = null,
            status = "SELESAI",
        ),
    )

    MubarokAppTheme {
        AppScaffold(
            title = "Kunjungan",
            onBack = {},
        ) {
            VisitListContent(
                state = VisitListUiState(
                    visits = visits,
                    isLoading = false,
                ),
                onSearch = {},
                onRefresh = {},
                onVisitClick = {},
            )
        }
    }
}
