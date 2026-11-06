package com.mhyddnn.mubarokappmobile.core.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mhyddnn.mubarokappmobile.core.common.Result
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

private val gson = Gson()

/**
 * Membungkus pemanggilan Retrofit menjadi [Result] — satu-satunya pintu repository ke API.
 *
 * - 2xx → [Result.Success] berisi `body.data`; `data` null dianggap error (pakai
 *   [safeApiCallUnit] untuk endpoint tanpa payload).
 * - Non-2xx → [Result.Error] dengan `code`, `message` dari envelope (atau [defaultMessage]), dan
 *   `fieldErrors` dari `errors` (kunci = path Zod digabung titik, lihat [fieldErrorsOf]).
 *   401 baru sampai sini setelah [TokenAuthenticator] gagal me-refresh.
 * - `IOException` (tanpa sinyal, timeout) → [Result.Error] tanpa `code`; exception lain juga tanpa
 *   `code`, pesan generik, detail di `cause`.
 * - `CancellationException` TIDAK ditangkap: dilempar ulang agar pembatalan coroutine tetap jalan.
 *
 * Tidak berganti thread: Retrofit `suspend` sudah menjalankan I/O di thread OkHttp.
 */
suspend fun <T : Any> safeApiCall(block: suspend () -> Response<ApiResponse<T>>): Result<T> =
    apiCall(block) { body, code ->
        body?.data?.let { Result.Success(it) } ?: Result.Error("Respons kosong dari server", code)
    }

/** Varian [safeApiCall] untuk endpoint tanpa payload (`ApiResponse<Unit>`); `data` diabaikan. */
suspend fun safeApiCallUnit(block: suspend () -> Response<ApiResponse<Unit>>): Result<Unit> =
    apiCall(block) { _, _ -> Result.Success(Unit) }

/**
 * Kerangka bersama kedua varian: [onSuccess] hanya menerima respons 2xx (body bisa null);
 * error HTTP & exception diseragamkan di sini agar aturan di [safeApiCall] berlaku sama.
 */
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
} catch (e: CancellationException) {
    throw e // pembatalan (layar ditutup / timeout) harus tetap membatalkan pemanggil
} catch (e: IOException) {
    Result.Error("Gagal terhubung ke server. Periksa koneksi Anda.", cause = e)
} catch (e: Exception) {
    // Pesan exception (mis. error parsing Gson) tidak untuk user; detail tetap di `cause`.
    Result.Error("Terjadi kesalahan tak terduga", cause = e)
}

private data class ParsedError(val message: String, val fieldErrors: Map<String, String>?)

/**
 * Baca envelope error (`message` + `errors`) dari body non-2xx. `errorBody()` adalah stream sekali
 * baca — jangan dibaca lagi di tempat lain. Body kosong / bukan JSON (mis. halaman error proxy)
 * → [defaultMessage] tanpa field error.
 */
private fun parseError(response: Response<*>): ParsedError {
    val raw = response.errorBody()?.string().orEmpty()
    val fallback = defaultMessage(response.code())
    if (raw.isBlank()) return ParsedError(fallback, null)
    return try {
        val type = object : TypeToken<ApiResponse<Any>>() {}.type
        val parsed: ApiResponse<Any>? = gson.fromJson(raw, type)
        ParsedError(parsed?.message?.takeIf { it.isNotBlank() } ?: fallback, fieldErrorsOf(parsed?.errors))
    } catch (_: Exception) {
        ParsedError(fallback, null)
    }
}

/** Pesan untuk user bila server tidak mengirim `message`. */
private fun defaultMessage(code: Int): String = when (code) {
    401 -> "Sesi berakhir. Silakan login ulang."
    403 -> "Anda tidak memiliki izin untuk aksi ini."
    404 -> "Data tidak ditemukan."
    409 -> "Data konflik / referensi tidak valid."
    in 500..599 -> "Server sedang bermasalah. Coba lagi nanti."
    else -> "Permintaan gagal (kode $code)."
}
