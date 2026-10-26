package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIcon
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIconButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.AppSpinner
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextButton
import com.mhyddnn.mubarokappmobile.core.designsystem.ConfirmDialog
import com.mhyddnn.mubarokappmobile.core.designsystem.EmptyView
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.ListItemCard
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.PaginationEffect
import com.mhyddnn.mubarokappmobile.core.designsystem.RefreshableBox
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusChip
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.displayStatus
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Daftar plan: plan buatan HP yang belum terkirim di atas, lalu plan dari server (atau cache HP
 * saat offline) dengan pull-to-refresh & infinite scroll. Tombol + membuka form tambah plan.
 */
@Composable
fun PlanListScreen(
    onPlanClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlanListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppScaffold(
        title = "Plan",
        onBack = onBack,
        actions = {
            AppIconButton(onClick = onAddClick) {
                AppIcon(Icons.Default.Add, contentDescription = "Tambah plan", tint = AppTheme.colors.onPrimaryContainer)
            }
        },
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
        when {
            state.isLoading -> LoadingView(contentModifier)
            state.errorMessage != null && state.plans.isEmpty() && state.pendingPlans.isEmpty() -> ErrorView(
                message = state.errorMessage!!,
                modifier = contentModifier,
                onRetry = viewModel::load,
            )
            state.plans.isEmpty() && state.pendingPlans.isEmpty() ->
                EmptyView(message = "Belum ada plan", modifier = contentModifier)
            else -> PlanList(
                state = state,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                onPlanClick = onPlanClick,
                onRetryPending = viewModel::retryPending,
                onDiscardPending = viewModel::discardPending,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun PlanList(
    state: PlanListUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlanClick: (Int) -> Unit,
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
            items(state.pendingPlans, key = { "pending-${it.id}" }) { pending ->
                PendingPlanCard(pending, onRetry = onRetryPending, onDiscard = onDiscardPending)
            }
            items(state.plans, key = { it.id }) { plan ->
                val pending = state.pending.filter { it.planId == plan.id }
                ListItemCard(
                    title = plan.invoice,
                    subtitle = listOfNotNull(plan.marketArea, plan.vehicle).joinToString(" · ").ifBlank { null },
                    trailingValue = plan.departDate?.let { Formatters.displayIsoDate(it) },
                    trailingLabel = "Tgl pergi",
                    chips = {
                        StatusChip(PlanStatusUi.status(plan.displayStatus(pending)))
                        if (pending.any { it.failedMessage != null }) {
                            StatusChip(text = "Gagal kirim", tone = StatusTone.Danger)
                        } else if (pending.isNotEmpty()) {
                            StatusChip(text = "Belum terkirim", tone = StatusTone.Warning)
                        }
                    },
                    onClick = { onPlanClick(plan.id) },
                )
            }
            if (state.isAppending) {
                item(key = "append-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppSpinner(color = AppTheme.colors.primary, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

/** Plan buatan HP yang belum diterima server: menunggu sinyal, atau ditolak + aksi kirim ulang/hapus. */
// ponytail: belum bisa dibuka/diedit — item ditolak dibuang lalu diisi ulang lewat form.
@Composable
private fun PendingPlanCard(
    plan: PendingPlan,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    val failed = plan.failedMessage != null

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ListItemCard(
            title = if (failed) "Ditolak server" else "Menunggu sinkron",
            subtitle = plan.failedMessage
                ?: listOfNotNull(plan.marketArea, plan.vehicle, plan.note).joinToString(" · ").ifBlank { null },
            trailingValue = Formatters.displayIsoDate(plan.departDate),
            trailingLabel = "Tgl pergi",
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
                AppTextButton(text = "Kirim ulang", onClick = { onRetry(plan.id) })
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Hapus plan ini?",
            message = "Plan belum pernah tercatat di server dan akan hilang permanen dari HP.",
            confirmText = "Hapus",
            onConfirm = { onDiscard(plan.id) },
            onDismiss = { confirmDiscard = false },
            destructive = true,
        )
    }
}
