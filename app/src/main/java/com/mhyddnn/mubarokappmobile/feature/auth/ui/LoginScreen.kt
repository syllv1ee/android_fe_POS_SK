package com.mhyddnn.mubarokappmobile.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.BuildConfig
import com.mhyddnn.mubarokappmobile.core.designsystem.AppIcon
import com.mhyddnn.mubarokappmobile.core.designsystem.AppPrimaryButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppText
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextField
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.composeunstyled.UnstyledButton

/**
 * Layar login, dipakai di dua keadaan:
 * - **Login awal**: host navigasi Login; setelah sukses `MainActivity` di redirect ke Home
 * - **Login ulang** (needsRelogin, dibuka dari Dashboard): sesi masih ada, jadi [onLoggedIn]
 *   menutup layar ini kembali ke layar asal. Username terisi otomatis dari sesi.
 *
 * Tombol "Atur Server (Dev)" hanya ada di build debug [mengatur ip server].
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showServerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }
    if (showServerDialog) {
        ServerUrlDialog(onDismiss = { showServerDialog = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(
            text = "Sales Mubarok Mobile",
            style = AppTheme.typography.headlineSmall,
        )

        AppTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = "Username",
            errorText = state.usernameError,
            enabled = !state.loading,
            keyboardType = KeyboardType.Text,
        )
        AppTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            errorText = state.passwordError,
            enabled = !state.loading,
            isPassword = true,
        )

        if (state.generalError != null) {
            AppText(
                text = state.generalError!!,
                color = AppTheme.colors.error,
                style = AppTheme.typography.bodySmall,
            )
        }

        AppPrimaryButton(
            text = "Masuk",
            onClick = viewModel::login,
            enabled = state.canSubmit,
            loading = state.loading,
        )

        if (BuildConfig.DEBUG) {
            UnstyledButton(onClick = { showServerDialog = true }, contentPadding = PaddingValues(8.dp)) {
                AppIcon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = AppTheme.colors.primary,
                )
                AppText(text = "Atur Server (Dev)", color = AppTheme.colors.primary, style = AppTheme.typography.labelLarge)
            }
        }
    }
}
