package com.mhyddnn.mubarokappmobile.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoint refresh minimal yang dipakai khusus oleh [TokenAuthenticator].
 * Dipisah dari `AuthApi` fitur agar lapisan `core` tidak bergantung ke `feature`.
 * BE merotasi refresh token: token baru di respons WAJIB disimpan, yang lama tak berlaku lagi.
 */
interface TokenRefreshApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<ApiResponse<TokenPair>>
}

/** Body `POST /auth/refresh`. */
data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String,
)

/** `data` dari `POST /auth/refresh` (juga bagian dari `data` login). */
data class TokenPair(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
)
