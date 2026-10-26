package com.mhyddnn.mubarokappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.datastore.SettingsManager
import com.mhyddnn.mubarokappmobile.core.datastore.ThemeMode
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import com.mhyddnn.mubarokappmobile.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State awal aplikasi: null = masih memuat sesi (tampilkan splash), true/false = sudah pasti. */
@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    settingsManager: SettingsManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    /** Mode tema pilihan user (override tema sistem). null = belum terbaca → tahan di splash. */
    val themeMode: StateFlow<ThemeMode?> = settingsManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoggedIn: StateFlow<Boolean?> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // DEV ONLY (BuildConfig.SKIP_LOGIN, debug saja): semai sesi dev agar langsung masuk
        // tanpa login. Tidak ada token, jadi panggilan API asli tetap 401 — ini hanya
        // untuk mempercepat pengembangan UI. Nonaktifkan via SKIP_LOGIN=false di build.gradle.
        if (BuildConfig.SKIP_LOGIN) {
            viewModelScope.launch {
                if (!sessionManager.isLoggedIn.first()) {
                    sessionManager.save(DEV_SESSION)
                }
            }
        }
    }

    private companion object {
        val DEV_SESSION = UserSession(
            username = "dev",
            employeeName = "Developer",
            roleName = "Developer",
            permissions = setOf("sales-kanvas:*"),
        )
    }
}
