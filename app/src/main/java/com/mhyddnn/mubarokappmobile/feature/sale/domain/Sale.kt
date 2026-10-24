package com.mhyddnn.mubarokappmobile.feature.sale.domain

/**
 * Model ringkas satu penjualan untuk layar daftar. Uang sebagai Long rupiah; tanggal string ISO
 * (format di UI). [paymentRemaining] = total sisa pembayaran (untuk status lunas/piutang).
 */
data class Sale(
    val id: Int,
    val invoice: String,
    val typeTransaction: String,
    val groupSaleId: Int,
    val total: Long,
    val note: String?,
    val invoiceDate: String?,
    val paymentRemaining: Long,
)
