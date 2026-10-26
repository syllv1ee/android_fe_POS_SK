package com.mhyddnn.mubarokappmobile.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.datastore.SettingsManager
import com.mhyddnn.mubarokappmobile.core.datastore.ThemeMode
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import com.mhyddnn.mubarokappmobile.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Profil: baca sesi (tanpa request), ubah tema (langsung berlaku di seluruh app), logout. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    val session: StateFlow<UserSession?> = authRepository.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val themeMode: StateFlow<ThemeMode> = settingsManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    /** Disimpan ke DataStore; `MainViewModel` mengamati nilai yang sama → tema berganti seketika. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsManager.setThemeMode(mode) }
    }

    /**
     * Sesi dibersihkan → MainActivity (mengamati isLoggedIn) otomatis kembali ke Login. Paling
     * lama ±3 dtk (batas request logout ke server); offline tetap berhasil.
     */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
