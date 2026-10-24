package com.mhyddnn.mubarokappmobile.feature.salescanvas

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.mhyddnn.mubarokappmobile.feature.sale.ui.SaleListScreen
import com.mhyddnn.mubarokappmobile.feature.salescanvas.ui.SalesCanvasPlaceholderScreen
import com.mhyddnn.mubarokappmobile.feature.salescanvas.ui.SalesCanvasScreen

/** Rute milik fitur Sales Kanvas. Rute fitur tinggal di fiturnya, bukan di `core/navigation`. */
object SalesCanvasRoutes {
    @Serializable data object Hub
    @Serializable data object Sale
    @Serializable data object Return
    @Serializable data object Billing
}

/**
 * Semua layar Sales Kanvas. Didaftarkan di `AppNavHost` cukup dengan satu baris
 * `salesCanvasGraph(navController)` — tambah/hapus layar fitur ini hanya di file ini.
 */
fun NavGraphBuilder.salesCanvasGraph(navController: NavHostController) {
    val onBack: () -> Unit = { navController.popBackStack() }

    composable<SalesCanvasRoutes.Hub> {
        SalesCanvasScreen(onMenuClick = { navController.navigate(it) }, onBack = onBack)
    }
    composable<SalesCanvasRoutes.Sale> {
        SaleListScreen(onBack = onBack)
    }
    composable<SalesCanvasRoutes.Return> {
        SalesCanvasPlaceholderScreen(title = "Retur", onBack = onBack)
    }
    composable<SalesCanvasRoutes.Billing> {
        SalesCanvasPlaceholderScreen(title = "Tagihan", onBack = onBack)
    }
}
