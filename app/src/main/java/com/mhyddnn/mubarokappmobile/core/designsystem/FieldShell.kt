package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Kerangka visual field bergaya "outlined" (label di atas, kotak berbatas, pesan error di
 * bawah) — dipakai [AppTextField], [MoneyInputField]/[QuantityInputField], [AppDropdown], dan
 * [AppDatePickerField] agar tampilan seragam. Pengganti `OutlinedTextField` M3 (yang tidak ada
 * padanannya di Compose Unstyled).
 */
@Composable
internal fun FieldShell(
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val borderColor = if (errorText != null) colors.error else colors.outline

    Column(modifier = modifier.alpha(if (enabled) 1f else 0.5f)) {
        AppText(
            text = label,
            style = AppTheme.typography.labelSmall,
            color = if (errorText != null) colors.error else colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .surface(
                    color = colors.surface,
                    shape = FieldShape,
                    borderColor = borderColor,
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                content()
            }
            trailingIcon?.invoke()
        }
        if (errorText != null) {
            Spacer(Modifier.height(4.dp))
            AppText(text = errorText, style = AppTheme.typography.bodySmall, color = colors.error)
        }
    }
}
