package com.mhyddnn.mubarokappmobile.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val loggedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotBlank() && !loading
}

/**
 * State form login. Username di-trim sebelum dikirim, password tidak (spasi bisa bagian password).
 * Mengubah isian menghapus error field itu & error umum, agar pesan lama tidak ada lagi.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Login ulang (token habis): sesi lokal masih ada -> isi username pemilik data antrean.
        viewModelScope.launch {
            authRepository.session.first()?.let { s -> _uiState.update { it.copy(username = s.username) } }
        }
    }

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, usernameError = null, generalError = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }

    fun login() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(loading = true, generalError = null) }

        viewModelScope.launch {
            when (val result = authRepository.login(state.username.trim(), state.password)) {
                // Sesi tersimpan → MainActivity (mengamati isLoggedIn) pindah ke Home; untuk login
                // ulang, layar menutup sendiri lewat [LoginUiState.loggedIn]. loading dibiarkan true
                // agar tombol tidak bisa ditekan lagi.
                is Result.Success -> _uiState.update { it.copy(loggedIn = true) }
                is Result.Error -> _uiState.update {
                    it.copy(
                        loading = false,
                        usernameError = result.fieldErrors?.get("username"),
                        passwordError = result.fieldErrors?.get("password"),
                        generalError = result.message,
                    )
                }
                Result.Loading -> Unit
            }
        }
    }
}
