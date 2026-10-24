package com.mhyddnn.mubarokappmobile.core.common

/**
 * Satu halaman hasil dari endpoint list ber-pagination (diisi dari `meta` envelope backend).
 * [hasNext] dipakai UI untuk memutuskan memuat halaman berikutnya.
 */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
) {
    val hasNext: Boolean get() = page < totalPages
}

/** Petakan item halaman tanpa kehilangan metadata pagination. */
inline fun <T, R> Page<T>.mapItems(transform: (T) -> R): Page<R> =
    Page(items.map(transform), page, limit, total, totalPages)
