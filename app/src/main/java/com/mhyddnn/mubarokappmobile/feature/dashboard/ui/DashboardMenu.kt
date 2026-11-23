
package com.mhyddnn.mubarokappmobile.feature.dashboard.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector
import com.mhyddnn.mubarokappmobile.feature.salescanvas.SalesCanvasRoutes
import com.mhyddnn.mubarokappmobile.feature.visit.VisitRoutes

data class DashboardMenu(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val route: Any,
    val requiredPermission: String? = null,
)

val dashboardMenus: List<DashboardMenu> = listOf(

    DashboardMenu(
        id = "sales_canvas",
        title = "Sales Kanvas",
        icon = Icons.Default.LocalShipping,
        route = SalesCanvasRoutes.Hub,
    ),

    DashboardMenu(
        id = "visit",
        title = "Kunjungan",
        icon = Icons.Default.Store,
        route = VisitRoutes.Index,
        requiredPermission = "visit:read",
    ),
)
