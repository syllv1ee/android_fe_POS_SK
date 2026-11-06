package com.mhyddnn.mubarokappmobile.core.sync

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * POST/PATCH generik untuk isi [SyncQueueEntity] — body sudah JSON, respons cukup dibaca kodenya.
 *
 * `@Url` relatif (mis. `plan`) di-resolve terhadap `BASE_URL`, lewat client utama: Bearer,
 * auto-refresh 401, dan override URL server dev tetap berlaku. Respons mentah ([ResponseBody]),
 * bukan [com.mhyddnn.mubarokappmobile.core.network.ApiResponse], karena worker tidak memakai `data`.
 */
interface SyncQueueApi {
    @POST
    suspend fun post(
        @Url path: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @PATCH
    suspend fun patch(
        @Url path: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>
}
