package com.mhyddnn.mubarokappmobile.feature.auth.data

import com.mhyddnn.mubarokappmobile.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("auth/profile")
    suspend fun profile(): Response<ApiResponse<UserDto>>
}
