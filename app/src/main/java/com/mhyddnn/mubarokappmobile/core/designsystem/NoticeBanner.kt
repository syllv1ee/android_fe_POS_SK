package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Notif (mis. "Mode offline", "Login ulang untuk sinkron") + aksi opsional. */
@Composable
fun NoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Info,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val (container, content) = tone.colors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .surface(color = container)
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = text,
            style = AppTheme.typography.bodySmall,
            color = content,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        if (actionText != null && onAction != null) {
            AppTextButton(text = actionText, onClick = onAction, color = content)
        }
    }
}
