package com.mhyddnn.mubarokappmobile.feature.auth.data

import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.common.map
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import com.mhyddnn.mubarokappmobile.core.datastore.isSuperAdminRole
import com.mhyddnn.mubarokappmobile.core.network.TokenStore
import com.mhyddnn.mubarokappmobile.core.network.safeApiCall
import com.mhyddnn.mubarokappmobile.core.network.safeApiCallUnit
import com.mhyddnn.mubarokappmobile.core.sync.SyncScheduler
import com.mhyddnn.mubarokappmobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Implementasi [AuthRepository]. Token → [TokenStore] (dibaca OkHttp), profil → [SessionManager]
 * (dibaca UI). Urutan simpan saat login penting: token dulu, sesi kemudian — begitu sesi tersimpan,
 * `MainActivity` pindah ke Home dan request pertama di sana sudah harus ber-Bearer.
 */
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
    private val tokens: TokenStore,
    private val syncScheduler: SyncScheduler,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    override val session: Flow<UserSession?> = sessionManager.session

    override val needsRelogin: Flow<Boolean> = sessionManager.needsRelogin

    override suspend fun login(username: String, password: String): Result<UserSession> {
        val login = safeApiCall { api.login(LoginRequest(username, password)) }
        if (login is Result.Success) {
            // Token dulu: request berikutnya (termasuk pengiriman antrean sinkron) langsung butuh Bearer.
            tokens.save(login.data.accessToken, login.data.refreshToken)
        }
        val result = login.map { it.user.toSession() }
        if (result is Result.Success) {
            sessionManager.save(result.data)
            // Login (termasuk login ulang setelah token habis) → kirim antrean yang tertahan.
            syncScheduler.schedule()
        }
        return result
    }

    /**
     * Logout harus jalan tanpa sinyal: request server hanya best-effort (dibatasi
     * [LOGOUT_TIMEOUT_MS]), sesi lokal & token SELALU dibersihkan — juga bila coroutine dibatalkan.
     * Request logout dikirim sebelum token dihapus, karena butuh Bearer.
     * Antrean sinkron user ini tidak dihapus; terkirim saat user yang sama login lagi.
     */
    override suspend fun logout(): Result<Unit> {
        try {
            withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { safeApiCallUnit { api.logout() } }
        } finally {
            withContext(NonCancellable) {
                tokens.clear()
                sessionManager.clear()
            }
        }
        return Result.Success(Unit)
    }

    private companion object {
        // ponytail: 3 dtk cukup untuk sinyal normal; gagal/timeout = token server dibiarkan kedaluwarsa sendiri.
        const val LOGOUT_TIMEOUT_MS = 3_000L
    }
}

/** `data.user` login → profil lokal. Permission diratakan jadi set nama (mis. `plan:write`). */
private fun UserDto.toSession(): UserSession = UserSession(
    username = username,
    // sales-kanvas-be tidak punya relasi employee di user; nama tampilan = `user.name`.
    employeeName = name,
    roleName = role?.name,
    permissions = permissions.orEmpty().map { it.name }.toSet(),
    isSuperAdmin = isSuperAdminRole(role?.name, role?.id),
)
