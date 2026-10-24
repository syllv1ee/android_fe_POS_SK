package com.mhyddnn.mubarokappmobile.core.network

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
    /** Diisi pada error validasi (400): map `field -> pesan` (satu pesan per field). */
    val errors: Map<String, String>? = null,
)

/** Metadata pagination (`meta`) untuk endpoint list. */
data class Meta(
    val page: Int = 1,
    val limit: Int = 10,
    val total: Int = 0,
    val totalPages: Int = 0,
)
