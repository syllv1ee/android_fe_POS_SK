package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

private val CardPadding = PaddingValues(14.dp)

/**
 * Baris menu serbaguna: ikon (dalam wadah berwarna) + judul + subtitle opsional + chevron.
 * Cocok untuk daftar aksi/pengaturan (mis. di Profil). Dapat dinonaktifkan ([enabled]).
 */
@Composable
fun MenuListItem(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    UnstyledButton(
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        shape = CardShape,
        contentPadding = CardPadding,
        modifier = modifier
            .fillMaxWidth()
            .surface(color = colors.surface, elevation = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .surface(color = colors.primaryContainer, shape = RoundedCornerShape(12.dp))
                    .padding(8.dp),
            ) {
                AppIcon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = title,
                style = typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                AppText(
                    text = subtitle,
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onClick != null) {
            AppIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    }
}
