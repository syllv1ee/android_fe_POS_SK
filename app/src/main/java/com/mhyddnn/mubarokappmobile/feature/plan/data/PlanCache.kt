package com.mhyddnn.mubarokappmobile.feature.plan.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * Cache plan dari server, per user - dipakai saat offline (daftar + detail).
 * Jika offline data tetap bisa di akses.
 */
@Entity(tableName = "plan_cache", primaryKeys = ["owner", "id"])
data class PlanEntity(
    val owner: String,
    val id: Int,
    /** Urutan tampil sesuai server (halaman × limit + index). */
    val position: Int,
    val json: String,
)

/** Data master form plan terakhir dari server, per user — agar form bisa dibuka offline. */
@Entity(tableName = "master_data_cache")
data class MasterDataEntity(
    @PrimaryKey val owner: String,
    val json: String,
)

@Dao
interface PlanDao {

    @Query("SELECT * FROM plan_cache WHERE owner = :owner ORDER BY position")
    suspend fun all(owner: String): List<PlanEntity>

    @Query("SELECT * FROM plan_cache WHERE owner = :owner AND id = :id")
    suspend fun get(owner: String, id: Int): PlanEntity?

    @Upsert
    suspend fun upsert(items: List<PlanEntity>)

    @Query("DELETE FROM plan_cache WHERE owner = :owner")
    suspend fun clear(owner: String)

    /** Halaman 1 sukses = data terbaru -> ganti seluruh cache user ini. */
    @Transaction
    suspend fun replace(owner: String, items: List<PlanEntity>) {
        clear(owner)
        upsert(items)
    }

    @Query("SELECT * FROM master_data_cache WHERE owner = :owner")
    suspend fun master(owner: String): MasterDataEntity?

    @Upsert
    suspend fun upsertMaster(item: MasterDataEntity)
}
