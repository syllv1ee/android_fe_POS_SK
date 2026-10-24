package com.mhyddnn.mubarokappmobile.core.navigation

import kotlinx.serialization.Serializable

/**
 * Rute kerangka aplikasi (type-safe, `@Serializable`). Rute fitur didefinisikan di fiturnya
 * (mis. `SalesCanvasRoutes`). Rute dengan argumen cukup jadi `data class`, mis.
 * `@Serializable data class SaleDetail(val id: Int)` → baca via `backStackEntry.toRoute()`.
 */
object Routes {
    @Serializable data object Login

    /** Dashboard utama (landing setelah login). */
    @Serializable data object Home

    /** Halaman profil/akun. */
    @Serializable data object Profile
}
