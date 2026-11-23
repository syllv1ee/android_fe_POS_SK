
package com.mhyddnn.mubarokappmobile.feature.plan.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.mhyddnn.mubarokappmobile.core.designsystem.EmptyView
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.ListItemCard
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.RefreshableBox
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusChip
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.MubarokAppTheme

@Composable
fun PlanListScreen(
    onPlanClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlanListViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    AppScaffold(
        title = "Plan",
        onBack = onBack,
        actions = {
            AppIconButton(onClick = onAddClick) {
                AppIcon(
                    Icons.Default.Add,
                    contentDescription = "Tambah plan",
                    tint = AppTheme.colors.onPrimaryContainer,
                )
            }
        },
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)

        when {
            state.isLoading && state.plans.isEmpty() -> {
                LoadingView(contentModifier)
            }

            state.errorMessage != null && state.plans.isEmpty() -> {
                ErrorView(
                    message = state.errorMessage,
                    modifier = contentModifier,
                    onRetry = viewModel::load,
                )
            }

            state.plans.isEmpty() -> {
                EmptyView(
                    message = "Belum ada plan",
                    modifier = contentModifier,
                )
            }

            else -> {
                RefreshableBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = contentModifier,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.offlineMessage?.let { reason ->
                            item(key = "offline") {
                                NoticeBanner(
                                    text = "Menampilkan data tersimpan di HP. $reason",
                                    tone = com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone.Warning,
                                    actionText = "Muat ulang",
                                    onAction = viewModel::refresh,
                                )
                            }
                        }

                        items(
                            items = state.plans,
                            key = { it.id },
                        ) { plan ->
                            PlanListItem(
                                plan = plan,
                                onClick = { onPlanClick(plan.id) },
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
private fun PlanListItem(
    plan: Plan,
    onClick: () -> Unit,
) {
    val subtitle = listOfNotNull(
        plan.employee,
        plan.marketArea,
        plan.vehicle,
    ).joinToString(" · ").ifBlank { null }

    ListItemCard(
        title = plan.planNo,
        subtitle = subtitle,
        trailingValue = plan.departDate?.let {
            Formatters.displayIsoDate(it)
        },
        trailingLabel = "Tgl berangkat",
        chips = {
            StatusChip(PlanStatusUi.status(plan.status))
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
private fun PlanListPreview() {
    val plans = listOf(
        Plan(
            id = 1,
            planNo = "RKN-0001",
            status = "DRAFT",
            departDate = "2026-11-18",
            returnDate = "2026-11-19",
            employee = "Budi",
            vehicle = "B 1234 XY",
            marketArea = "Area Kota",
            outlets = listOf("Toko A", "Toko B"),
        ),
    )

    MubarokAppTheme {
        AppScaffold(title = "Plan", onBack = {}) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(plans) { plan ->
                    PlanListItem(plan = plan, onClick = {})
                }
            }
        }
    }
}
