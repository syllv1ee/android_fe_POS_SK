package com.mhyddnn.mubarokappmobile.feature.salescanvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.core.designsystem.AppScaffold
import com.mhyddnn.mubarokappmobile.core.designsystem.EmptyView
import com.mhyddnn.mubarokappmobile.core.designsystem.MenuShortcutCard
import com.mhyddnn.mubarokappmobile.feature.salescanvas.SalesCanvasRoutes
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Hub Sales Kanvas: pintasan ke Plan, Penjualan, Retur, Tagihan (grid 2 kolom). Statis; rute
 * tujuan didaftarkan di [com.mhyddnn.mubarokappmobile.feature.salescanvas.salesCanvasGraph].
 */
@Composable
fun SalesCanvasScreen(
    onMenuClick: (Any) -> Unit,
    onBack: () -> Unit,
) {
    AppScaffold(title = "Sales Kanvas", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                Triple("Plan", Icons.Default.EventNote, SalesCanvasRoutes.Plan),
                Triple("Penjualan", Icons.Default.ShoppingCart, SalesCanvasRoutes.Sale),
                Triple("Retur", Icons.Default.SwapHoriz, SalesCanvasRoutes.Return),
                Triple("Tagihan", Icons.Default.Receipt, SalesCanvasRoutes.Billing),
            ).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, icon, route) ->
                        MenuShortcutCard(
                            label = label,
                            icon = icon,
                            onClick = { onMenuClick(route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Layar kosong untuk menu yang belum dibuat (Penjualan, Retur, Tagihan). */
// ponytail: satu layar kosong dipakai bersama; pecah per fitur saat isinya mulai dibuat.
@Composable
fun SalesCanvasPlaceholderScreen(title: String, onBack: () -> Unit) {
    AppScaffold(title = title, onBack = onBack) {
        EmptyView(
            message = "Belum ada konten",
            modifier = Modifier.background(AppTheme.colors.background),
        )
    }
}
