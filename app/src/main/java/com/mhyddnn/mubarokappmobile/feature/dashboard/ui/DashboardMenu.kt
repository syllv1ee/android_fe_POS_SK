package com.mhyddnn.mubarokappmobile.feature.dashboard.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.ui.graphics.vector.ImageVector
import com.mhyddnn.mubarokappmobile.feature.salescanvas.SalesCanvasRoutes

/**
 * Satu item menu/pintasan di dashboard. Untuk menambah menu baru cukup tambahkan entri di
 * [dashboardMenus] — UI grid otomatis menampilkannya.
 *
 * @param route tujuan navigasi: objek rute `@Serializable` (mis. `SalesCanvasRoutes.Hub`).
 * @param requiredPermission bila diisi, menu hanya tampil bila sesi punya izin tsb (wildcard
 *        `modul:*` / `*` didukung). `null` = selalu tampil.
 */
data class DashboardMenu(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val route: Any,
    val requiredPermission: String? = null,
)

/** Registri menu dashboard. */
val dashboardMenus: List<DashboardMenu> = listOf(
    DashboardMenu(id = "sales_canvas", title = "Sales Kanvas", icon = Icons.Default.LocalShipping, route = SalesCanvasRoutes.Hub),
)
