package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Baris label–nilai untuk layar detail (mis. "Invoice  SK-2606-0001"). Label di kiri dengan
 * lebar minimum agar nilai sejajar antar-baris; nilai mengisi sisa lebar dan rata kanan.
 */
@Composable
fun LabeledRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val typography = AppTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            text = label,
            modifier = Modifier.widthIn(min = 96.dp),
            style = typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        AppText(
            text = value,
            modifier = Modifier.weight(1f),
            style = typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
