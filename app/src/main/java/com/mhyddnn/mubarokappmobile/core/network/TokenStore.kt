package com.mhyddnn.mubarokappmobile.core.network

import android.content.Context

/**
 * Penyimpanan pasangan token dari `POST /auth/login` & `/auth/refresh` (sales-kanvas-be
 * mengirim token di body JSON, bukan cookie). Interface agar [TokenAuthenticator] bisa diuji
 * di JVM tanpa Android.
 *
 * Kontrak implementasi:
 * - Thread-safe: dibaca/ditulis dari thread OkHttp yang berbeda-beda secara bersamaan.
 * - [save] baru kembali setelah tersimpan permanen — refresh token dirotasi BE, jadi token lama
 *   sudah mati begitu respons refresh diterima.
 * - [save] selalu menyimpan keduanya sekaligus; tidak ada keadaan setengah tersimpan.
 */
interface TokenStore {
    val accessToken: String?
    val refreshToken: String?
    fun save(accessToken: String, refreshToken: String)
    fun clear()
}

/**
 * [TokenStore] di SharedPreferences privat app (sandbox per aplikasi).
 *
 * ponytail: tidak dienkripsi — `EncryptedSharedPreferences` (security-crypto) sudah deprecated.
 * Bila perlu, enkripsi nilai dengan kunci Android Keystore di sini; pemanggil tidak berubah.
 */
class PrefsTokenStore(context: Context) : TokenStore {

    private val prefs =
        context.applicationContext.getSharedPreferences("session_tokens", Context.MODE_PRIVATE)

    override val accessToken: String?
        @Synchronized get() = prefs.getString(KEY_ACCESS, null)

    override val refreshToken: String?
        @Synchronized get() = prefs.getString(KEY_REFRESH, null)

    // commit (sinkron), bukan apply: refresh token dirotasi BE — token lama langsung tidak berlaku,
    // jadi yang baru harus sudah tersimpan sebelum request berikutnya / app dimatikan.
    @Synchronized
    override fun save(accessToken: String, refreshToken: String) {
        prefs.edit().putString(KEY_ACCESS, accessToken).putString(KEY_REFRESH, refreshToken).commit()
    }

    @Synchronized
    override fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
