package com.mhyddnn.mubarokappmobile.feature.auth.data

import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.common.map
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import com.mhyddnn.mubarokappmobile.core.datastore.isSuperAdminRole
import com.mhyddnn.mubarokappmobile.core.network.SessionCookieJar
import com.mhyddnn.mubarokappmobile.core.network.safeApiCall
import com.mhyddnn.mubarokappmobile.core.network.safeApiCallUnit
import com.mhyddnn.mubarokappmobile.core.sync.SyncScheduler
import com.mhyddnn.mubarokappmobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
    private val cookieJar: SessionCookieJar,
    private val syncScheduler: SyncScheduler,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    override val session: Flow<UserSession?> = sessionManager.session

    override val needsRelogin: Flow<Boolean> = sessionManager.needsRelogin

    override suspend fun login(username: String, password: String): Result<UserSession> {
        val result = safeApiCall { api.login(LoginRequest(username, password)) }
            .map { it.user.toSession() }
        if (result is Result.Success) {
            sessionManager.save(result.data)
            // Login (termasuk login ulang setelah token habis) → kirim antrean yang tertahan.
            syncScheduler.schedule()
        }
        return result
    }

    override suspend fun logout(): Result<Unit> {
        val result = safeApiCallUnit { api.logout() }
        // Apapun hasil server, sesi lokal & cookie wajib dibersihkan.
        cookieJar.clear()
        sessionManager.clear()
        return if (result is Result.Error && result.code == 401) Result.Success(Unit) else result
    }
}

private fun UserDto.toSession(): UserSession = UserSession(
    username = username,
    employeeName = employee?.name,
    roleName = role?.name,
    // login kirim nested (role.permissionRoles), profile kirim flat (permissions).
    permissions = (role?.permissionRoles?.map { it.permission.name } ?: permissions?.map { it.name })
        .orEmpty()
        .toSet(),
    isSuperAdmin = isSuperAdminRole(role?.name, role?.id),
)
