package com.mhyddnn.mubarokappmobile.feature.auth.domain

import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Menentukan navigasi di `MainActivity`. */
    val isLoggedIn: Flow<Boolean>

    /** Profil user aktif; null bila belum login. */
    val session: Flow<UserSession?>

    /** true = token tak bisa diperbarui lagi; app tetap jalan, tapi perlu login ulang untuk sinkron. */
    val needsRelogin: Flow<Boolean>

    /**
     * `POST /auth/login`. Sukses → token disimpan, lalu sesi & permission, lalu pengiriman antrean sinkron
     * dipicu. Error → [Result.Error] dengan `fieldErrors` (`username`/`password`) bila validasi.
     * Dipakai juga untuk login ulang saat needsRelogin (sesi lama ditimpa).
     */
    suspend fun login(username: String, password: String): Result<UserSession>

    /**
     * Logout di server (best-effort, dibatasi waktu) + bersihkan token & sesi lokal. Selalu
     * [Result.Success]: lokal pasti bersih walau offline. Antrean sinkron & cache user ini tidak dihapus.
     */
    suspend fun logout(): Result<Unit>
}
