package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Chip pilihan (sample digunakan pada mode tema) */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    UnstyledButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        backgroundColor = if (selected) colors.primaryContainer else colors.surface,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        borderColor = if (selected) colors.primaryContainer else colors.outline,
        borderWidth = 1.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = modifier,
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.labelLarge,
            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        )
    }
}
