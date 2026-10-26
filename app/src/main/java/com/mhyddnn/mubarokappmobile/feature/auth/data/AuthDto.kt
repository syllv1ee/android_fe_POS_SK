package com.mhyddnn.mubarokappmobile.feature.auth.data

import com.google.gson.annotations.SerializedName

/**
 * Body `POST /auth/login`. Password dikirim apa adanya — keamanannya bergantung pada HTTPS
 * (release hanya mengizinkan HTTPS, lihat `network_security_config.xml`).
 */
data class LoginRequest(
    val username: String,
    val password: String,
)

/** Payload `data` dari login: token di body (sales-kanvas-be tidak memakai cookie) + user. */
data class LoginData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    val user: UserDto,
)

/**
 * `data` dari `GET /auth/me` dan `data.user` dari login (`toSafeUser()` di BE): permission
 * dikirim flat, role hanya `{ id, name }`.
 *
 * Catatan: tanggal (`last_login_at`, dst.) sengaja tidak dipetakan — app tidak memakainya.
 */
data class UserDto(
    val id: Int,
    val username: String,
    val name: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    val role: RoleRef? = null,
    val permissions: List<PermissionRef>? = null,
)

/** Role user; `id`/`name` dipakai untuk deteksi Super Admin (`isSuperAdminRole`). */
data class RoleRef(val id: Int, val name: String)

/** Satu permission; hanya `name` (mis. `plan:write`) yang disimpan ke sesi. */
data class PermissionRef(val id: Int, val name: String)
