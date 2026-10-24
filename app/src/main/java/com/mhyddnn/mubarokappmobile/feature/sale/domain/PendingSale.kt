package com.mhyddnn.mubarokappmobile.feature.sale.domain

/**
 * Penjualan yang dibuat di HP tapi belum diterima server. [failedMessage] null = masih
 * menunggu sinyal; terisi = ditolak server (perlu dicek user, tidak dikirim ulang otomatis).
 */
data class PendingSale(
    val id: String,
    val invoiceDate: String,
    val total: Long,
    val note: String?,
    val createdAt: Long,
    val failedMessage: String?,
)
