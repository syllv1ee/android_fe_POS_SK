package com.mhyddnn.mubarokappmobile.feature.plan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.core.common.Formatters
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.AppSecondaryButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextButton
import com.mhyddnn.mubarokappmobile.core.designsystem.ConfirmDialog
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.LabeledRow
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.SectionCard
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusChip
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.displayStatus
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import java.math.BigDecimal

/**
 * Detail plan + ubah status
 */
@Composable
fun PlanDetailScreen(
    onBack: () -> Unit,
    viewModel: PlanDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppScaffold(title = "Detail Plan", onBack = onBack) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
        val plan = state.plan
        when {
            state.isLoading -> LoadingView(contentModifier)
            plan == null -> ErrorView(
                message = state.errorMessage ?: "Plan tidak ditemukan",
                modifier = contentModifier,
                onRetry = viewModel::load,
            )
            else -> PlanDetailContent(
                plan = plan,
                state = state,
                onReload = viewModel::load,
                onChangeStatus = viewModel::updateStatus,
                onRetryPending = viewModel::retryPending,
                onDiscardPending = viewModel::discardPending,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun PlanDetailContent(
    plan: Plan,
    state: PlanDetailUiState,
    onReload: () -> Unit,
    onChangeStatus: (String) -> Unit,
    onRetryPending: (String) -> Unit,
    onDiscardPending: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = plan.displayStatus(state.pending)
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
        state.pending.forEach { PendingStatusBanner(it, onRetryPending, onDiscardPending) }

        SectionCard(title = plan.invoice, trailing = { StatusChip(PlanStatusUi.status(status)) }) {
            LabeledRow("Tgl invoice", plan.invoiceDate?.let { Formatters.displayIsoDate(it) } ?: dash)
            LabeledRow("Tgl pergi", plan.departDate?.let { Formatters.displayIsoDate(it) } ?: dash)
            LabeledRow("Tgl kembali", plan.returnDate?.let { Formatters.displayIsoDate(it) } ?: dash)
            LabeledRow("Market area", plan.marketArea ?: dash)
            LabeledRow("Kendaraan", plan.vehicle ?: dash)
            LabeledRow("Revisi ke", plan.revision.toString())
            plan.revisedTo?.let { LabeledRow("Direvisi menjadi", it) }
            plan.note?.let { LabeledRow("Catatan", it) }
        }

        SectionCard(title = "Personil") {
            if (plan.personnel.isEmpty()) LabeledRow("Personil", dash)
            plan.personnel.forEach { LabeledRow(it.position, it.name) }
        }

        SectionCard(title = "Produk") {
            if (plan.products.isEmpty()) LabeledRow("Produk", dash)
            plan.products.forEach {
                // qty Double dari BE (Decimal) → "10" bukan "10.0", "2.5" tetap.
                LabeledRow(listOfNotNull(it.code, it.name).joinToString(" · "), BigDecimal.valueOf(it.qty).stripTrailingZeros().toPlainString())
            }
        }

        if (plan.kasbon.isNotEmpty()) {
            SectionCard(title = "Kasbon material") {
                plan.kasbon.forEach {
                    LabeledRow("${it.name} (${it.qty}×)", Formatters.rupiah(it.price * it.qty))
                }
            }
        }

        // BATAL terkunci di BE: tidak bisa diubah lagi.
        if (status != PlanStatus.CANCELLED) {
            StatusActions(current = status, onChangeStatus = onChangeStatus)
        }
    }
}

/**
 * Tombol ke setiap status selain [current] (BE tidak membatasi urutan transisi), dengan konfirmasi.
 * Hanya mengantrekan perubahan; pengiriman oleh SyncWorker.
 */
// belum ada permission `plan:write` | sementara tidak perlu dulu".
@Composable
private fun StatusActions(current: String, onChangeStatus: (String) -> Unit) {
    var target by remember { mutableStateOf<String?>(null) }

    SectionCard(title = "Ubah status") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanStatus.selectable.filter { it != current }.forEach { status ->
                AppSecondaryButton(
                    text = PlanStatusUi.status(status).text,
                    onClick = { target = status },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    target?.let { status ->
        val cancel = status == PlanStatus.CANCELLED
        ConfirmDialog(
            title = "Ubah status ke ${PlanStatusUi.status(status).text}?",
            message = if (cancel) "Plan yang dibatalkan tidak bisa diubah lagi. Dikirim otomatis saat ada sinyal."
            else "Dikirim otomatis saat ada sinyal.",
            confirmText = "Ubah",
            onConfirm = { onChangeStatus(status) },
            onDismiss = { target = null },
            destructive = cancel,
        )
    }
}

@Composable
private fun PendingStatusBanner(
    pending: PendingPlanStatus,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    val label = PlanStatusUi.status(pending.status).text
    val failed = pending.failedMessage
    if (failed == null) {
        NoticeBanner(text = "Status : $label menunggu sinkron.", tone = StatusTone.Warning)
        return
    }
    Column {
        NoticeBanner(text = "Status : $label ditolak server: $failed", tone = StatusTone.Danger)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(text = "Buang", onClick = { onDiscard(pending.id) }, color = AppTheme.colors.error)
            AppTextButton(text = "Kirim ulang", onClick = { onRetry(pending.id) })
        }
    }
}
