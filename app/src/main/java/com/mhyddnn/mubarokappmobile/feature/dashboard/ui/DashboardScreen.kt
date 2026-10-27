package com.mhyddnn.mubarokappmobile.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIcon
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIconButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppText
import com.mhyddnn.mubarokappmobile.core.designsystem.BrandHeader
import com.mhyddnn.mubarokappmobile.core.designsystem.MenuShortcutCard
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

/**
 * Beranda
 *
 * Banner: needsRelogin (merah, tombol Login → [onRelogin]) didahulukan; selain itu jumlah data
 * belum terkirim (kuning). Navigasi diserahkan ke pemanggil lewat [onMenuClick] (objek rute).
 */
@Composable
fun DashboardScreen(
    onMenuClick: (Any) -> Unit,
    onOpenProfile: () -> Unit,
    onRelogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val menus by viewModel.menus.collectAsStateWithLifecycle()
    val needsRelogin by viewModel.needsRelogin.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        BrandHeader(
            actions = {
                AppIconButton(onClick = onOpenProfile) {
                    AppIcon(Icons.Default.AccountCircle, contentDescription = "Profil", tint = colors.onPrimary)
                }
            },
        ) {
            AppText(
                text = "Beranda",
                style = typography.labelMedium,
                color = colors.onPrimary.copy(alpha = 0.8f),
            )
            AppText(
                text = "Halo, ${session?.username}",
                style = typography.headlineSmall,
                color = colors.onPrimary,
            )
            session?.roleName?.let {
                AppText(
                    text = it,
                    style = typography.bodyMedium,
                    color = colors.onPrimary.copy(alpha = 0.85f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (needsRelogin) {
                NoticeBanner(
                    text = "Sesi berakhir. Aplikasi tetap bisa dipakai, tapi data baru dikirim " +
                        "ke server setelah login ulang.",
                    tone = StatusTone.Danger,
                    actionText = "Login",
                    onAction = onRelogin,
                )
            } else if (unsyncedCount > 0) {
                NoticeBanner(
                    text = "$unsyncedCount data belum terkirim — dikirim otomatis saat ada sinyal.",
                    tone = StatusTone.Warning,
                )
            }

            MenuGrid(menus = menus, onMenuClick = onMenuClick)

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/** Grid 3 kolom; baris terakhir diisi Spacer agar kartu tidak melebar. */
@Composable
private fun MenuGrid(menus: List<DashboardMenu>, onMenuClick: (Any) -> Unit) {
    if (menus.isEmpty()) {
        AppText(
            text = "Tidak ada menu yang tersedia.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        menus.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { menu ->
                    MenuShortcutCard(
                        label = menu.title,
                        icon = menu.icon,
                        onClick = { onMenuClick(menu.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
