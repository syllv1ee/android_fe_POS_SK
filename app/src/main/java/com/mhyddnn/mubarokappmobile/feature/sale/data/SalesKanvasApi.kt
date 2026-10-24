package com.mhyddnn.mubarokappmobile.feature.sale.data

import com.mhyddnn.mubarokappmobile.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SalesKanvasApi {

    /** Daftar penjualan (paginated). Butuh permission `sales-kanvas:read`. */
    @GET("sales-kanvas/sale")
    suspend fun getSales(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): Response<ApiResponse<List<SaleDto>>>
}
