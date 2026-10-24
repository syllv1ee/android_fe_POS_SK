package com.mhyddnn.mubarokappmobile.feature.sale.data

import com.google.gson.Gson
import com.mhyddnn.mubarokappmobile.core.common.Page
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.common.map
import com.mhyddnn.mubarokappmobile.core.common.mapItems
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.safeApiCallPaged
import com.mhyddnn.mubarokappmobile.core.sync.OutboxDao
import com.mhyddnn.mubarokappmobile.core.sync.OutboxEntity
import com.mhyddnn.mubarokappmobile.core.sync.SyncScheduler
import com.mhyddnn.mubarokappmobile.feature.sale.domain.PendingSale
import com.mhyddnn.mubarokappmobile.feature.sale.domain.Sale
import com.mhyddnn.mubarokappmobile.feature.sale.domain.SaleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

private const val KIND_SALE = "sale"
private const val PATH_SALE = "sales-kanvas/sale"

class SaleRepositoryImpl @Inject constructor(
    private val api: SalesKanvasApi,
    private val saleDao: SaleDao,
    private val outboxDao: OutboxDao,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler,
) : SaleRepository {

    private val gson = Gson()

    private suspend fun owner(): String? = sessionManager.session.first()?.username

    override suspend fun getSales(page: Int, limit: Int): Result<Page<Sale>> {
        val result = safeApiCallPaged { api.getSales(page, limit) }
            .map { it.mapItems { dto -> dto.toDomain() } }
        val owner = owner()
        if (result is Result.Success && owner != null) {
            val entities = result.data.items.mapIndexed { i, sale ->
                sale.toEntity(owner, position = (page - 1) * limit + i)
            }
            if (page == 1) saleDao.replace(owner, entities) else saleDao.upsert(entities)
        }
        return result
    }

    override suspend fun cachedSales(): List<Sale> =
        owner()?.let { saleDao.all(it).map(SaleEntity::toDomain) }.orEmpty()

    override suspend fun createSale(request: CreateSaleRequest) {
        val owner = checkNotNull(owner()) { "Belum login" }
        outboxDao.insert(
            OutboxEntity(
                id = UUID.randomUUID().toString(),
                owner = owner,
                kind = KIND_SALE,
                path = PATH_SALE,
                body = gson.toJson(request),
            ),
        )
        syncScheduler.schedule()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val pendingSales: Flow<List<PendingSale>> = sessionManager.session.flatMapLatest { s ->
        if (s == null) flowOf(emptyList())
        else outboxDao.observe(s.username, KIND_SALE).map { items -> items.map { it.toPendingSale() } }
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

    private fun OutboxEntity.toPendingSale(): PendingSale {
        val req = gson.fromJson(body, CreateSaleRequest::class.java)
        return PendingSale(
            id = id,
            invoiceDate = req.tglInvoice,
            total = req.total,
            note = req.note,
            createdAt = createdAt,
            failedMessage = error.takeIf { status == OutboxEntity.STATUS_FAILED },
        )
    }
}
