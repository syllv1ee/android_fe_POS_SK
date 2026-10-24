package com.mhyddnn.mubarokappmobile.feature.auth.data

import com.google.gson.annotations.SerializedName

/** Body `POST /auth/login`. */
data class LoginRequest(
    val username: String,
    val password: String,
)

/** Payload `data` dari login (`{ user: {...} }`). */
data class LoginData(
    val user: UserDto,
)

/**
 * `data` dari `GET /auth/profile` dan `data.user` dari login.
 *
 * Catatan: `last_login_at` sengaja TIDAK dipetakan — BE mengirimnya sebagai object (bukan
 * string) dan app tidak memakainya, jadi dibiarkan di-skip Gson agar tidak gagal parse.
 */
data class UserDto(
    val id: Int,
    val username: String,
    @SerializedName("is_active") val isActive: Boolean = true,
    val employee: EmployeeRef? = null,
    val role: RoleRef? = null,
    /** Hanya dikirim `GET /auth/profile` (flat). `POST /auth/login` kirim nested lewat [RoleRef.permissionRoles]. */
    val permissions: List<PermissionRef>? = null,
)

data class RoleRef(
    val id: Int,
    val name: String,
    /** Hanya dikirim `POST /auth/login` — junction permission via role, bukan field flat. */
    val permissionRoles: List<PermissionRoleRef>? = null,
)

data class PermissionRoleRef(val permission: PermissionRef)

data class EmployeeRef(val id: Int, val name: String)

data class PermissionRef(val id: Int, val name: String)
