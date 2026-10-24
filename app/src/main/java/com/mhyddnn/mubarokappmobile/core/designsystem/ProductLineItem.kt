package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Baris satu produk dalam daftar detail (detail penjualan, barang dibawa di plan, retur):
 * nama produk + baris kuantitas/harga di kiri, [meta] opsional (mis. batch / tgl expired) di
 * bawahnya, dan [trailingAmount] opsional (mis. subtotal) di kanan.
 *
 * Semua angka diterima sudah terformat (lihat `Formatters`) agar komponen tetap UI-only.
 */
@Composable
fun ProductLineItem(
    name: String,
    qty: String,
    modifier: Modifier = Modifier,
    price: String? = null,
    meta: String? = null,
    trailingAmount: String? = null,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = name,
                style = typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            AppText(
                text = if (price != null) "$qty × $price" else qty,
                style = typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (meta != null) {
                AppText(
                    text = meta,
                    style = typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (trailingAmount != null) {
            AppText(
                text = trailingAmount,
                style = typography.titleSmall,
            )
        }
    }
}
