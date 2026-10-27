package com.mhyddnn.mubarokappmobile.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.BuildConfig
import com.mhyddnn.mubarokappmobile.core.datastore.ThemeMode
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import com.mhyddnn.mubarokappmobile.core.designsystem.AppFilterChip
import com.mhyddnn.mubarokappmobile.core.designsystem.AppSecondaryButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppText
import com.mhyddnn.mubarokappmobile.core.designsystem.BrandHeader
import com.mhyddnn.mubarokappmobile.core.designsystem.InitialsAvatar
import com.mhyddnn.mubarokappmobile.core.designsystem.LabeledRow
import com.mhyddnn.mubarokappmobile.core.designsystem.MenuListItem
import com.mhyddnn.mubarokappmobile.core.designsystem.SectionCard
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusChip
import com.mhyddnn.mubarokappmobile.core.designsystem.StatusTone
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme

private const val PLACEHOLDER = "tidak tersedia"

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        BrandHeader(title = "Profil", onBack = onBack) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(
                    name = session?.employeeName ?: session?.username,
                    size = 56.dp,
                    containerColor = colors.onPrimary,
                    contentColor = colors.primary,
                )
                Column {
                    AppText(
                        text = session?.username ?: "Pengguna",
                        style = AppTheme.typography.titleLarge,
                        color = colors.onPrimary,
                    )
                    session?.roleName?.let {
                        AppText(
                            text = it,
                            style = AppTheme.typography.bodyMedium,
                            color = colors.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IdentitySection(session)
            AppearanceSection(themeMode = themeMode, onSelect = viewModel::setThemeMode)
            PermissionSection(session)
            AppInfoSection()

            MenuListItem(
                title = "Ganti Kata Sandi",
                leadingIcon = Icons.Default.LockReset,
                subtitle = "Belum tersedia",
                enabled = false,
            )
            MenuListItem(
                title = "Bantuan",
                leadingIcon = Icons.AutoMirrored.Filled.HelpOutline,
                subtitle = "Belum tersedia",
                enabled = false,
            )

            AppSecondaryButton(
                text = "Keluar",
                onClick = { viewModel.logout() },
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun IdentitySection(session: UserSession?) {
    SectionCard(title = "Identitas") {
        LabeledRow("Username", session?.username ?: PLACEHOLDER)
        LabeledRow("Nama", session?.employeeName ?: PLACEHOLDER)
        LabeledRow("NIP", PLACEHOLDER)
        LabeledRow("Kontak", PLACEHOLDER)
        LabeledRow("Email", PLACEHOLDER)
        LabeledRow("Divisi", PLACEHOLDER)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSection(themeMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SectionCard(title = "Tampilan") {
        AppText(
            "Tema aplikasi.",
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                AppFilterChip(
                    label = themeLabel(mode),
                    selected = themeMode == mode,
                    onClick = { onSelect(mode) },
                )
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Sistem"
    ThemeMode.LIGHT -> "Terang"
    ThemeMode.DARK -> "Gelap"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionSection(session: UserSession?) {
    SectionCard(title = "Hak Akses") {
        val perms = session?.permissions?.sorted().orEmpty()
        if (perms.isEmpty()) {
            AppText(
                "Tidak ada izin terdaftar.",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                perms.forEach { StatusChip(it, tone = StatusTone.Neutral) }
            }
        }
    }
}

@Composable
private fun AppInfoSection() {
    SectionCard(title = "Aplikasi") {
        LabeledRow("Versi", BuildConfig.VERSION_NAME)
        LabeledRow("Status APP", if (BuildConfig.DEBUG) "Debug" else "Release")
    }
}
