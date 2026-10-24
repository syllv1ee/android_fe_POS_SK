package com.mhyddnn.mubarokappmobile.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.core.VerticalSeparator
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIcon
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIconButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppText
import com.mhyddnn.mubarokappmobile.core.designsystem.BrandHeader
import com.mhyddnn.mubarokappmobile.core.designsystem.MenuShortcutCard
import com.mhyddnn.mubarokappmobile.core.designsystem.NoticeBanner
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.core.designsystem.surface
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

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
    // val summary by viewModel.summary.collectAsStateWithLifecycle()
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
                text = "Halo, ${session?.employeeName ?: session?.username ?: "Sales"}",
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
            // SummaryCard(summary = summary)

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

@Composable
private fun SummaryCard(summary: DashboardSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .surface(color = AppTheme.colors.surface, elevation = 3.dp)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SummaryStat(
            label = "Trip",
            value = summary.tripCount?.toString() ?: "—",
            modifier = Modifier.weight(1f),
        )
        VerticalSeparator(color = AppTheme.colors.outlineVariant, modifier = Modifier.height(36.dp))
        SummaryStat(
            label = "Penjualan",
            value = summary.saleCount?.toString() ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AppText(
            text = value,
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.primary,
        )
        AppText(
            text = label,
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun MenuGrid(menus: List<DashboardMenu>, onMenuClick: (Any) -> Unit) {
    if (menus.isEmpty()) {
        AppText(
            text = "Tidak ada menu yang tersedia untuk akun Anda.",
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
