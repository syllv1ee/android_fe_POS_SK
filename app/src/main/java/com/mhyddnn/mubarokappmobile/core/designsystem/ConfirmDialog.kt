package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledDialog
import com.composeunstyled.UnstyledDialogPanel
import com.composeunstyled.UnstyledScrim
import com.composeunstyled.rememberDialogState
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Dialog konfirmasi Batal / [confirmText]. Tampilkan dengan `if (show) ConfirmDialog(...)`. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    UnstyledDialog(state = rememberDialogState(initiallyVisible = true), onDismiss = onDismiss) {
        UnstyledScrim()
        UnstyledDialogPanel(
            shape = CardShape,
            backgroundColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppText(text = title, style = AppTheme.typography.titleMedium)
                AppText(
                    text = message,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AppTextButton(text = "Batal", onClick = onDismiss)
                    AppTextButton(
                        text = confirmText,
                        color = if (destructive) AppTheme.colors.error else AppTheme.colors.primary,
                        onClick = { onDismiss(); onConfirm() },
                    )
                }
            }
        }
    }
}
