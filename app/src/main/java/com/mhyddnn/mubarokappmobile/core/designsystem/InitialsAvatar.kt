package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Avatar lingkaran berisi inisial nama (mis. "Budi Santoso" → "BS"). Dipakai header profil &
 * tempat lain yang butuh representasi user tanpa gambar.
 */
@Composable
fun InitialsAvatar(
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = AppTheme.colors.primary,
    contentColor: Color = AppTheme.colors.onPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .surface(color = containerColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = initials(name),
            style = AppTheme.typography.titleMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

private fun initials(name: String?): String {
    val parts = name?.trim()?.split(Regex("\\s+")).orEmpty().filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
