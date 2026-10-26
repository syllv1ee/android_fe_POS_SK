package com.mhyddnn.mubarokappmobile.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * Saat sebuah request menerima `401` (access_token kedaluwarsa), OkHttp memanggil
 * authenticator ini. Ia memanggil `POST /auth/refresh` dengan body `{ refresh_token }`; bila
 * berhasil, pasangan token baru disimpan dan request asli diulang dengan Bearer baru.
 *
 * - `@Synchronized` + cek token: beberapa 401 bersamaan hanya memicu SATU refresh. BE
 *   merotasi refresh token dan me-revoke sesi bila token lama dipakai ulang, jadi request yang
 *   dikirim dengan access token lama cukup diulang dengan token terbaru tanpa refresh lagi.
 * - Refresh ditolak server (401/403) → [onSessionExpired] (tandai needsRelogin; sesi lokal tetap).
 *   Gagal jaringan / 5xx → menyerah tanpa logout, agar sinyal jelek tidak menendang user.
 * - Maksimal satu kali retry per request agar tidak berulang tanpa henti.
 * - Retry tidak melewati interceptor aplikasi lagi, jadi header Bearer dipasang di sini.
 *
 * Thread: dipanggil OkHttp di thread jaringan, jadi `runBlocking` aman (bukan main thread).
 * Lock dipegang selama request refresh berjalan; request lain yang kena 401 menunggu paling lama
 * sebatas timeout client refresh (30 dtk), lalu memakai token hasil refresh tadi.
 */
class TokenAuthenticator(
    private val refreshApi: TokenRefreshApi,
    private val tokens: TokenStore,
    private val onSessionExpired: () -> Unit,
) : Authenticator {

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // sudah pernah di-retry → menyerah
        // 401 di login/refresh = kredensial/token salah, bukan access token kedaluwarsa.
        // 401 di logout = sesi server sudah mati; refresh di sini bisa selesai SETELAH logout
        // menghapus token dan menyimpan pasangan token baru yang masih hidup.
        val path = response.request.url.encodedPath
        if (path.endsWith("auth/login") || path.endsWith("auth/refresh") || path.endsWith("auth/logout")) {
            return null
        }

        // Request lain sudah me-refresh selama request ini berjalan/menunggu lock.
        val current = tokens.accessToken
        if (current != null && current != response.request.bearerToken()) {
            return response.request.withBearer(current)
        }

        // Tanpa refresh token (mis. sesi dari versi app lama berbasis cookie) tidak ada yang bisa
        // diperbarui → minta login ulang. markNeedsRelogin aman bila memang belum login.
        val refreshToken = tokens.refreshToken ?: run {
            onSessionExpired()
            return null
        }

        val refreshed = try {
            runBlocking { refreshApi.refresh(RefreshRequest(refreshToken)) }
        } catch (_: IOException) {
            return null
        }

        val pair = refreshed.body()?.data
        return when {
            refreshed.isSuccessful && pair != null -> {
                tokens.save(pair.accessToken, pair.refreshToken)
                response.request.withBearer(pair.accessToken)
            }
            refreshed.code() == 401 || refreshed.code() == 403 -> {
                onSessionExpired()
                null
            }
            else -> null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
