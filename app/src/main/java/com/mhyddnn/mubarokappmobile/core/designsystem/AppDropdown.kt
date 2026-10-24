package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.core.ModalBottomSheet
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Dropdown pilihan tunggal yang generik. [items] bisa tipe apa saja; [itemLabel]
 * menentukan teks yang ditampilkan. Cocok untuk dropdown master (customer, product, bank).
 *
 * Klik field membuka **bottom sheet** berisi daftar pilihan (bukan menu dropdown biasa) — lebih
 * nyaman dijamah untuk daftar panjang di layar sentuh.
 */
@Composable
fun <T> AppDropdown(
    label: String,
    items: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Pilih…",
    errorText: String? = null,
    enabled: Boolean = true,
) {
    var showSheet by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    Box(modifier = modifier.fillMaxWidth()) {
        FieldShell(
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dropdown_$label"),
            errorText = errorText,
            enabled = enabled,
            trailingIcon = {
                AppIcon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
            },
        ) {
            AppText(
                text = selected?.let(itemLabel) ?: placeholder,
                style = AppTheme.typography.bodyLarge,
                color = if (selected != null) colors.onSurface else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Overlay transparan menangkap tap di seluruh area field — field di atas hanya teks
        // statis (bukan input), tapi tetap dilapis biar area sentuh konsisten dengan field lain.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showSheet = true },
        )
    }

    if (showSheet) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(items, query) {
            if (query.isBlank()) items else items.filter { itemLabel(it).contains(query, ignoreCase = true) }
        }
        val sheetState = rememberModalBottomSheetState(initialDetent = SheetDetent.FullyExpanded)

        ModalBottomSheet(state = sheetState, onDismiss = { showSheet = false }) {
            Scrim()
            Sheet(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                backgroundColor = colors.surface,
                contentColor = colors.onSurface,
                contentPadding = PaddingValues(16.dp),
            ) {
                Column {
                    AppTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Cari",
                        placeholder = "Cari…",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(modifier = Modifier.navigationBarsPadding()) {
                        items(filtered) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(item)
                                        showSheet = false
                                    }
                                    .padding(horizontal = 4.dp, vertical = 14.dp),
                            ) {
                                AppText(text = itemLabel(item), style = AppTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
