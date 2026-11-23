
package com.mhyddnn.mubarokappmobile.feature.plan.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.AppSecondaryButton
import com.mhyddnn.mubarokappmobile.core.designsystem.ConfirmDialog
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.LabeledRow
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.SectionCard
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusChip
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanStatus
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.MubarokAppTheme

@Composable
fun PlanDetailScreen(
    onBack: () -> Unit,
    viewModel: PlanDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppScaffold(
        title = "Detail Plan",
        onBack = onBack,
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)

        val plan = state.plan

        when {
            state.isLoading && plan == null -> {
                LoadingView(contentModifier)
            }

            plan == null -> {
                ErrorView(
                    message = state.errorMessage ?: "Plan tidak ditemukan",
                    modifier = contentModifier,
                    onRetry = viewModel::load,
                )
            }

            else -> {
                PlanDetailContent(
                    plan = plan,
                    state = state,
                    onReload = viewModel::load,
                    onApprove = viewModel::approve,
                    onCancel = viewModel::cancel,
                    onComplete = viewModel::complete,
                    onCancelLoading = viewModel::cancelLoading,
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun PlanDetailContent(
    plan: Plan,
    state: PlanDetailUiState,
    onReload: () -> Unit,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onCancelLoading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dash = "-"

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.offlineMessage?.let { reason ->
            NoticeBanner(
                text = "Menampilkan data tersimpan di HP. $reason",
                tone = StatusTone.Warning,
                actionText = "Muat ulang",
                onAction = onReload,
            )
        }

        state.errorMessage?.let { message ->
            NoticeBanner(
                text = message,
                tone = StatusTone.Danger,
            )
        }

        SectionCard(
            title = plan.planNo,
            trailing = {
                StatusChip(PlanStatusUi.status(plan.status))
            },
        ) {
            LabeledRow(
                "Tanggal berangkat",
                plan.departDate?.let { Formatters.displayIsoDate(it) } ?: dash,
            )

            LabeledRow(
                "Tanggal kembali",
                plan.returnDate?.let { Formatters.displayIsoDate(it) } ?: dash,
            )

            LabeledRow("Karyawan", plan.employee ?: dash)
            LabeledRow("Market area", plan.marketArea ?: dash)
            LabeledRow("Kendaraan", plan.vehicle ?: dash)
            LabeledRow(
                "Outlet",
                plan.outlets.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: dash,
            )
        }

        if (state.isUpdating) {
            NoticeBanner(
                text = "Sedang mengirim perubahan ke server...",
                tone = StatusTone.Warning,
            )
        }

        StatusActions(
            status = plan.status,
            isUpdating = state.isUpdating,
            onApprove = onApprove,
            onCancel = onCancel,
            onComplete = onComplete,
            onCancelLoading = onCancelLoading,
        )
    }
}

@Composable
private fun StatusActions(
    status: String,
    isUpdating: Boolean,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onCancelLoading: () -> Unit,
) {
    var target by remember { mutableStateOf<String?>(null) }

    val actions = when (status) {
        PlanStatus.DRAFT -> listOf(
            "approve" to "Setujui plan",
            "cancel" to "Batalkan plan",
        )

        PlanStatus.APPROVED -> listOf(
            "cancel" to "Batalkan plan",
        )

        PlanStatus.LOADING -> listOf(
            "complete" to "Selesaikan plan",
            "cancel-loading" to "Batalkan proses loading",
        )

        else -> emptyList()
    }

    if (actions.isEmpty()) return

    SectionCard(title = "Tindakan Plan") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { (action, label) ->
                AppSecondaryButton(
                    text = label,
                    onClick = { target = action },
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    target?.let { action ->
        val title = when (action) {
            "approve" -> "Setujui plan?"
            "cancel" -> "Batalkan plan?"
            "complete" -> "Selesaikan plan?"
            "cancel-loading" -> "Batalkan proses loading?"
            else -> "Konfirmasi tindakan?"
        }

        val message = when (action) {
            "approve" -> "Plan akan berubah menjadi DISETUJUI."
            "cancel" -> "Plan akan dibatalkan. Tindakan ini tidak dapat dibatalkan kembali."
            "complete" -> "Plan akan diselesaikan. Pastikan proses penjualan sudah selesai."
            "cancel-loading" -> "Proses loading akan dibatalkan sesuai aturan server."
            else -> ""
        }

        val destructive = action == "cancel" || action == "cancel-loading"

        ConfirmDialog(
            title = title,
            message = message,
            confirmText = "Ya, lanjutkan",
            onConfirm = {
                when (action) {
                    "approve" -> onApprove()
                    "cancel" -> onCancel()
                    "complete" -> onComplete()
                    "cancel-loading" -> onCancelLoading()
                }
                target = null
            },
            onDismiss = {
                target = null
            },
            destructive = destructive,
        )
    }
}

private val previewPlan = Plan(
    id = 1,
    planNo = "RKN-0001",
    status = PlanStatus.APPROVED,
    departDate = "2026-11-18",
    returnDate = "2026-11-19",
    employee = "Budi",
    vehicle = "B 1234 XY",
    marketArea = "Area Kota",
    outlets = listOf("Toko A", "Toko B"),
)

@Preview(name = "Terang")
@Preview(
    name = "Gelap",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PlanDetailPreview() {
    MubarokAppTheme {
        AppScaffold(
            title = "Detail Plan",
            onBack = {},
        ) {
            PlanDetailContent(
                plan = previewPlan,
                state = PlanDetailUiState(
                    isLoading = false,
                    plan = previewPlan,
                ),
                onReload = {},
                onApprove = {},
                onCancel = {},
                onComplete = {},
                onCancelLoading = {},
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
            )
        }
    }
}
