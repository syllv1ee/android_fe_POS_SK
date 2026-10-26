package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/** Kerangka layar standar*/
@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = AppTheme.colors
    // Ikon status bar mengikuti terang/gelapnya warna top bar (primaryContainer).
    ApplyStatusBarIcons(darkIcons = colors.primaryContainer.luminance() > 0.5f)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .surface(color = colors.primaryContainer)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = colors.onPrimaryContainer,
                        )
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                }
                AppText(
                    text = title,
                    style = AppTheme.typography.titleLarge,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                actions()
            }
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(0.dp))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            floatingActionButton()
        }
    }
}
