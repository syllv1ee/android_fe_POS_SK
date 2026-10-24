package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Pengganti `androidx.compose.material3.Icon` — Compose Unstyled tidak punya komponen ikon
 * bertinting, jadi ini tipis di atas `Image` + `rememberVectorPainter` Compose Foundation.
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier.size(24.dp),
    tint: androidx.compose.ui.graphics.Color = AppTheme.colors.onSurface,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = if (tint.isSpecified) ColorFilter.tint(tint) else null,
    )
}
