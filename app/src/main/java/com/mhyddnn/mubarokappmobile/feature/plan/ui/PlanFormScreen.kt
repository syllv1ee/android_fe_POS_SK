
package com.mhyddnn.mubarokappmobile.feature.plan.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.core.designsystem.AppDatePickerField
import com.mhyddnn.mubarokappmobile.core.designsystem.AppDropdown
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIcon
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIconButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppPrimaryButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextButton
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.SectionCard
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanMasterData
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanOption
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.MubarokAppTheme

@Composable
fun PlanFormScreen(
    onCreated: (Int) -> Unit,
    onQueued: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlanFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdPlanId) {
        state.createdPlanId?.let(onCreated)
    }

    AppScaffold(
        title = "Tambah Plan",
        onBack = onBack,
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)

        val master = state.master

        when {
            state.isLoading -> LoadingView(contentModifier)

            master == null -> ErrorView(
                message = state.loadError ?: "Gagal memuat data pilihan",
                modifier = contentModifier,
                onRetry = viewModel::loadMaster,
            )

            else -> PlanFormContent(
                state = state,
                master = master,
                onEdit = viewModel::edit,
                onSubmit = viewModel::submit,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun PlanFormContent(
    state: PlanFormUiState,
    master: PlanMasterData,
    onEdit: ((PlanFormUiState) -> PlanFormUiState) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errors = state.fieldErrors

    fun List<PlanOption>.find(id: Int?) =
        firstOrNull { it.id == id }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        master.offlineMessage?.let { reason ->
            NoticeBanner(
                text = "Pilihan menggunakan data tersimpan di HP. $reason",
                tone = StatusTone.Warning,
            )
        }

        state.errorMessage?.let {
            NoticeBanner(
                text = it,
                tone = StatusTone.Danger,
            )
        }

        SectionCard(title = "Data Plan") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppDropdown(
                    label = "Karyawan",
                    items = master.employees,
                    selected = master.employees.find(state.employeeId),
                    onSelected = { option ->
                        onEdit { it.copy(employeeId = option.id) }
                    },
                    itemLabel = PlanOption::label,
                    errorText = errors["employee_id"],
                )

                AppDatePickerField(
                    label = "Tanggal berangkat",
                    value = state.departureDate,
                    onValueChange = { value ->
                        onEdit { it.copy(departureDate = value) }
                    },
                    errorText = errors["departure_date"],
                )

                AppDatePickerField(
                    label = "Tanggal kembali (opsional)",
                    value = state.returnDate,
                    onValueChange = { value ->
                        onEdit { it.copy(returnDate = value) }
                    },
                    errorText = errors["return_date"],
                )

                AppDropdown(
                    label = "Market area (opsional)",
                    items = master.marketAreas,
                    selected = master.marketAreas.find(state.marketAreaId),
                    onSelected = { option ->
                        onEdit { it.copy(marketAreaId = option.id) }
                    },
                    itemLabel = PlanOption::label,
                    errorText = errors["market_area_id"],
                )

                AppDropdown(
                    label = "Kendaraan (opsional)",
                    items = master.vehicles,
                    selected = master.vehicles.find(state.vehicleId),
                    onSelected = { option ->
                        onEdit { it.copy(vehicleId = option.id) }
                    },
                    itemLabel = PlanOption::label,
                    errorText = errors["vehicle_id"],
                )
            }
        }

        SectionCard(title = "Outlet") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.outlets.isEmpty()) {
                    NoticeBanner(
                        text = "Belum ada outlet yang dipilih.",
                        tone = StatusTone.Warning,
                    )
                }

                state.outlets.forEachIndexed { index, row ->
                    Row(verticalAlignment = Alignment.Top) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            AppDropdown(
                                label = "Outlet ${index + 1}",
                                items = master.outlets,
                                selected = master.outlets.find(row.outletId),
                                onSelected = { option ->
                                    onEdit { current ->
                                        current.copy(
                                            outlets = current.outlets.toMutableList().also {
                                                it[index] = row.copy(outletId = option.id)
                                            },
                                        )
                                    }
                                },
                                itemLabel = PlanOption::label,
                                errorText = errors["outlet_ids"],
                            )
                        }

                        AppIconButton(
                            onClick = {
                                onEdit { current ->
                                    current.copy(
                                        outlets = current.outlets.filterIndexed { i, _ ->
                                            i != index
                                        },
                                    )
                                }
                            },
                        ) {
                            AppIcon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus outlet",
                                tint = AppTheme.colors.error,
                            )
                        }
                    }
                }

                AppTextButton(
                    text = "+ Tambah outlet",
                    onClick = {
                        onEdit { current ->
                            current.copy(
                                outlets = current.outlets + OutletRow(),
                            )
                        }
                    },
                )
            }
        }

        AppPrimaryButton(
            text = "Simpan plan",
            onClick = onSubmit,
            loading = state.isSaving,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Terang")
@Preview(
    name = "Gelap",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PlanFormPreview() {
    val master = PlanMasterData(
        marketAreas = listOf(PlanOption(1, "Area Kota")),
        vehicles = listOf(PlanOption(1, "B 1234 XY")),
        employees = listOf(PlanOption(1, "Budi")),
        outlets = listOf(
            PlanOption(1, "Toko A"),
            PlanOption(2, "Toko B"),
        ),
    )

    MubarokAppTheme {
        AppScaffold(
            title = "Tambah Plan",
            onBack = {},
        ) {
            PlanFormContent(
                state = PlanFormUiState(
                    isLoading = false,
                    master = master,
                    employeeId = 1,
                    outlets = listOf(OutletRow(1)),
                ),
                master = master,
                onEdit = {},
                onSubmit = {},
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
            )
        }
    }
}
