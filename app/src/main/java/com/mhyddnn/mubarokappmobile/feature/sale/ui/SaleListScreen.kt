package com.mhyddnn.mubarokappmobile.feature.sale.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.UnstyledDialog
import com.composeunstyled.UnstyledDialogPanel
import com.composeunstyled.UnstyledScrim
import com.composeunstyled.rememberDialogState
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.AppSpinner
import com.mhyddnn.mubarokappmobile.core.designsystem.AppText
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextButton
import com.mhyddnn.mubarokappmobile.core.designsystem.CardShape
import com.mhyddnn.mubarokappmobile.core.designsystem.EmptyView
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.ListItemCard
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.PaginationEffect
import com.mhyddnn.mubarokappmobile.core.designsystem.RefreshableBox
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusChip
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.sale.domain.PendingSale
import com.mhyddnn.mubarokappmobile.feature.sale.domain.Sale
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Daftar penjualan kanvas: loading / error+retry / kosong / list dengan pull-to-refresh
 * ([RefreshableBox]) dan infinite scroll ([PaginationEffect]).
 */
@Composable
fun SaleListScreen(
    onBack: () -> Unit,
    viewModel: SaleListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppScaffold(title = "Penjualan", onBack = onBack) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
        when {
            state.isLoading -> LoadingView(contentModifier)
            state.errorMessage != null && state.sales.isEmpty() && state.pending.isEmpty() -> ErrorView(
                message = state.errorMessage!!,
                modifier = contentModifier,
                onRetry = viewModel::load,
            )
            state.sales.isEmpty() && state.pending.isEmpty() -> EmptyView(
                message = "Belum ada penjualan",
                modifier = contentModifier,
            )
            else -> SaleList(
                state = state,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                onRetryPending = viewModel::retryPending,
                onDiscardPending = viewModel::discardPending,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun SaleList(
    state: SaleListUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryPending: (String) -> Unit,
    onDiscardPending: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    PaginationEffect(listState = listState, onLoadMore = onLoadMore)
    RefreshableBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.offlineMessage?.let { reason ->
                item(key = "offline") {
                    NoticeBanner(
                        text = "Menampilkan data tersimpan di HP. $reason",
                        tone = StatusTone.Warning,
                        actionText = "Muat ulang",
                        onAction = onRefresh,
                    )
                }
            }
            items(state.pending, key = { "pending-${it.id}" }) { pending ->
                PendingSaleCard(pending, onRetry = onRetryPending, onDiscard = onDiscardPending)
            }
            items(state.sales, key = { it.id }) { sale -> SaleCard(sale) }
            if (state.isAppending) {
                item(key = "append-loading") { AppendLoading() }
            }
        }
    }
}

@Composable
private fun AppendLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppSpinner(color = AppTheme.colors.primary, strokeWidth = 2.dp)
    }
}

// ponytail: kartu belum bisa diklik — tambahkan onClick saat layar detail penjualan dibuat.
@Composable
private fun SaleCard(sale: Sale) {
    ListItemCard(
        title = sale.invoice,
        subtitle = sale.note,
        trailingValue = Formatters.rupiah(sale.total),
        trailingLabel = sale.invoiceDate?.let { Formatters.displayIsoDate(it) },
        chips = {
            StatusChip(SaleStatusUi.transaction(sale.typeTransaction))
            StatusChip(SaleStatusUi.groupSale(sale.groupSaleId))
            if (sale.typeTransaction.equals("KREDIT", ignoreCase = true)) {
                StatusChip(SaleStatusUi.paymentStatus(sale.paymentRemaining))
            }
        },
    )
}

/** Penjualan buatan HP yang belum diterima server: menunggu sinyal, atau ditolak + aksi. */
@Composable
private fun PendingSaleCard(
    sale: PendingSale,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    val failed = sale.failedMessage != null

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ListItemCard(
            title = if (failed) "Ditolak server" else "Menunggu sinkron",
            subtitle = sale.failedMessage ?: sale.note,
            trailingValue = Formatters.rupiah(sale.total),
            trailingLabel = Formatters.displayIsoDate(sale.invoiceDate),
            chips = {
                StatusChip(
                    text = if (failed) "Gagal" else "Belum terkirim",
                    tone = if (failed) StatusTone.Danger else StatusTone.Warning,
                )
            },
        )
        if (failed) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppTextButton(text = "Hapus", onClick = { confirmDiscard = true }, color = AppTheme.colors.error)
                AppTextButton(text = "Kirim ulang", onClick = { onRetry(sale.id) })
            }
        }
    }

    if (confirmDiscard) {
        UnstyledDialog(state = rememberDialogState(initiallyVisible = true), onDismiss = { confirmDiscard = false }) {
            UnstyledScrim()
            UnstyledDialogPanel(
                shape = CardShape,
                backgroundColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.onSurface,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppText(text = "Hapus penjualan ini?", style = AppTheme.typography.titleMedium)
                    AppText(
                        text = "Data belum pernah tercatat di server dan akan hilang permanen dari HP.",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        AppTextButton(text = "Batal", onClick = { confirmDiscard = false })
                        AppTextButton(
                            text = "Hapus",
                            color = AppTheme.colors.error,
                            onClick = { confirmDiscard = false; onDiscard(sale.id) },
                        )
                    }
                }
            }
        }
    }
}
