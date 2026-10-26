package com.mhyddnn.mubarokappmobile.core.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Menempelkan `Authorization: Bearer <access_token>` ke tiap request bila token ada.
 *
 * - Request yang sudah membawa header `Authorization` dibiarkan (header eksplisit menang).
 * - Belum login → request dikirim tanpa header; endpoint terlindungi membalas 401 dan
 *   [TokenAuthenticator] yang memutuskan (refresh / minta login ulang).
 * - Jalan di thread OkHttp: membaca [TokenStore] secara sinkron di sini aman.
 */
class AuthInterceptor(private val tokens: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokens.accessToken
        if (token == null || request.header(AUTHORIZATION) != null) return chain.proceed(request)
        return chain.proceed(request.withBearer(token))
    }
}

/** Nama header token; juga disensor dari log (`NetworkModule`). */
internal const val AUTHORIZATION = "Authorization"

/** Salinan request dengan Bearer [token] (mengganti header lama bila ada). */
internal fun Request.withBearer(token: String): Request =
    newBuilder().header(AUTHORIZATION, "Bearer $token").build()

/**
 * Access token yang dipakai request ini, null bila tanpa header. Dipakai [TokenAuthenticator]
 * untuk tahu apakah request dikirim dengan token lama (sudah di-refresh request lain).
 */
internal fun Request.bearerToken(): String? = header(AUTHORIZATION)?.removePrefix("Bearer ")
