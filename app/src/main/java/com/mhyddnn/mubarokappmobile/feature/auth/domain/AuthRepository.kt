package com.mhyddnn.mubarokappmobile.feature.auth.domain

import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val isLoggedIn: Flow<Boolean>

    val session: Flow<UserSession?>

    /** true = token tak bisa diperbarui lagi; app tetap jalan, tapi perlu login ulang untuk sinkron. */
    val needsRelogin: Flow<Boolean>

    /** Login + simpan sesi & permission. Cookie token disimpan otomatis oleh CookieJar. */
    suspend fun login(username: String, password: String): Result<UserSession>

    /** Logout di server + bersihkan cookie & sesi lokal (selalu bersihkan lokal). */
    suspend fun logout(): Result<Unit>
}
