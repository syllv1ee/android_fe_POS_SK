package com.mhyddnn.mubarokappmobile.feature.salescanvas

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.mhyddnn.mubarokappmobile.feature.plan.ui.PlanDetailScreen
import com.mhyddnn.mubarokappmobile.feature.plan.ui.PlanFormScreen
import com.mhyddnn.mubarokappmobile.feature.plan.ui.PlanListScreen
import com.mhyddnn.mubarokappmobile.feature.salescanvas.ui.SalesCanvasPlaceholderScreen
import com.mhyddnn.mubarokappmobile.feature.salescanvas.ui.SalesCanvasScreen

/** Rute milik fitur Sales Kanvas. Rute fitur tinggal di fiturnya, bukan di `core/navigation`. */
object SalesCanvasRoutes {
    @Serializable data object Hub
    @Serializable data object Plan
    @Serializable data object PlanCreate
    /** Dibaca `PlanDetailViewModel` lewat `SavedStateHandle.toRoute()`. */
    @Serializable data class PlanDetail(val id: Int)
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
    composable<SalesCanvasRoutes.Plan> {
        PlanListScreen(
            onPlanClick = { navController.navigate(SalesCanvasRoutes.PlanDetail(it)) },
            onAddClick = { navController.navigate(SalesCanvasRoutes.PlanCreate) },
            onBack = onBack,
        )
    }
    composable<SalesCanvasRoutes.PlanCreate> {
        PlanFormScreen(
            onCreated = { id ->
                // Buat ulang daftar (agar plan baru ikut tampil), lalu buka detail di atasnya.
                navController.navigate(SalesCanvasRoutes.Plan) {
                    popUpTo<SalesCanvasRoutes.Plan> { inclusive = true }
                }
                navController.navigate(SalesCanvasRoutes.PlanDetail(id))
            },
            // Offline: plan belum punya id → kembali ke daftar, tampil sebagai "Belum terkirim".
            onQueued = onBack,
            onBack = onBack,
        )
    }
    composable<SalesCanvasRoutes.PlanDetail> {
        PlanDetailScreen(onBack = onBack)
    }
    composable<SalesCanvasRoutes.Sale> {
        // ponytail: modul penjualan belum dibuat (endpoint belum ada di sales-kanvas-be) → menu
        // sengaja tetap ada sebagai placeholder. Buat dengan mencontek `feature/plan` (CLAUDE.md).
        SalesCanvasPlaceholderScreen(title = "Penjualan", onBack = onBack)
    }
    composable<SalesCanvasRoutes.Return> {
        SalesCanvasPlaceholderScreen(title = "Retur", onBack = onBack)
    }
    composable<SalesCanvasRoutes.Billing> {
        SalesCanvasPlaceholderScreen(title = "Tagihan", onBack = onBack)
    }
}
