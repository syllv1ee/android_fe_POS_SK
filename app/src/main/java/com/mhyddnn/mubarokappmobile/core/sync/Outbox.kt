package com.mhyddnn.mubarokappmobile.core.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Satu request tulis (POST) yang menunggu dikirim ke server. Disimpan dulu di HP agar input
 * tetap bisa dilakukan tanpa sinyal; [SyncWorker] mengirimnya saat ada koneksi.
 *
 * Data TIDAK pernah dihapus otomatis kecuali server sudah menerima (2xx). Item yang ditolak
 * server (400/403/409…) jadi [STATUS_FAILED] dan menunggu tindakan user.
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    /** UUID — juga dikirim sebagai header `Idempotency-Key` agar kirim ulang tidak dobel. */
    @PrimaryKey val id: String,
    /** Username pembuat. Hanya dikirim saat pemiliknya yang login. */
    val owner: String,
    /** Jenis data, mis. `"sale"` — dipakai fitur untuk menampilkan antreannya sendiri. */
    val kind: String,
    /** Path relatif ke BASE_URL, mis. `sales-kanvas/sale`. */
    val path: String,
    /** Body JSON siap kirim. */
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
    val error: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_FAILED = "FAILED"
    }
}

@Dao
interface OutboxDao {

    @Insert
    suspend fun insert(item: OutboxEntity)

    @Delete
    suspend fun delete(item: OutboxEntity)

    @Query("DELETE FROM outbox WHERE id = :id AND owner = :owner")
    suspend fun delete(id: String, owner: String)

    /** Item tertua yang belum terkirim milik [owner] (urutan kirim = urutan input). */
    @Query("SELECT * FROM outbox WHERE owner = :owner AND status = 'PENDING' ORDER BY createdAt LIMIT 1")
    suspend fun nextPending(owner: String): OutboxEntity?

    @Query("UPDATE outbox SET status = 'FAILED', error = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    /** Kembalikan item gagal ke antrean (mis. setelah data master di server diperbaiki). */
    @Query("UPDATE outbox SET status = 'PENDING', error = NULL WHERE id = :id AND owner = :owner")
    suspend fun retry(id: String, owner: String)

    @Query("SELECT * FROM outbox WHERE owner = :owner AND kind = :kind ORDER BY createdAt DESC")
    fun observe(owner: String, kind: String): Flow<List<OutboxEntity>>

    @Query("SELECT COUNT(*) FROM outbox WHERE owner = :owner")
    fun observeCount(owner: String): Flow<Int>
}

/** Jumlah data user aktif yang belum diterima server (untuk badge/banner lintas fitur). */
class OutboxStatus @Inject constructor(
    private val dao: OutboxDao,
    private val sessionManager: SessionManager,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val unsyncedCount: Flow<Int> = sessionManager.session.flatMapLatest { s ->
        if (s == null) flowOf(0) else dao.observeCount(s.username)
    }
}
