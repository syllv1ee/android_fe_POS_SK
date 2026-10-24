package com.mhyddnn.mubarokappmobile.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Palet brand MubarokApp — Maroon / Merah Bata (primary), Gold (secondary),
// Green (tertiary). Dipakai membangun ColorScheme di Theme.kt.
// ============================================================================

// --- Light ---
val md_primary = Color(0xFF8E2A2A)
val md_onPrimary = Color(0xFFFFFFFF)
val md_primaryContainer = Color(0xFFFFDAD5)
val md_onPrimaryContainer = Color(0xFF410003)

val md_secondary = Color(0xFF775A0B)
val md_onSecondary = Color(0xFFFFFFFF)
val md_secondaryContainer = Color(0xFFFFDF9B)
val md_onSecondaryContainer = Color(0xFF261A00)

val md_tertiary = Color(0xFF2E6B4F)
val md_onTertiary = Color(0xFFFFFFFF)
val md_tertiaryContainer = Color(0xFFB1F1CC)
val md_onTertiaryContainer = Color(0xFF002111)

val md_error = Color(0xFFBA1A1A)
val md_onError = Color(0xFFFFFFFF)
val md_errorContainer = Color(0xFFFFDAD6)
val md_onErrorContainer = Color(0xFF410002)

val md_background = Color(0xFFFFF8F6)
val md_onBackground = Color(0xFF231918)
val md_surface = Color(0xFFFFF8F6)
val md_onSurface = Color(0xFF231918)
val md_surfaceVariant = Color(0xFFF5DDDA)
val md_onSurfaceVariant = Color(0xFF534341)
val md_outline = Color(0xFF857371)
val md_outlineVariant = Color(0xFFD8C2BF)
val md_inversePrimary = Color(0xFFFFB3AC)

// --- Dark ---
val md_primary_dark = Color(0xFFFFB3AC)
val md_onPrimary_dark = Color(0xFF571D1D)
val md_primaryContainer_dark = Color(0xFF733330)
val md_onPrimaryContainer_dark = Color(0xFFFFDAD5)

val md_secondary_dark = Color(0xFFE9C16C)
val md_onSecondary_dark = Color(0xFF3F2E00)
val md_secondaryContainer_dark = Color(0xFF5B4300)
val md_onSecondaryContainer_dark = Color(0xFFFFDF9B)

val md_tertiary_dark = Color(0xFF96D5B0)
val md_onTertiary_dark = Color(0xFF003823)
val md_tertiaryContainer_dark = Color(0xFF115138)
val md_onTertiaryContainer_dark = Color(0xFFB1F1CC)

val md_error_dark = Color(0xFFFFB4AB)
val md_onError_dark = Color(0xFF690005)
val md_errorContainer_dark = Color(0xFF93000A)
val md_onErrorContainer_dark = Color(0xFFFFDAD6)

val md_background_dark = Color(0xFF231918)
val md_onBackground_dark = Color(0xFFF1DEDC)
val md_surface_dark = Color(0xFF231918)
val md_onSurface_dark = Color(0xFFF1DEDC)
val md_surfaceVariant_dark = Color(0xFF534341)
val md_onSurfaceVariant_dark = Color(0xFFD8C2BF)
val md_outline_dark = Color(0xFFA08C8A)
val md_outlineVariant_dark = Color(0xFF534341)
val md_inversePrimary_dark = Color(0xFF8E2A2A)

// ============================================================================
// AppColors — pengganti `androidx.compose.material3.ColorScheme`. Hanya token yang
// benar-benar dipakai di layar (lihat pemakaian `AppTheme.colors.*`); tambah field
// baru kalau memang perlu, jangan re-desain nilai `md_*` di atas.
// ============================================================================

data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val scrim: Color,
)

val LightAppColors = AppColors(
    primary = md_primary,
    onPrimary = md_onPrimary,
    primaryContainer = md_primaryContainer,
    onPrimaryContainer = md_onPrimaryContainer,
    background = md_background,
    onBackground = md_onBackground,
    surface = md_surface,
    onSurface = md_onSurface,
    onSurfaceVariant = md_onSurfaceVariant,
    outline = md_outline,
    outlineVariant = md_outlineVariant,
    error = md_error,
    onError = md_onError,
    scrim = Color.Black,
)

val DarkAppColors = AppColors(
    primary = md_primary_dark,
    onPrimary = md_onPrimary_dark,
    primaryContainer = md_primaryContainer_dark,
    onPrimaryContainer = md_onPrimaryContainer_dark,
    background = md_background_dark,
    onBackground = md_onBackground_dark,
    surface = md_surface_dark,
    onSurface = md_onSurface_dark,
    onSurfaceVariant = md_onSurfaceVariant_dark,
    outline = md_outline_dark,
    outlineVariant = md_outlineVariant_dark,
    error = md_error_dark,
    onError = md_onError_dark,
    scrim = Color.Black,
)

// ============================================================================
// Warna semantik status (di luar ColorScheme M3) — dipakai StatusChip.
// Sengaja independen dari brand agar makna (sukses/peringatan/dll) tetap jelas.
// ============================================================================

data class StatusToneColors(val container: Color, val content: Color)

data class StatusPalette(
    val neutral: StatusToneColors,
    val info: StatusToneColors,
    val success: StatusToneColors,
    val warning: StatusToneColors,
    val danger: StatusToneColors,
)

val LightStatusPalette = StatusPalette(
    neutral = StatusToneColors(Color(0xFFE2E8F0), Color(0xFF334155)),
    info = StatusToneColors(Color(0xFFDBEAFE), Color(0xFF1E40AF)),
    success = StatusToneColors(Color(0xFFD1FAE5), Color(0xFF065F46)),
    warning = StatusToneColors(Color(0xFFFEF3C7), Color(0xFF92400E)),
    danger = StatusToneColors(Color(0xFFFEE2E2), Color(0xFF991B1B)),
)

val DarkStatusPalette = StatusPalette(
    neutral = StatusToneColors(Color(0xFF334155), Color(0xFFE2E8F0)),
    info = StatusToneColors(Color(0xFF1E3A8A), Color(0xFFBFDBFE)),
    success = StatusToneColors(Color(0xFF064E3B), Color(0xFFA7F3D0)),
    warning = StatusToneColors(Color(0xFF78350F), Color(0xFFFDE68A)),
    danger = StatusToneColors(Color(0xFF7F1D1D), Color(0xFFFECACA)),
)
