package com.mhyddnn.mubarokappmobile.core.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mhyddnn.mubarokappmobile.core.common.Page
import com.mhyddnn.mubarokappmobile.core.common.Result
import retrofit2.Response
import java.io.IOException

private val gson = Gson()

/**
 * Membungkus pemanggilan Retrofit menjadi [Result]:
 * - sukses (2xx) → [Result.Success] dengan `body.data`.
 * - error HTTP → [Result.Error] dengan `message`, `code`, dan `fieldErrors` (bila 400).
 * - kegagalan jaringan/parsing → [Result.Error] tanpa `code`.
 *
 * `data` null pada respons sukses dianggap error. Untuk endpoint tanpa payload pakai
 * [safeApiCallUnit].
 */
suspend fun <T : Any> safeApiCall(block: suspend () -> Response<ApiResponse<T>>): Result<T> =
    apiCall(block) { body, code ->
        body?.data?.let { Result.Success(it) } ?: Result.Error("Respons kosong dari server", code)
    }

/** Varian [safeApiCall] untuk endpoint tanpa payload (`ApiResponse<Unit>`); `data` diabaikan. */
suspend fun safeApiCallUnit(block: suspend () -> Response<ApiResponse<Unit>>): Result<Unit> =
    apiCall(block) { _, _ -> Result.Success(Unit) }

/**
 * Varian [safeApiCall] untuk endpoint list ber-pagination: mengembalikan [Page] berisi item
 * + metadata dari `meta`. Bila `meta` tak ada, di-fallback ke satu halaman.
 */
suspend fun <T> safeApiCallPaged(
    block: suspend () -> Response<ApiResponse<List<T>>>,
): Result<Page<T>> = apiCall(block) { body, code ->
    if (body == null) return@apiCall Result.Error("Respons kosong dari server", code)
    val items = body.data ?: emptyList()
    val meta = body.meta
    Result.Success(
        Page(
            items = items,
            page = meta?.page ?: 1,
            limit = meta?.limit ?: items.size,
            total = meta?.total ?: items.size,
            totalPages = meta?.totalPages ?: 1,
        ),
    )
}

/** Kerangka bersama: [onSuccess] hanya menangani respons 2xx; error HTTP & exception di sini. */
private suspend fun <T, R> apiCall(
    block: suspend () -> Response<ApiResponse<T>>,
    onSuccess: (body: ApiResponse<T>?, code: Int) -> Result<R>,
): Result<R> = try {
    val response = block()
    if (response.isSuccessful) {
        onSuccess(response.body(), response.code())
    } else {
        val error = parseError(response)
        Result.Error(error.message, response.code(), error.fieldErrors)
    }
} catch (e: IOException) {
    Result.Error("Gagal terhubung ke server. Periksa koneksi Anda.", cause = e)
} catch (e: Exception) {
    Result.Error(e.message ?: "Terjadi kesalahan tak terduga", cause = e)
}

private data class ParsedError(val message: String, val fieldErrors: Map<String, String>?)

private fun parseError(response: Response<*>): ParsedError {
    val raw = response.errorBody()?.string().orEmpty()
    val fallback = defaultMessage(response.code())
    if (raw.isBlank()) return ParsedError(fallback, null)
    return try {
        val type = object : TypeToken<ApiResponse<Any>>() {}.type
        val parsed: ApiResponse<Any>? = gson.fromJson(raw, type)
        ParsedError(parsed?.message?.takeIf { it.isNotBlank() } ?: fallback, parsed?.errors)
    } catch (_: Exception) {
        ParsedError(fallback, null)
    }
}

private fun defaultMessage(code: Int): String = when (code) {
    401 -> "Sesi berakhir. Silakan login ulang."
    403 -> "Anda tidak memiliki izin untuk aksi ini."
    404 -> "Data tidak ditemukan."
    409 -> "Data konflik / referensi tidak valid."
    in 500..599 -> "Server sedang bermasalah. Coba lagi nanti."
    else -> "Permintaan gagal (kode $code)."
}
