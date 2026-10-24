package com.mhyddnn.mubarokappmobile.feature.sale.domain

import com.mhyddnn.mubarokappmobile.core.common.Page
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.feature.sale.data.CreateSaleRequest
import kotlinx.coroutines.flow.Flow

interface SaleRepository {

    /** Ambil satu halaman daftar penjualan. Sukses → cache lokal ikut diperbarui. */
    suspend fun getSales(page: Int = 1, limit: Int = 10): Result<Page<Sale>>

    /** Daftar penjualan terakhir yang tersimpan di HP (untuk mode offline). */
    suspend fun cachedSales(): List<Sale>

    /**
     * Simpan penjualan baru ke antrean lokal lalu kirim saat ada sinyal. Selalu berhasil
     * secara lokal (tidak butuh internet); hasil kirim terlihat di [pendingSales].
     */
    // ponytail: domain memakai DTO request langsung — buat model domain sendiri bila form
    // perlu bentuk data yang berbeda dari body API.
    suspend fun createSale(request: CreateSaleRequest)

    /** Penjualan yang belum diterima server (menunggu sinyal / ditolak server). */
    val pendingSales: Flow<List<PendingSale>>

    /** Kirim ulang item yang ditolak server. */
    suspend fun retryPending(id: String)

    /** Buang item antrean (mis. ditolak server dan memang salah input). */
    suspend fun discardPending(id: String)
}
