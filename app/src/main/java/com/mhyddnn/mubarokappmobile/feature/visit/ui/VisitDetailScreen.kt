package com.mhyddnn.mubarokappmobile.feature.visit.ui

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
import androidx.compose.runtime.LaunchedEffect
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
import com.mhyddnn.mubarokappmobile.core.designsystem.AppPrimaryButton
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
import com.mhyddnn.mubarokappmobile.feature.visit.domain.Visit
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.MubarokAppTheme

@Composable
fun VisitDetailScreen(
    visitId: Int,
    onBack: () -> Unit,
    viewModel: VisitDetailViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(visitId) {
        viewModel.load(visitId)
    }

    AppScaffold(
        title = "Detail Kunjungan",
        onBack = onBack,
    ) {
        when {
            state.isLoading -> {
                LoadingView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                )
            }

            state.errorMessage != null && state.visit == null -> {
                ErrorView(
                    message = state.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                    onRetry = { viewModel.load(visitId) },
                )
            }

            state.visit != null -> {
                VisitDetailContent(
                    visit = state.visit,
                    isUpdating = state.isUpdating,
                    actionMessage = state.actionMessage,
                    onUpdateStatus = viewModel::updateStatus,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                )
            }
        }
    }
}

@Composable
private fun VisitDetailContent(
    visit: Visit,
    isUpdating: Boolean,
    actionMessage: String?,
    onUpdateStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actionMessage?.let {
            NoticeBanner(
                text = it,
                tone = if (visit.status == "DIRENCANAKAN") {
                    StatusTone.Warning
                } else {
                    StatusTone.Success
                },
            )
        }

        SectionCard(title = "Informasi Kunjungan") {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledRow(
                    label = "Nama toko",
                    value = visit.storeName,
                )

                LabeledRow(
                    label = "Tanggal kunjungan",
                    value = runCatching {
                        Formatters.displayIsoDate(visit.visitDate)
                    }.getOrDefault(visit.visitDate),
                )

                LabeledRow(
                    label = "Market area",
                    value = visit.marketAreaName ?: "-",
                )

                LabeledRow(
                    label = "Status",
                    value = statusText,
                )

                StatusChip(
                    text = statusText,
                    tone = statusTone,
                )
            }
        }

        SectionCard(title = "Catatan Kunjungan") {
            LabeledRow(
                label = "Catatan",
                value = visit.note?.takeIf { it.isNotBlank() } ?: "Tidak ada catatan",
            )
        }

        if (visit.status == "DIRENCANAKAN") {
            SectionCard(title = "Tindakan") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppPrimaryButton(
                        text = "Tandai Selesai",
                        onClick = { showCompleteDialog = true },
                        enabled = !isUpdating,
                        loading = isUpdating,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    AppSecondaryButton(
                        text = "Batalkan Kunjungan",
                        onClick = { showCancelDialog = true },
                        enabled = !isUpdating,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showCompleteDialog) {
        ConfirmDialog(
            title = "Selesaikan Kunjungan",
            message = "Apakah kunjungan ke ${visit.storeName} sudah selesai?",
            confirmText = "Ya, Selesaikan",
            onConfirm = {
                showCompleteDialog = false
                onUpdateStatus("SELESAI")
            },
            onDismiss = {
                showCompleteDialog = false
            },
        )
    }

    if (showCancelDialog) {
        ConfirmDialog(
            title = "Batalkan Kunjungan",
            message = "Apakah kamu yakin ingin membatalkan kunjungan ke ${visit.storeName}?",
            confirmText = "Ya, Batalkan",
            onConfirm = {
                showCancelDialog = false
                onUpdateStatus("BATAL")
            },
            onDismiss = {
                showCancelDialog = false
            },
            destructive = true,
        )
    }
}

@Preview(name = "Terang")
@Preview(
    name = "Gelap",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VisitDetailPreview() {
    val visit = Visit(
        id = 1,
        storeName = "Toko Maju Jaya",
        visitDate = "2026-11-22",
        marketAreaId = 1,
        marketAreaName = "Kudus Kota",
        note = "Mengecek stok produk dan melakukan penawaran produk baru.",
        status = "DIRENCANAKAN",
    )

    MubarokAppTheme {
        AppScaffold(
            title = "Detail Kunjungan",
            onBack = {},
        ) {
            VisitDetailContent(
                visit = visit,
                isUpdating = false,
                actionMessage = null,
                onUpdateStatus = {},
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
            )
        }
    }
}