package com.mhyddnn.mubarokappmobile.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session")

/** Profil ringkas user yang sedang login (dipakai UI untuk header & gating menu). */
data class UserSession(
    val username: String,
    val employeeName: String?,
    val roleName: String?,
    val permissions: Set<String>,
    /** Super Admin → bypass semua permission (lihat [isSuperAdminRole] & doc_rbac.md §6). */
    val isSuperAdmin: Boolean = false,
)

/**
 * Deteksi role Super Admin, konsisten dengan `PermissionsGuard.isSuperAdmin()` di backend
 * (doc_rbac.md §6): cocok salah satu nama (case-insensitive) `super admin`/`superadmin`/
 * `super-admin`, ATAU fallback `role.id == 1`.
 */
fun isSuperAdminRole(roleName: String?, roleId: Int?): Boolean {
    val name = roleName?.trim()?.lowercase()
    return name == "super admin" ||
        name == "superadmin" ||
        name == "super-admin" ||
        roleId == 1
}

/**
 * Cek izin di sisi UI (mis. menampilkan/menyembunyikan menu). Super Admin selalu lolos.
 * Selain itu mendukung wildcard `modul:*` dan super `*`.
 */
fun UserSession.has(permission: String): Boolean =
    isSuperAdmin ||
        permission in permissions ||
        "${permission.substringBefore(':')}:*" in permissions ||
        "*" in permissions

/**
 * Menyimpan status login & daftar permission user. Token TIDAK disimpan di sini —
 * token ada di cookie ([com.mhyddnn.mubarokappmobile.core.network.SessionCookieJar]).
 * Permission dipakai untuk menyembunyikan/menonaktifkan aksi (mis. tombol "Simpan
 * Penjualan" hanya muncul bila ada `sales-kanvas:write`).
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val USERNAME = stringPreferencesKey("username")
        val EMPLOYEE = stringPreferencesKey("employee_name")
        val ROLE = stringPreferencesKey("role_name")
        val PERMISSIONS = stringSetPreferencesKey("permissions")
        val IS_SUPER_ADMIN = booleanPreferencesKey("is_super_admin")
        val NEEDS_RELOGIN = booleanPreferencesKey("needs_relogin")
    }

    val isLoggedIn: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.LOGGED_IN] ?: false }

    val session: Flow<UserSession?> =
        context.dataStore.data.map { prefs ->
            if (prefs[Keys.LOGGED_IN] != true) return@map null
            UserSession(
                username = prefs[Keys.USERNAME].orEmpty(),
                employeeName = prefs[Keys.EMPLOYEE],
                roleName = prefs[Keys.ROLE],
                permissions = prefs[Keys.PERMISSIONS] ?: emptySet(),
                isSuperAdmin = prefs[Keys.IS_SUPER_ADMIN] ?: false,
            )
        }

    /**
     * true = refresh token ditolak server (habis 7 hari / dicabut). Sesi lokal SENGAJA tetap ada
     * agar app tetap bisa dipakai offline; UI cukup meminta login ulang, dan sinkron outbox
     * berhenti sampai login berhasil.
     */
    val needsRelogin: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NEEDS_RELOGIN] ?: false }

    suspend fun markNeedsRelogin() {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.LOGGED_IN] == true) prefs[Keys.NEEDS_RELOGIN] = true
        }
    }

    suspend fun save(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN] = true
            prefs[Keys.USERNAME] = session.username
            // Login ulang bisa dengan user lain tanpa clear() — jangan sisakan nilai user lama.
            session.employeeName?.let { prefs[Keys.EMPLOYEE] = it } ?: prefs.remove(Keys.EMPLOYEE)
            session.roleName?.let { prefs[Keys.ROLE] = it } ?: prefs.remove(Keys.ROLE)
            prefs[Keys.PERMISSIONS] = session.permissions
            prefs[Keys.IS_SUPER_ADMIN] = session.isSuperAdmin
            prefs[Keys.NEEDS_RELOGIN] = false
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
