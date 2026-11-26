
package com.mhyddnn.mubarokappmobile.feature.visit.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * Cache kunjungan per pengguna.
 *
 * localId:
 * - Positif: ID data dari server.
 * - Negatif: ID lokal untuk kunjungan yang dibuat secara offline.
 *
 * serverId:
 * - Null: belum berhasil dikirim ke server.
 * - Berisi ID server: sudah berhasil disinkronkan.
 */
@Entity(
    tableName = "visit_cache",
    primaryKeys = ["owner", "localId"],
)
data class VisitEntity(
    val owner: String,
    val localId: Int,
    val serverId: Int?,
    val idempotencyKey: String?,
    val position: Int,
    val json: String,
    val syncStatus: String,
)

@Dao
interface VisitDao {

    @Query(
        """
        SELECT * FROM visit_cache
        WHERE owner = :owner
        ORDER BY position ASC
        """
    )
    suspend fun getAll(owner: String): List<VisitEntity>

    @Query(
        """
        SELECT * FROM visit_cache
        WHERE owner = :owner AND localId = :localId
        LIMIT 1
        """
    )
    suspend fun getByLocalId(
        owner: String,
        localId: Int,
    ): VisitEntity?

    @Query(
        """
        SELECT * FROM visit_cache
        WHERE owner = :owner AND serverId = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(
        owner: String,
        serverId: Int,
    ): VisitEntity?

    @Query(
        """
        SELECT * FROM visit_cache
        WHERE owner = :owner AND idempotencyKey = :key
        LIMIT 1
        """
    )
    suspend fun getByIdempotencyKey(
        owner: String,
        key: String,
    ): VisitEntity?

    @Upsert
    suspend fun upsert(item: VisitEntity)

    @Upsert
    suspend fun upsertAll(items: List<VisitEntity>)

    @Query(
        """
        DELETE FROM visit_cache
        WHERE owner = :owner AND localId = :localId
        """
    )
    suspend fun delete(
        owner: String,
        localId: Int,
    )

    @Query("DELETE FROM visit_cache WHERE owner = :owner")
    suspend fun clear(owner: String)

    /**
     * Memperbarui data lokal setelah server menerima kunjungan.
     * localId tidak berubah agar halaman detail tetap bisa membukanya.
     */
    @Query(
        """
        UPDATE visit_cache
        SET serverId = :serverId,
            json = :json,
            syncStatus = :syncStatus
        WHERE owner = :owner AND localId = :localId
        """
    )
    suspend fun markAsSynced(
        owner: String,
        localId: Int,
        serverId: Int,
        json: String,
        syncStatus: String = "SYNCED",
    ): Int

    /**
     * Mengganti cache hasil unduhan server.
     * Data offline yang localId-nya negatif tetap dipertahankan.
     */
    @Transaction
    suspend fun replaceSyncedVisits(
        owner: String,
        items: List<VisitEntity>,
    ) {
        deleteSyncedVisits(owner)
        upsertAll(items)
    }

    /**
     * Hanya hapus data server yang localId = serverId.
     *
     * Jangan hapus data lokal yang sudah berhasil disinkronkan:
     * localId negatif dan serverId positif.
     */
    @Query(
        """
        DELETE FROM visit_cache
        WHERE owner = :owner
        AND syncStatus = 'SYNCED'
        AND localId = serverId
        """
    )
    suspend fun deleteSyncedVisits(owner: String)
}

/**
 * Cache wilayah pasar per pengguna.
 * Data ini tetap tersedia ketika perangkat tidak memiliki internet.
 */
@Entity(
    tableName = "visit_market_area_cache",
    primaryKeys = ["owner", "id"],
)
data class VisitMarketAreaEntity(
    val owner: String,
    val id: Int,
    val name: String,
    val code: String?,
    val position: Int,
)

@Dao
interface VisitMarketAreaDao {

    @Query(
        """
        SELECT * FROM visit_market_area_cache
        WHERE owner = :owner
        ORDER BY position ASC
        """
    )
    suspend fun getAll(owner: String): List<VisitMarketAreaEntity>

    @Upsert
    suspend fun upsertAll(items: List<VisitMarketAreaEntity>)

    @Query(
        """
        DELETE FROM visit_market_area_cache
        WHERE owner = :owner
        """
    )
    suspend fun clear(owner: String)

    @Transaction
    suspend fun replaceAll(
        owner: String,
        items: List<VisitMarketAreaEntity>,
    ) {
        clear(owner)
        upsertAll(items)
    }
}
