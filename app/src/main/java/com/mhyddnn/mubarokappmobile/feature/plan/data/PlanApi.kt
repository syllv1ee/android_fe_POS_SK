package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.mhyddnn.mubarokappmobile.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Baca butuh `plan:read`, buat `plan:write`. */
interface PlanApi {

    @GET("plan")
    suspend fun getPlans(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): Response<ApiResponse<PlanPageDto>>

    @GET("plan/{id}")
    suspend fun getPlan(@Path("id") id: Int): Response<ApiResponse<PlanDto>>

    /**
     * Dicoba online dulu (error validasi tampil di form); gagal karena sinyal -> antrean sinkron dengan
     * [idempotencyKey] yang sama, jadi kiriman ulang tidak membuat plan dobel.
     */
    @POST("plan")
    suspend fun createPlan(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreatePlanRequest,
    ): Response<ApiResponse<PlanDto>>

    /** Data master untuk form; semua array tanpa paging. */
    @GET("master/market-area")
    suspend fun getMarketAreas(): Response<ApiResponse<List<MasterOptionDto>>>

    @GET("master/vehicle")
    suspend fun getVehicles(): Response<ApiResponse<List<MasterOptionDto>>>

    @GET("master/employee")
    suspend fun getEmployees(): Response<ApiResponse<List<MasterOptionDto>>>

    @GET("master/product")
    suspend fun getProducts(): Response<ApiResponse<List<MasterOptionDto>>>

    @GET("master/kasbon-material-type")
    suspend fun getKasbonTypes(): Response<ApiResponse<List<MasterOptionDto>>>
}
