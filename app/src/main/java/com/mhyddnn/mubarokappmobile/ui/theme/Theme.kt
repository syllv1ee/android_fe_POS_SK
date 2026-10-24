package com.mhyddnn.mubarokappmobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppColors = compositionLocalOf { LightAppColors }
val LocalAppTypography = compositionLocalOf { AppTypographyDefault }

/** Pengganti `MaterialTheme.colorScheme`/`MaterialTheme.typography`. */
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
    val typography: AppTypography
        @Composable get() = LocalAppTypography.current
}

/**
 * Tema aplikasi. Warna brand (maroon) selalu dipakai apa adanya — tidak ada mode
 * "dynamic color" (Material You) karena itu murni fitur Material3 yang sudah dilepas.
 */
@Composable
fun MubarokAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides AppTypographyDefault,
        content = content,
    )
}
