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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextField
import com.mhyddnn.mubarokappmobile.core.designsystem.ErrorView
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.designsystem.MoneyInputField
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.QuantityInputField
import com.mhyddnn.mubarokappmobile.core.designsystem.SectionCard
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanMasterData
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanOption
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Posisi personil, sama dengan sales-kanvas-fe. BE menerima string bebas. */
private val Positions = listOf("Sopir", "Sales", "Helper")

/**
 * Form tambah plan. [onCreated] (id plan dari server) dan [onQueued] (tersimpan di antrean)
 * dipanggil sekali lewat `LaunchedEffect` saat state berubah — navigasi tetap milik pemanggil.
 */
@Composable
fun PlanFormScreen(
    onCreated: (Int) -> Unit,
    onQueued: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlanFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdPlanId) { state.createdPlanId?.let(onCreated) }
    LaunchedEffect(state.queued) { if (state.queued) onQueued() }

    AppScaffold(title = "Tambah Plan", onBack = onBack) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
        val master = state.master
        when {
            state.isLoading -> LoadingView(contentModifier)
            master == null -> ErrorView(
                message = state.loadError ?: "Gagal memuat data master",
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
    val err = state.fieldErrors
    fun List<PlanOption>.find(id: Int?) = firstOrNull { it.id == id }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        master.offlineMessage?.let { reason ->
            NoticeBanner(
                text = "Pilihan dari data tersimpan di HP. Plan dikirim otomatis saat ada sinyal. $reason",
                tone = StatusTone.Warning,
            )
        }
        state.errorMessage?.let { NoticeBanner(text = it, tone = StatusTone.Danger) }

        SectionCard(title = "Data plan") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppDatePickerField(
                    label = "Tgl invoice",
                    value = state.invoiceDate,
                    onValueChange = { v -> onEdit { it.copy(invoiceDate = v) } },
                    errorText = err["tgl_invoice"],
                )
                AppDatePickerField(
                    label = "Tgl pergi kanvas",
                    value = state.departDate,
                    onValueChange = { v -> onEdit { it.copy(departDate = v) } },
                    errorText = err["tgl_pergi_kanvas"],
                )
                AppDatePickerField(
                    label = "Tgl kembali kanvas (opsional)",
                    value = state.returnDate,
                    onValueChange = { v -> onEdit { it.copy(returnDate = v) } },
                    errorText = err["tgl_kembali_kanvas"],
                )
                AppDropdown(
                    label = "Market area",
                    items = master.marketAreas,
                    selected = master.marketAreas.find(state.marketAreaId),
                    onSelected = { o -> onEdit { it.copy(marketAreaId = o.id) } },
                    itemLabel = PlanOption::label,
                    errorText = err["market_area_id"],
                )
                AppDropdown(
                    label = "Kendaraan",
                    items = master.vehicles,
                    selected = master.vehicles.find(state.vehicleId),
                    onSelected = { o -> onEdit { it.copy(vehicleId = o.id) } },
                    itemLabel = PlanOption::label,
                    errorText = err["vehicle_id"],
                )
                AppTextField(
                    value = state.note,
                    onValueChange = { v -> onEdit { it.copy(note = v) } },
                    label = "Catatan (opsional)",
                    singleLine = false,
                    errorText = err["note"],
                )
            }
        }

        RowsSection(
            title = "Personil",
            addText = "Tambah personil",
            rows = state.personnel,
            onAdd = { onEdit { it.copy(personnel = it.personnel + PersonnelRow()) } },
            onRemove = { i -> onEdit { it.copy(personnel = it.personnel.without(i)) } },
        ) { i, row ->
            val set: (PersonnelRow) -> Unit = { r -> onEdit { it.copy(personnel = it.personnel.with(i, r)) } }
            AppDropdown(
                label = "Karyawan",
                items = master.employees,
                selected = master.employees.find(row.employeeId),
                onSelected = { set(row.copy(employeeId = it.id)) },
                itemLabel = PlanOption::label,
                errorText = err["salesKanvasPlanPersonil.$i.employee_id"],
            )
            AppDropdown(
                label = "Posisi",
                items = Positions,
                selected = row.position,
                onSelected = { set(row.copy(position = it)) },
                itemLabel = { it },
            )
        }

        RowsSection(
            title = "Produk",
            addText = "Tambah produk",
            rows = state.products,
            onAdd = { onEdit { it.copy(products = it.products + ProductRow()) } },
            onRemove = { i -> onEdit { it.copy(products = it.products.without(i)) } },
        ) { i, row ->
            val set: (ProductRow) -> Unit = { r -> onEdit { it.copy(products = it.products.with(i, r)) } }
            AppDropdown(
                label = "Produk",
                items = master.products,
                selected = master.products.find(row.productId),
                onSelected = { set(row.copy(productId = it.id)) },
                itemLabel = PlanOption::label,
                errorText = err["salesKanvasPlanDetail.$i.product_id"],
            )
            QuantityInputField(
                label = "Qty",
                value = row.qty,
                onValueChange = { set(row.copy(qty = it)) },
                errorText = err["salesKanvasPlanDetail.$i.qty"],
            )
        }

        RowsSection(
            title = "Kasbon material (opsional)",
            addText = "Tambah kasbon",
            rows = state.kasbon,
            onAdd = { onEdit { it.copy(kasbon = it.kasbon + KasbonRow()) } },
            onRemove = { i -> onEdit { it.copy(kasbon = it.kasbon.without(i)) } },
        ) { i, row ->
            val set: (KasbonRow) -> Unit = { r -> onEdit { it.copy(kasbon = it.kasbon.with(i, r)) } }
            AppDropdown(
                label = "Jenis",
                items = master.kasbonTypes,
                selected = master.kasbonTypes.find(row.typeId),
                onSelected = { set(row.copy(typeId = it.id)) },
                itemLabel = PlanOption::label,
                errorText = err["salesKanvasKasbonMaterial.$i.kasbon_material_type_id"],
            )
            MoneyInputField(
                label = "Harga",
                value = row.price,
                onValueChange = { set(row.copy(price = it)) },
                errorText = err["salesKanvasKasbonMaterial.$i.price"],
            )
            QuantityInputField(
                label = "Qty",
                value = row.qty,
                onValueChange = { set(row.copy(qty = it)) },
                errorText = err["salesKanvasKasbonMaterial.$i.qty"],
            )
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

/** Kartu berisi baris dinamis (tambah/hapus). BE menerima array kosong. */
@Composable
private fun <T> RowsSection(
    title: String,
    addText: String,
    rows: List<T>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    rowContent: @Composable (Int, T) -> Unit,
) {
    SectionCard(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { i, row ->
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowContent(i, row)
                    }
                    AppIconButton(onClick = { onRemove(i) }) {
                        AppIcon(Icons.Default.Delete, contentDescription = "Hapus", tint = AppTheme.colors.error)
                    }
                }
            }
            AppTextButton(text = "+ $addText", onClick = onAdd)
        }
    }
}

private fun <T> List<T>.with(index: Int, item: T): List<T> = toMutableList().also { it[index] = item }

private fun <T> List<T>.without(index: Int): List<T> = filterIndexed { i, _ -> i != index }
