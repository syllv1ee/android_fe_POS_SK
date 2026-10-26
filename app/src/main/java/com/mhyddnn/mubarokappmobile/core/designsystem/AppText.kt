package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Text Standar */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val resolvedColor = when {
        color.isSpecified -> color
        style.color.isSpecified -> style.color
        else -> AppTheme.colors.onSurface
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = resolvedColor,
            textAlign = textAlign ?: style.textAlign,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}
