package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

private val CardPadding = PaddingValues(16.dp)

/**
 * Kartu item daftar serbaguna (mis. baris penjualan / customer): judul + subtitle di kiri,
 * nilai (mis. nominal) + label status di kanan. Bisa diklik.
 *
 * [chips] adalah slot opsional untuk satu/beberapa [StatusChip] di bawah subtitle (mis. tipe
 * transaksi + group sale, atau status plan).
 */
@Composable
fun ListItemCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingValue: String? = null,
    trailingLabel: String? = null,
    chips: @Composable (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    UnstyledButton(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = CardShape,
        contentPadding = CardPadding,
        modifier = modifier
            .fillMaxWidth()
            .surface(color = colors.surface, elevation = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = title,
                style = typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                AppText(
                    text = subtitle,
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (chips != null) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = chips,
                )
            }
        }
        if (trailingValue != null || trailingLabel != null) {
            Column(horizontalAlignment = Alignment.End) {
                if (trailingValue != null) {
                    AppText(text = trailingValue, style = typography.titleSmall)
                }
                if (trailingLabel != null) {
                    AppText(
                        text = trailingLabel,
                        style = typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
