package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

private val ButtonPadding = PaddingValues(horizontal = 16.dp)
private val ButtonArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)

/**
 * Tombol primer lebar-penuh dengan dukungan state loading.
 * Saat [loading] true, tombol dinonaktifkan dan menampilkan spinner.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = AppTheme.colors
    UnstyledButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CardShape,
        backgroundColor = if (enabled) colors.primary else colors.outlineVariant,
        contentColor = colors.onPrimary,
        contentPadding = ButtonPadding,
        horizontalArrangement = ButtonArrangement,
        modifier = modifier.fillMaxWidth().height(50.dp),
    ) {
        ButtonContent(text = text, loading = loading, color = colors.onPrimary)
    }
}

/** Tombol sekunder (outline) lebar-penuh. */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = AppTheme.colors
    UnstyledButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CardShape,
        borderColor = colors.outline,
        borderWidth = 1.dp,
        contentColor = colors.primary,
        contentPadding = ButtonPadding,
        horizontalArrangement = ButtonArrangement,
        modifier = modifier.fillMaxWidth().height(50.dp),
    ) {
        ButtonContent(text = text, loading = loading, color = colors.primary)
    }
}

/** Tombol teks polos (tanpa latar/border). */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AppTheme.colors.primary,
) {
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        shape = FieldShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier,
    ) {
        AppText(text = text, style = AppTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun ButtonContent(text: String, loading: Boolean, color: Color) {
    if (loading) {
        AppSpinner(color = color, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    }
    AppText(text = text, color = color, style = AppTheme.typography.labelLarge)
}
