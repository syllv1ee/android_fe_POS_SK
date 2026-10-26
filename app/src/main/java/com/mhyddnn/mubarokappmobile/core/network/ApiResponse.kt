package com.mhyddnn.mubarokappmobile.core.network

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Envelope response seragam backend NestJS:
 * `{ success, message, data, statusCode, meta?, errors? }`.
 *
 * Nama field sengaja sama dengan key JSON agar Gson memetakannya langsung.
 *
 * @param T tipe payload `data` (pakai `Unit` bila endpoint tidak mengembalikan data).
 */
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val statusCode: Int? = null,
    /** Hanya untuk endpoint list. */
    val meta: Meta? = null,
    /**
     * Diisi pada error validasi (400). sales-kanvas-be mengirim array issue Zod
     * `[{ path: ["field"], message }]`; dibaca mentah lalu diubah lewat [fieldErrorsOf].
     */
    val errors: JsonElement? = null,
)

/**
 * `errors` envelope → map `field -> pesan` (pesan pertama per field). Mendukung array issue Zod
 * (path bertingkat digabung titik, mis. `salesKanvasPlanDetail.0.qty`) dan objek `{ field: pesan }`.
 * Issue tanpa path (mis. key asing di root) diabaikan — sudah terwakili `message`.
 */
fun fieldErrorsOf(errors: JsonElement?): Map<String, String>? {
    val map = when {
        errors == null || errors.isJsonNull -> return null
        errors.isJsonArray -> errors.asJsonArray.mapNotNull { issue ->
            val obj = issue as? JsonObject ?: return@mapNotNull null
            val path = (obj.get("path") as? JsonArray)?.joinToString(".") { it.asString }
            val message = obj.get("message")?.takeIf { it.isJsonPrimitive }?.asString
            if (path.isNullOrEmpty() || message == null) null else path to message
        }.reversed().toMap() // reversed + toMap → pesan PERTAMA per field yang menang
        errors.isJsonObject -> errors.asJsonObject.entrySet().associate { (key, value) ->
            key to if (value.isJsonPrimitive) value.asString else value.toString()
        }
        else -> return null
    }
    return map.ifEmpty { null }
}

/** Metadata pagination (`meta`) untuk endpoint list. */
data class Meta(
    val page: Int = 1,
    val limit: Int = 10,
    val total: Int = 0,
    val totalPages: Int = 0,
)
