package com.mhyddnn.mubarokappmobile.core.network

import retrofit2.Response
import retrofit2.http.POST

/**
 * Endpoint refresh minimal yang dipakai khusus oleh [TokenAuthenticator].
 * Dipisah dari `AuthApi` fitur agar lapisan `core` tidak bergantung ke `feature`.
 * Cookie `refresh_token` ikut otomatis lewat [SessionCookieJar]; respons tidak diperlukan.
 */
interface TokenRefreshApi {
    @POST("auth/refresh")
    suspend fun refresh(): Response<Unit>
}
