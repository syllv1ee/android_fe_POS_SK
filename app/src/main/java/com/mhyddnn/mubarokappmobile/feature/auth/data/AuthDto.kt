package com.mhyddnn.mubarokappmobile.feature.auth.data

import com.google.gson.annotations.SerializedName

/**
 * Body `POST /auth/login`.
 */
data class LoginRequest(
    val username: String,
    val password: String,
)

/** Payload `data` dari login: token di body + user. */
data class LoginData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    val user: UserDto,
)

/**
 * `data` dari `GET /auth/me`
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

/** Permission */
data class PermissionRef(val id: Int, val name: String)
