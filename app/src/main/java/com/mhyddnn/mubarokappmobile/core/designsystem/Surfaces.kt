package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Radius standar kartu/field di seluruh app — pengganti `MaterialTheme.shapes` M3. */
val CardShape: Shape = RoundedCornerShape(12.dp)
val FieldShape: Shape = RoundedCornerShape(8.dp)

/**
 * Pengganti `Surface`/`Card` M3: latar warna + sudut membulat, dengan bayangan/border opsional.
 * Dipakai lewat modifier chain, mis. `Modifier.surface(color = AppTheme.colors.surface)`.
 */
fun Modifier.surface(
    color: Color,
    shape: Shape = CardShape,
    elevation: Dp = 0.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
): Modifier {
    var m = this
    if (elevation > 0.dp) m = m.shadow(elevation, shape, clip = false)
    m = m.clip(shape).background(color, shape)
    if (borderColor != null) m = m.border(borderWidth, borderColor, shape)
    return m
}
