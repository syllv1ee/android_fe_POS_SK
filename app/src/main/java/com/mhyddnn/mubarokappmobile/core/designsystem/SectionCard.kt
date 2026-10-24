package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Card bertajuk untuk mengelompokkan konten di layar detail (mis. "Personil", "Barang dibawa",
 * "Pembayaran"). Judul di kiri, [trailing] opsional di kanan (mis. jumlah item / aksi), lalu
 * [content] tersusun vertikal dengan jarak antar-elemen yang konsisten.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .surface(color = AppTheme.colors.surface, elevation = 1.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = title,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
            trailing?.invoke()
        }
        content()
    }
}
