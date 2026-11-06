package com.mhyddnn.mubarokappmobile.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Mode tema yang dipilih user */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Preferensi aplikasi non-sesi (file `settings`, terpisah dari `session` agar tetap ada walau
 * logout): mode tema dan override URL server (dev). DataStore Preferences — tepat untuk setting
 * kecil; Room dipakai untuk cache & antrean sinkron.
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SERVER_URL = stringPreferencesKey("server_base_url")
    }

    /** Disimpan sebagai nama enum; nilai tak dikenal -> [ThemeMode.SYSTEM]. */
    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    /**
     * Override URL server (DEV) | bila diisi, scheme/host/port-nya menggantikan BASE_URL lewat
     * [com.mhyddnn.mubarokappmobile.core.network.ServerUrlInterceptor] (hanya build debug).
     * `null`/kosong berarti pakai default `BuildConfig.BASE_URL`.
     */
    val serverBaseUrl: Flow<String?> =
        context.settingsDataStore.data.map { it[Keys.SERVER_URL]?.takeIf(String::isNotBlank) }

    /** Kosong/null = hapus override. */
    suspend fun setServerBaseUrl(url: String?) {
        context.settingsDataStore.edit { prefs ->
            val trimmed = url?.trim().orEmpty()
            if (trimmed.isBlank()) prefs.remove(Keys.SERVER_URL) else prefs[Keys.SERVER_URL] = trimmed
        }
    }
}
