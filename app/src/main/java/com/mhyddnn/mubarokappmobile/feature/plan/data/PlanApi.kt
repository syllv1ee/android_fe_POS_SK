
package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.mhyddnn.mubarokappmobile.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PlanApi {

    @GET("sales-kanvas-plans")
    suspend fun getPlans(): Response<ApiResponse<List<PlanDto>>>

    @GET("sales-kanvas-plans/{id}")
    suspend fun getPlan(
        @Path("id") id: Int,
    ): Response<ApiResponse<PlanDto>>

    @POST("sales-kanvas-plans")
    suspend fun createPlan(
        @Body body: CreatePlanRequest,
    ): Response<ApiResponse<PlanDto>>

    @POST("sales-kanvas-plans/{id}/approve")
    suspend fun approvePlan(
        @Path("id") id: Int,
    ): Response<ApiResponse<PlanDto>>

    @POST("sales-kanvas-plans/{id}/cancel")
    suspend fun cancelPlan(
        @Path("id") id: Int,
    ): Response<ApiResponse<PlanDto>>

    @POST("sales-kanvas-plans/{id}/complete")
    suspend fun completePlan(
        @Path("id") id: Int,
        @Body body: CompletePlanRequest,
    ): Response<ApiResponse<PlanDto>>

    @POST("sales-kanvas-plans/{id}/cancel-loading")
    suspend fun cancelLoading(
        @Path("id") id: Int,
    ): Response<ApiResponse<PlanDto>>

    @GET("market-areas")
    suspend fun getMarketAreas(): Response<ApiResponse<List<MasterOptionDto>>>

    @GET("vehicles")
    suspend fun getVehicles(): Response<ApiResponse<List<MasterOptionDto>>>

    @GET("employees")
    suspend fun getEmployees(): Response<ApiResponse<List<MasterOptionDto>>>

    @GET("outlets")
    suspend fun getOutlets(): Response<ApiResponse<List<MasterOptionDto>>>
}
