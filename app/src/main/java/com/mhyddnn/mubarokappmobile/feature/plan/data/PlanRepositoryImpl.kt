package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.google.gson.Gson
import com.mhyddnn.mubarokappmobile.core.common.Page
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.common.map
import com.mhyddnn.mubarokappmobile.core.common.mapItems
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.safeApiCall
import com.mhyddnn.mubarokappmobile.core.sync.OutboxDao
import com.mhyddnn.mubarokappmobile.core.sync.OutboxEntity
import com.mhyddnn.mubarokappmobile.core.sync.SyncScheduler
import com.mhyddnn.mubarokappmobile.core.sync.SyncWorker
import com.mhyddnn.mubarokappmobile.feature.plan.domain.CreatePlanOutcome
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PendingPlanStatus
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanMasterData
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

private const val KIND_PLAN_STATUS = "plan-status"
private const val KIND_PLAN_CREATE = "plan-create"

/**
 * Gagal karena sinyal / server / sesi  bukan karena isi data akan masuk ke outbox.
 * Penolakan isi (400 validasi, 403, 409) tetap ditampilkan di form agar bisa langsung dibetulkan.
 */
internal fun Result.Error.isTransient(): Boolean =
    code == null || SyncWorker.outcomeOf(code) in setOf(SyncWorker.SendOutcome.RETRY, SyncWorker.SendOutcome.AUTH)

/**
 * Sumber data plan untuk semua layar plan.
 *
 * - **Baca** (daftar/detail/master): selalu coba server dulu; sukses -> cache Room per user
 *   (`plan_cache`, `plan_master_cache`). Gagal -> ViewModel memakai `cached*()` / master tersimpan.
 * - **Ubah status**: SELALU lewat outbox, juga saat online — satu jalur, sehingga UI cukup
 *   membaca [pendingStatuses] untuk menampilkan "Belum terkirim".
 * - **Buat plan**: online dulu, gagal sementara → outbox (lihat [createPlan]).
 *
 * Cache menyimpan JSON [PlanDto] apa adanya (DTO di paket `data` dijaga R8 dari rename, jadi
 * JSON lama tetap terbaca setelah update app). Semua data dibatasi `owner` = username aktif.
 */
class PlanRepositoryImpl @Inject constructor(
    private val api: PlanApi,
    private val planDao: PlanDao,
    private val outboxDao: OutboxDao,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler,
) : PlanRepository {

    private val gson = Gson()

    /** Username aktif; null = belum login (cache tidak ditulis/dibaca). */
    private suspend fun owner(): String? = sessionManager.session.first()?.username

    private fun PlanEntity.toDomain(): Plan = gson.fromJson(json, PlanDto::class.java).toDomain()

    override suspend fun getPlans(page: Int, limit: Int): Result<Page<Plan>> {
        val result = safeApiCall { api.getPlans(page, limit) }.map { it.toPage() }
        val owner = owner()
        if (result is Result.Success && owner != null) {
            // position global (lintas halaman) agar cache offline tampil dengan urutan server.
            val entities = result.data.items.mapIndexed { i, dto ->
                PlanEntity(owner, dto.id, position = (page - 1) * limit + i, json = gson.toJson(dto))
            }
            // Halaman 1 = snapshot terbaru → ganti total (plan yang hilang di server ikut hilang);
            // halaman berikutnya hanya menambah.
            if (page == 1) planDao.replace(owner, entities) else planDao.upsert(entities)
        }
        return result.map { it.mapItems(PlanDto::toDomain) }
    }

    override suspend fun cachedPlans(): List<Plan> =
        owner()?.let { planDao.all(it).map { e -> e.toDomain() } }.orEmpty()

    override suspend fun cachedPlan(id: Int): Plan? =
        owner()?.let { planDao.get(it, id)?.toDomain() }

    override suspend fun getPlan(id: Int): Result<Plan> {
        val result = safeApiCall { api.getPlan(id) }
        val owner = owner()
        if (result is Result.Success && owner != null) {
            // Hanya perbarui yang sudah ada di cache, agar urutan daftar tidak berubah.
            planDao.get(owner, id)?.let { planDao.upsert(listOf(it.copy(json = gson.toJson(result.data)))) }
        }
        return result.map { it.toDomain() }
    }

    override suspend fun getMasterData(): Result<PlanMasterData> {
        val owner = owner()
        val fresh = fetchMasterData()
        if (fresh is Result.Success) {
            owner?.let { planDao.upsertMaster(PlanMasterEntity(it, gson.toJson(fresh.data))) }
            return Result.Success(fresh.data.toDomain())
        }
        val error = fresh as Result.Error
        val cached = owner?.let { planDao.master(it) }
            ?: return error.copy(message = "${error.message} Buka form ini sekali saat online agar bisa dipakai offline.")
        return Result.Success(gson.fromJson(cached.json, PlanMasterCacheDto::class.java).toDomain(error.message))
    }

    /** Lima endpoint master paralel; semua-atau-tidak: satu gagal → Error (cache tidak ditimpa sebagian). */
    private suspend fun fetchMasterData(): Result<PlanMasterCacheDto> = coroutineScope {
        val results = listOf(
            async { safeApiCall { api.getMarketAreas() } },
            async { safeApiCall { api.getVehicles() } },
            async { safeApiCall { api.getEmployees() } },
            async { safeApiCall { api.getProducts() } },
            async { safeApiCall { api.getKasbonTypes() } },
        ).map { it.await() }
        results.firstOrNull { it is Result.Error }?.let { return@coroutineScope it as Result.Error }
        val (areas, vehicles, employees, products, kasbon) = results.map { (it as Result.Success).data }
        Result.Success(PlanMasterCacheDto(areas, vehicles, employees, products, kasbon))
    }

    override suspend fun createPlan(plan: NewPlan): Result<CreatePlanOutcome> {
        val owner = owner() ?: return Result.Error("Belum login")
        val request = plan.toRequest()
        // Key yang sama dipakai online & outbox: bila server sempat memproses tapi balasannya
        // hilang, kiriman ulang dari outbox mengembalikan plan yang sama, bukan plan kedua.
        val key = UUID.randomUUID().toString()
        val result = safeApiCall { api.createPlan(key, request) }
        if (result !is Result.Error || !result.isTransient()) {
            return result.map { CreatePlanOutcome.Created(it.toDomain()) }
        }
        outboxDao.insert(
            OutboxEntity(id = key, owner = owner, kind = KIND_PLAN_CREATE, path = "plan", body = gson.toJson(request)),
        )
        syncScheduler.schedule()
        return Result.Success(CreatePlanOutcome.Queued)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val pendingPlans: Flow<List<PendingPlan>> = sessionManager.session.flatMapLatest { s ->
        if (s == null) flowOf(emptyList())
        else outboxDao.observe(s.username, KIND_PLAN_CREATE).map { items ->
            val master = planDao.master(s.username)
                ?.let { gson.fromJson(it.json, PlanMasterCacheDto::class.java).toDomain() }
            items.map { it.toPendingPlan(master) }
        }
    }

    /**
     * Antrekan `PATCH plan/{id}/update-status`. Tanpa `Idempotency-Key` di BE untuk endpoint ini —
     * aman karena hanya men-set nilai. Format path dibaca balik oleh `toPendingPlanStatus()`.
     */
    override suspend fun updateStatus(planId: Int, status: String) {
        val owner = checkNotNull(owner()) { "Belum login" }
        outboxDao.insert(
            OutboxEntity(
                id = UUID.randomUUID().toString(),
                owner = owner,
                kind = KIND_PLAN_STATUS,
                path = "plan/$planId/update-status",
                body = gson.toJson(UpdatePlanStatusRequest(status)),
                method = OutboxEntity.METHOD_PATCH,
            ),
        )
        syncScheduler.schedule()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val pendingStatuses: Flow<List<PendingPlanStatus>> = sessionManager.session.flatMapLatest { s ->
        if (s == null) flowOf(emptyList())
        else outboxDao.observe(s.username, KIND_PLAN_STATUS).map { items -> items.map { it.toPendingPlanStatus() } }
    }

    override suspend fun retryPending(id: String) {
        val owner = owner() ?: return
        outboxDao.retry(id, owner)
        syncScheduler.schedule()
    }

    override suspend fun discardPending(id: String) {
        val owner = owner() ?: return
        outboxDao.delete(id, owner)
    }
}
