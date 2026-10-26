package com.mhyddnn.mubarokappmobile.feature.dashboard.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.ui.graphics.vector.ImageVector
import com.mhyddnn.mubarokappmobile.feature.salescanvas.SalesCanvasRoutes

/**
 * Satu item menu/pintasan di dashboard. Untuk menambah menu baru cukup tambahkan entri di
 * [dashboardMenus] — UI grid otomatis menampilkannya.
 *
 * @param id kunci stabil (unik) untuk menu ini.
 * @param route tujuan navigasi: objek rute `@Serializable` (mis. `SalesCanvasRoutes.Hub`); rute
 *        harus sudah didaftarkan di nav graph, atau `navigate()` crash.
 * @param requiredPermission bila diisi, menu hanya tampil bila sesi punya izin tsb (aturan
 *        `UserSession.has`: wildcard `modul:*` / `*`). `null` = selalu tampil.
 */
data class DashboardMenu(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val route: Any,
    val requiredPermission: String? = null,
)

/** Registri menu dashboard; urutan list = urutan tampil di grid (3 kolom). */
val dashboardMenus: List<DashboardMenu> = listOf(
    DashboardMenu(id = "sales_canvas", title = "Sales Kanvas", icon = Icons.Default.LocalShipping, route = SalesCanvasRoutes.Hub),
)
