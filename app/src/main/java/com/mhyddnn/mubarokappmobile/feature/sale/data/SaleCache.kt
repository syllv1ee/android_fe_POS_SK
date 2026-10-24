package com.mhyddnn.mubarokappmobile.feature.sale.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.mhyddnn.mubarokappmobile.feature.sale.domain.Sale

/** Cache daftar penjualan dari server, per user — dipakai saat offline. */
@Entity(tableName = "sale_cache", primaryKeys = ["owner", "id"])
data class SaleEntity(
    val owner: String,
    val id: Int,
    /** Urutan tampil sesuai server (halaman × limit + index). */
    val position: Int,
    val invoice: String,
    val typeTransaction: String,
    val groupSaleId: Int,
    val total: Long,
    val note: String?,
    val invoiceDate: String?,
    val paymentRemaining: Long,
)

@Dao
interface SaleDao {

    @Query("SELECT * FROM sale_cache WHERE owner = :owner ORDER BY position")
    suspend fun all(owner: String): List<SaleEntity>

    @Upsert
    suspend fun upsert(items: List<SaleEntity>)

    @Query("DELETE FROM sale_cache WHERE owner = :owner")
    suspend fun clear(owner: String)

    /** Halaman 1 sukses = data terbaru → ganti seluruh cache user ini. */
    @Transaction
    suspend fun replace(owner: String, items: List<SaleEntity>) {
        clear(owner)
        upsert(items)
    }
}

fun Sale.toEntity(owner: String, position: Int) = SaleEntity(
    owner, id, position, invoice, typeTransaction, groupSaleId, total, note, invoiceDate, paymentRemaining,
)

fun SaleEntity.toDomain() = Sale(
    id, invoice, typeTransaction, groupSaleId, total, note, invoiceDate, paymentRemaining,
)
