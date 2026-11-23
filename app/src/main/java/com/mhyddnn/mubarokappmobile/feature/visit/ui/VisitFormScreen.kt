package com.mhyddnn.mubarokappmobile.feature.visit.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.core.designsystem.AppDatePickerField
import com.mhyddnn.mubarokappmobile.core.designsystem.AppDropdown
import com.mhyddnn.mubarokappmobile.core.designsystem.AppPrimaryButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextField
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.SectionCard
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitMarketArea
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.MubarokAppTheme

@Composable
fun VisitFormScreen(
    onCreated: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: VisitFormViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(state.createdVisitId) {
        state.createdVisitId?.let(onCreated)
    }

    AppScaffold(
        title = "Tambah Kunjungan",
        onBack = onBack,
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)

        when {
            state.isLoading -> {
                LoadingView(contentModifier)
            }

            state.loadError != null -> {
                ErrorView(
                    message = state.loadError,
                    modifier = contentModifier,
                    onRetry = viewModel::loadMaster,
                )
            }

            else -> {
                VisitFormContent(
                    state = state,
                    onEdit = viewModel::edit,
                    onSubmit = viewModel::submit,
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun VisitFormContent(
    state: VisitFormUiState,
    onEdit: ((VisitFormUiState) -> VisitFormUiState) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errors = state.fieldErrors

    fun findArea(id: Int?): VisitMarketArea? =
        state.marketAreas.firstOrNull { it.id == id }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .androidxPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.errorMessage?.let {
            NoticeBanner(
                text = it,
                tone = StatusTone.Danger,
            )
        }

        SectionCard(title = "Data Kunjungan") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextField(
                    value = state.storeName,
                    onValueChange = { value ->
                        onEdit { it.copy(storeName = value) }
                    },
                    label = "Nama toko",
                    placeholder = "Masukkan nama toko",
                    singleLine = true,
                    errorText = errors["store_name"],
                    modifier = Modifier.fillMaxWidth(),
                )

                AppDatePickerField(
                    label = "Tanggal kunjungan",
                    value = state.visitDate,
                    onValueChange = { value ->
                        onEdit { it.copy(visitDate = value) }
                    },
                    errorText = errors["visit_date"],
                    modifier = Modifier.fillMaxWidth(),
                )

                AppDropdown(
                    label = "Market area",
                    items = state.marketAreas,
                    selected = findArea(state.marketAreaId),
                    onSelected = { area ->
                        onEdit { it.copy(marketAreaId = area.id) }
                    },
                    itemLabel = { area ->
                        area.code?.let { "$it · ${area.name}" } ?: area.name
                    },
                    errorText = errors["market_area_id"],
                    placeholder = "Pilih market area",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SectionCard(title = "Catatan") {
            AppTextField(
                value = state.note,
                onValueChange = { value ->
                    onEdit { it.copy(note = value) }
                },
                label = "Catatan (opsional)",
                placeholder = "Tambahkan catatan kunjungan...",
                singleLine = false,
                errorText = errors["note"],
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AppPrimaryButton(
            text = "Simpan kunjungan",
            onClick = onSubmit,
            loading = state.isSaving,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun Modifier.androidxPadding(): Modifier =
    this.then(Modifier)

@Preview(name = "Terang")
@Preview(
    name = "Gelap",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VisitFormPreview() {
    val areas = listOf(
        VisitMarketArea(
            id = 1,
            name = "Kudus Kota",
            code = "KDS",
        ),
        VisitMarketArea(
            id = 2,
            name = "Jati",
            code = "JTI",
        ),
        VisitMarketArea(
            id = 3,
            name = "Bae",
            code = "BAE",
        ),
    )

    MubarokAppTheme {
        AppScaffold(
            title = "Tambah Kunjungan",
            onBack = {},
        ) {
            VisitFormContent(
                state = VisitFormUiState(
                    isLoading = false,
                    marketAreas = areas,
                    storeName = "Toko Maju Jaya",
                    visitDate = "2026-11-22",
                    marketAreaId = 1,
                    note = "Kunjungan rutin",
                ),
                onEdit = {},
                onSubmit = {},
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
            )
        }
    }
}