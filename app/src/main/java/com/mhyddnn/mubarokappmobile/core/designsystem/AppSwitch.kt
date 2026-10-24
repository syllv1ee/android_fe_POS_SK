package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledToggleSwitch
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Pengganti `Switch` M3. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    UnstyledToggleSwitch(
        toggled = checked,
        onToggled = if (enabled) onCheckedChange else null,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        backgroundColor = if (checked) colors.primary else colors.outlineVariant,
        contentPadding = PaddingValues(3.dp),
        thumb = {
            Box(modifier = Modifier.size(18.dp).surface(color = colors.onPrimary, shape = CircleShape))
        },
    )
}
