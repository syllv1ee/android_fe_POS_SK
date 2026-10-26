package com.mhyddnn.mubarokappmobile.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composeunstyled.UnstyledDialog
import com.composeunstyled.UnstyledDialogPanel
import com.composeunstyled.UnstyledScrim
import com.composeunstyled.rememberDialogState
import com.mhyddnn.mubarokappmobile.BuildConfig
import com.mhyddnn.mubarokappmobile.core.datastore.SettingsManager
import com.mhyddnn.mubarokappmobile.core.designsystem.AppText
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextButton
import com.mhyddnn.mubarokappmobile.core.designsystem.AppTextField
import com.mhyddnn.mubarokappmobile.core.designsystem.CardShape
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel kecil untuk override URL server (DEV). */
@HiltViewModel
class DevServerViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
) : ViewModel() {

    /** Nilai tersimpan saat ini, atau default BASE_URL bila belum di-override. */
    suspend fun currentOrDefault(): String =
        settingsManager.serverBaseUrl.first() ?: BuildConfig.BASE_URL

    /**
     * Simpan override (null = reset). Fire-and-forget: dialog langsung ditutup; request berikutnya
     * memakai URL baru karena `ServerUrlInterceptor` membaca setting di setiap request.
     */
    fun save(url: String?) {
        viewModelScope.launch { settingsManager.setServerBaseUrl(url) }
    }
}

/**
 * Dialog DEV untuk mengatur URL server (ganti IP tanpa rebuild). Hanya dipakai di build debug.
 * "Simpan" memakai URL yang diketik; "Reset" kembali ke default [BuildConfig.BASE_URL].
 */
@Composable
fun ServerUrlDialog(
    onDismiss: () -> Unit,
    viewModel: DevServerViewModel = hiltViewModel(),
) {
    var url by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        url = viewModel.currentOrDefault()
        loaded = true
    }

    val colors = AppTheme.colors
    val dialogState = rememberDialogState(initiallyVisible = true)

    UnstyledDialog(state = dialogState, onDismiss = onDismiss) {
        UnstyledScrim()
        UnstyledDialogPanel(
            shape = CardShape,
            backgroundColor = colors.surface,
            contentColor = colors.onSurface,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(modifier = Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppText(text = "Server URL (Dev)", style = AppTheme.typography.titleMedium)
                AppText(
                    text = "Hanya host & port yang dipakai. Contoh: http://192.168.1.4:3000/api/",
                    style = AppTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                AppTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "URL Server",
                    enabled = loaded,
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.width(320.dp)) {
                    AppTextButton(text = "Reset default", onClick = { viewModel.save(null); onDismiss() })
                    AppTextButton(text = "Simpan", onClick = { viewModel.save(url); onDismiss() })
                }
            }
        }
    }
}
