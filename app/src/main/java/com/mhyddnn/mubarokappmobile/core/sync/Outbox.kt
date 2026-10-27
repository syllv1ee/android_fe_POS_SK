package com.mhyddnn.mubarokappmobile.core.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Satu request tulis (POST/PATCH) yang menunggu dikirim ke server. Disimpan dulu di HP agar input
 * tetap bisa dilakukan tanpa sinyal; [SyncWorker] mengirimnya saat ada koneksi.
 *
 * Siklus hidup:
 * - [STATUS_PENDING] → dikirim → 2xx → **dihapus** (satu-satunya penghapusan otomatis).
 * - [STATUS_PENDING] → ditolak (400/403/404/409…) → [STATUS_FAILED] + [error]; menunggu user:
 *   [OutboxDao.retry] (kembali PENDING) atau buang ([OutboxDao.delete] by id).
 * - Gangguan sementara (offline, 5xx, 401 yang bisa di-refresh) → tetap PENDING, dicoba lagi.
 *
 * Fitur menulis lewat `outboxDao.insert` + `SyncScheduler.schedule()` dan membaca antreannya
 * sendiri lewat [OutboxDao.observe] dengan [kind] miliknya.
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    /**
     * UUID — juga dikirim sebagai header `Idempotency-Key`. sales-kanvas-be menghormatinya di
     * `POST /plan` & `PATCH /plan/:id` (revisi): kirim ulang dengan key sama → plan yang sama,
     * tidak dobel (endpoint_plan.md). PATCH yang hanya men-set nilai (update-status) memang aman
     * diulang. Endpoint POST LAIN yang masuk outbox wajib dibuat menghormati header ini dulu di BE.
     */
    @PrimaryKey val id: String,
    /** Username pembuat. Hanya dikirim saat pemiliknya yang login. */
    val owner: String,
    /** Jenis data, mis. `"plan-create"` — dipakai fitur untuk menampilkan antreannya sendiri. */
    val kind: String,
    /** Path relatif ke BASE_URL, mis. `plan` atau `plan/12/update-status`. */
    val path: String,
    /** Body JSON siap kirim (hasil `Gson.toJson` DTO request fitur), dikirim apa adanya. */
    val body: String,
    /** Epoch ms saat dibuat — menentukan urutan kirim ([OutboxDao.nextPending]). */
    val createdAt: Long = System.currentTimeMillis(),
    /** [STATUS_PENDING] atau [STATUS_FAILED]. */
    val status: String = STATUS_PENDING,
    /** Pesan penolakan server untuk ditampilkan ke user; null selama PENDING. */
    val error: String? = null,
    /** [METHOD_POST] atau [METHOD_PATCH]. */
    val method: String = METHOD_POST,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_FAILED = "FAILED"
        const val METHOD_POST = "POST"
        const val METHOD_PATCH = "PATCH"
    }
}

/**
 * Akses tabel `outbox`. Query untuk UI selalu disaring `owner` agar antrean user lain di HP yang
 * sama tidak terlihat/terubah.
 */
@Dao
interface OutboxDao {

    /** Id UUID baru per operasi; konflik id = bug pemanggil → exception (ABORT bawaan Room). */
    @Insert
    suspend fun insert(item: OutboxEntity)

    /** Dipakai [SyncWorker] setelah server menerima (2xx). */
    @Delete
    suspend fun delete(item: OutboxEntity)

    /** Buang oleh user (biasanya item FAILED). Tanpa efek bila [id] bukan milik [owner]. */
    @Query("DELETE FROM outbox WHERE id = :id AND owner = :owner")
    suspend fun delete(id: String, owner: String)

    /**
     * Item tertua yang belum terkirim milik [owner] (urutan kirim = urutan input). `rowid` =
     * urutan insert, pemecah seri bila dua item dibuat di milidetik yang sama.
     */
    @Query("SELECT * FROM outbox WHERE owner = :owner AND status = 'PENDING' ORDER BY createdAt, rowid LIMIT 1")
    suspend fun nextPending(owner: String): OutboxEntity?

    /** Dipakai [SyncWorker]; tanpa filter owner karena [id] berasal dari [nextPending] user itu. */
    @Query("UPDATE outbox SET status = 'FAILED', error = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    /** Kembalikan item gagal ke antrean (mis. setelah data master di server diperbaiki). */
    @Query("UPDATE outbox SET status = 'PENDING', error = NULL WHERE id = :id AND owner = :owner")
    suspend fun retry(id: String, owner: String)

    /**
     * Antrean satu fitur ([kind]), PENDING maupun FAILED, terbaru dulu. Flow Room: memancarkan
     * ulang setiap tabel `outbox` berubah (insert, kirim sukses, ditandai gagal).
     */
    @Query("SELECT * FROM outbox WHERE owner = :owner AND kind = :kind ORDER BY createdAt DESC")
    fun observe(owner: String, kind: String): Flow<List<OutboxEntity>>

    /** Semua item milik [owner], termasuk FAILED. */
    @Query("SELECT COUNT(*) FROM outbox WHERE owner = :owner")
    fun observeCount(owner: String): Flow<Int>
}

/**
 * Gagal karena sinyal / server / sesi — bukan karena isi data → boleh masuk outbox. Penolakan isi
 * (400 validasi, 403, 409) tetap ditampilkan di form agar bisa langsung dibetulkan. Aturan kodenya
 * sama dengan [SyncWorker.outcomeOf], jadi form dan worker tidak pernah berbeda pendapat.
 */
internal fun Result.Error.isTransient(): Boolean =
    code == null || SyncWorker.outcomeOf(code) in setOf(SyncWorker.SendOutcome.RETRY, SyncWorker.SendOutcome.AUTH)

/**
 * Jumlah data user aktif yang belum diterima server (untuk badge/banner lintas fitur). Ikut
 * berganti saat user login/logout ([flatMapLatest] atas sesi); belum login → 0.
 */
class OutboxStatus @Inject constructor(
    private val dao: OutboxDao,
    private val sessionManager: SessionManager,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val unsyncedCount: Flow<Int> = sessionManager.session.flatMapLatest { s ->
        if (s == null) flowOf(0) else dao.observeCount(s.username)
    }
}
