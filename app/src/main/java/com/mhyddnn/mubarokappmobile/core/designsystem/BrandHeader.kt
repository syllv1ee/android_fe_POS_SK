package com.mhyddnn.mubarokappmobile.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Header brand melengkung (maroon) yang menembus status bar — dipakai sebagai "top bar" kaya
 * pada Dashboard & Profil agar konsisten. Menyediakan baris atas (back opsional + judul opsional
 * + [actions]) lalu slot [content] (mis. sapaan / identitas). Bagian bawah diberi ruang ekstra
 * agar konten berikutnya bisa "mengambang" menimpa header (pakai offset negatif di pemanggil).
 */
@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    // Ikon status bar mengikuti terang/gelapnya warna header (primary).
    ApplyStatusBarIcons(darkIcons = colors.primary.luminance() > 0.5f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .surface(color = colors.primary, shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 44.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                AppIconButton(onClick = onBack) {
                    AppIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = colors.onPrimary,
                    )
                }
            }
            if (title != null) {
                AppText(
                    text = title,
                    style = AppTheme.typography.titleLarge,
                    color = colors.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            actions()
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
