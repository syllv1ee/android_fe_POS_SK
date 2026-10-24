package com.mhyddnn.mubarokappmobile.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * Saat sebuah request menerima `401` (access_token kedaluwarsa), OkHttp memanggil
 * authenticator ini. Ia mencoba `POST /auth/refresh` (cookie `refresh_token` ikut via
 * [SessionCookieJar]); bila berhasil, request asli diulang dengan cookie baru.
 *
 * - `@Synchronized`: beberapa 401 bersamaan hanya memicu satu refresh. Request yang dikirim
 *   sebelum refresh terakhir sukses cukup diulang (cookie-nya sudah baru) tanpa refresh lagi —
 *   penting bila backend merotasi refresh_token.
 * - Refresh ditolak server (401/403) → [onSessionExpired] (tandai needsRelogin; sesi lokal tetap).
 *   Gagal jaringan / 5xx → menyerah tanpa logout, agar sinyal jelek tidak menendang user.
 * - Maksimal satu kali retry per request agar tidak berulang tanpa henti.
 */
class TokenAuthenticator(
    private val refreshApi: TokenRefreshApi,
    private val onSessionExpired: () -> Unit,
) : Authenticator {

    private var lastRefreshAt = 0L

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // sudah pernah di-retry → menyerah
        // 401 di login = kredensial salah, bukan sesi kedaluwarsa.
        if (response.request.url.encodedPath.endsWith("auth/login")) return null

        val retry = response.request.newBuilder().build()
        // Request lain sudah me-refresh selama request ini berjalan/menunggu lock.
        if (response.sentRequestAtMillis < lastRefreshAt) return retry

        val code = try {
            runBlocking { refreshApi.refresh().code() }
        } catch (_: IOException) {
            return null
        }

        return when {
            code in 200..299 -> {
                lastRefreshAt = System.currentTimeMillis()
                retry
            }
            code == 401 || code == 403 -> {
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
