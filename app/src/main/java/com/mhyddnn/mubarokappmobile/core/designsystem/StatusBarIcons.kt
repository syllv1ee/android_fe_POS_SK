package com.mhyddnn.mubarokappmobile.core.designsystem

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Mengatur warna ikon status bar untuk layar saat ini (app memakai edge-to-edge, konten
 * menembus status bar). [darkIcons] = true → ikon gelap (untuk latar terang), false → ikon
 * terang (untuk latar gelap, mis. header brand maroon).
 */
@Composable
fun ApplyStatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
    }
}
