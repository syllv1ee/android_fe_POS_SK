package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Indikator loading di tengah layar. */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppSpinner(color = AppTheme.colors.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
    }
}

/** Tampilan daftar kosong. */
@Composable
fun EmptyView(
    message: String = "Belum ada data",
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox,
) {
    MessageState(icon = icon, message = message, modifier = modifier)
}

/** Tampilan error dengan tombol "Coba lagi" opsional. */
@Composable
fun ErrorView(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.WifiOff,
) {
    MessageState(icon = icon, message = message, modifier = modifier) {
        if (onRetry != null) {
            AppSecondaryButton(text = "Coba lagi", onClick = onRetry)
        }
    }
}

@Composable
private fun MessageState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = colors.outline,
        )
        AppText(
            text = message,
            style = AppTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = colors.onSurfaceVariant,
        )
        action?.invoke()
    }
}
