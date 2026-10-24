package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

private val CardPadding = PaddingValues(16.dp)

/**
 * Kartu pintasan menu untuk dashboard: ikon di dalam wadah berwarna + label di bawahnya.
 * Domain-agnostik & dapat diklik. Dipakai grid menu (lihat dashboard).
 */
@Composable
fun MenuShortcutCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    UnstyledButton(
        onClick = onClick,
        shape = CardShape,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .surface(color = colors.surface, elevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .surface(color = colors.primaryContainer, shape = RoundedCornerShape(16.dp))
                    .aspectRatio(1f)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = colors.onPrimaryContainer,
                )
            }
            AppText(
                text = label,
                style = AppTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
