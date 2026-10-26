package com.mhyddnn.mubarokappmobile.feature.auth.domain

import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.datastore.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Login, logout, dan status sesi. Semua Flow berasal dari `SessionManager` (DataStore): memancarkan
 * nilai awal lalu setiap kali sesi berubah, tidak pernah selesai.
 */
interface AuthRepository {

    /** Menentukan host navigasi di `MainActivity` (Login vs Home). */
    val isLoggedIn: Flow<Boolean>

    /** Profil user aktif; null bila belum login. */
    val session: Flow<UserSession?>

    /** true = token tak bisa diperbarui lagi; app tetap jalan, tapi perlu login ulang untuk sinkron. */
    val needsRelogin: Flow<Boolean>

    /**
     * `POST /auth/login`. Sukses → token disimpan, lalu sesi & permission, lalu sinkron outbox
     * dipicu. Error → [Result.Error] dengan `fieldErrors` (`username`/`password`) bila validasi.
     * Dipakai juga untuk login ulang saat needsRelogin (sesi lama ditimpa).
     */
    suspend fun login(username: String, password: String): Result<UserSession>

    /**
     * Logout di server (best-effort, dibatasi waktu) + bersihkan token & sesi lokal. Selalu
     * [Result.Success]: lokal pasti bersih walau offline. Outbox & cache user ini tidak dihapus.
     */
    suspend fun logout(): Result<Unit>
}
