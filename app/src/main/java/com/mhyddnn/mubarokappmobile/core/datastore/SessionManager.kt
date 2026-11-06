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

// File `datastore/session.preferences_pb`. Delegate ini WAJIB satu per nama file per proses —
// dua instance DataStore ke file yang sama → IllegalStateException.
private val Context.dataStore by preferencesDataStore(name = "session")

/** Profil ringkas user yang sedang login (dipakai UI untuk header & gating menu). */
data class UserSession(
    val username: String,
    val employeeName: String?,
    val roleName: String?,
    /** Nama permission dari `user.permissions[].name` saat login, mis. `plan:write`. */
    val permissions: Set<String>,
    /** Super Admin → bypass semua permission (lihat [isSuperAdminRole]). */
    val isSuperAdmin: Boolean = false,
)

/**
 * Deteksi role Super Admin, cermin `PermissionsGuard.isSuperAdmin()`
 */
fun isSuperAdminRole(roleName: String?, roleId: Int?): Boolean {
    val name = roleName?.trim()?.lowercase()
    return name == "super admin" ||
        name == "superadmin" ||
        name == "super-admin" ||
        roleId == 1
}

/**
 * Cek izin di sisi UI (menampilkan/menyembunyikan menu & tombol) , server tetap
 * memeriksa ulang dan membalas 403. Super Admin selalu lolos; selain itu cocok persis, wildcard
 * `modul:*`, atau `*`.
 *
 * Harus sejalan dengan `matchesPermission()` di BE (`permission.guard.ts`). Saat ini BE tidak
 * mengenal `*` tapi mendukung `modul:aksi:*`; samakan bila aturan permission diubah.
 */
fun UserSession.has(permission: String): Boolean =
    isSuperAdmin ||
        permission in permissions ||
        "${permission.substringBefore(':')}:*" in permissions ||
        "*" in permissions

/**
 * Menyimpan status login & profil user di DataStore Preferences. Token TIDAK disimpan di sini —
 * token ada di [com.mhyddnn.mubarokappmobile.core.network.TokenStore] (dibaca sinkron oleh OkHttp,
 * sedangkan DataStore hanya async).
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

    /** Cek Login. */
    val isLoggedIn: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.LOGGED_IN] ?: false }

    /** Profil user aktif; null bila belum login. */
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

    /** true = refresh token ditolak server */
    val needsRelogin: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NEEDS_RELOGIN] ?: false }

    /** Dipanggil `TokenAuthenticator` saat refresh ditolak. Tanpa efek bila belum/sudah logout. */
    suspend fun markNeedsRelogin() {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.LOGGED_IN] == true) prefs[Keys.NEEDS_RELOGIN] = true
        }
    }

    /**
     * Simpan profil setelah login sukses (atomik dalam satu `edit`) dan hapus tanda needsRelogin.
     */
    suspend fun save(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN] = true
            prefs[Keys.USERNAME] = session.username
            // Login ulang bisa dengan user lain.
            session.employeeName?.let { prefs[Keys.EMPLOYEE] = it } ?: prefs.remove(Keys.EMPLOYEE)
            session.roleName?.let { prefs[Keys.ROLE] = it } ?: prefs.remove(Keys.ROLE)
            prefs[Keys.PERMISSIONS] = session.permissions
            prefs[Keys.IS_SUPER_ADMIN] = session.isSuperAdmin
            prefs[Keys.NEEDS_RELOGIN] = false
        }
    }

    /**
     * Logout: hapus seluruh sesi → [isLoggedIn] false → `MainActivity` pindah ke Login. Antrean sinkron &
     * cache di Room TIDAK ikut dihapus (milik username itu, dipakai lagi saat ia login kembali).
     */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
