package com.mhyddnn.mubarokappmobile.feature.visit.data

import com.mhyddnn.mubarokappmobile.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VisitApi {

    @GET("visit")
    suspend fun getVisits(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String? = null,
    ): Response<ApiResponse<VisitListDataDto>>

    @GET("visit/{id}")
    suspend fun getVisit(
        @Path("id") id: Int,
    ): Response<ApiResponse<VisitDto>>

    @GET("market-areas")
    suspend fun getMarketAreas(): Response<ApiResponse<List<VisitMarketAreaDto>>>

    @POST("visit")
    suspend fun createVisit(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateVisitRequest,
    ): Response<ApiResponse<VisitDto>>

    @PATCH("visit/{id}/update-status")
    suspend fun updateStatus(
        @Path("id") id: Int,
        @Body body: UpdateVisitStatusRequest,
    ): Response<ApiResponse<VisitDto>>
}