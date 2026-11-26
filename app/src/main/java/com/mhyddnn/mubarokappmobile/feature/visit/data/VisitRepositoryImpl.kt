
package com.mhyddnn.mubarokappmobile.feature.visit.data

import com.google.gson.Gson
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.common.map
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.safeApiCall
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueDao
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueEntity
import com.mhyddnn.mubarokappmobile.core.sync.SyncScheduler
import com.mhyddnn.mubarokappmobile.core.sync.isTransient
import com.mhyddnn.mubarokappmobile.feature.visit.domain.NewVisit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.Visit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitMarketArea
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitPage
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class VisitRepositoryImpl @Inject constructor(
    private val api: VisitApi,
    private val visitDao: VisitDao,
    private val marketAreaDao: VisitMarketAreaDao,
    private val syncQueueDao: SyncQueueDao,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler,
) : VisitRepository {

    private val gson = Gson()

    private suspend fun owner(): String? =
        sessionManager.session.first()?.username
            ?.takeIf { it.isNotBlank() }

    private fun decodeVisit(entity: VisitEntity): VisitDto? =
        runCatching {
            gson.fromJson(entity.json, VisitDto::class.java)
        }.getOrNull()

    private fun decodeVisitDomain(entity: VisitEntity): Visit? =
        decodeVisit(entity)?.toDomain()

    private fun nextPosition(items: List<VisitEntity>): Int =
        (items.maxOfOrNull { it.position } ?: -1) + 1

    private suspend fun saveServerVisit(
        username: String,
        dto: VisitDto,
    ) {
        val existing = visitDao.getByServerId(username, dto.id)
        val all = visitDao.getAll(username)

        val localId = existing?.localId ?: dto.id

        visitDao.upsert(
            VisitEntity(
                owner = username,
                localId = localId,
                serverId = dto.id,
                idempotencyKey = existing?.idempotencyKey,
                position = existing?.position ?: nextPosition(all),
                json = gson.toJson(dto),
                syncStatus = "SYNCED",
            ),
        )
    }

    private suspend fun cachedVisits(
        username: String,
        page: Int,
        limit: Int,
        search: String?,
    ): VisitPage {
        val query = search?.trim()?.takeIf { it.isNotBlank() }
            ?.lowercase()

        val all = visitDao.getAll(username)

        val visits = all.mapNotNull { entity ->
            decodeVisitDomain(entity)
        }.filter { visit ->
            query == null ||
                    visit.storeName.lowercase().contains(query) ||
                    visit.visitDate.lowercase().contains(query) ||
                    visit.marketAreaName.orEmpty().lowercase().contains(query)
        }

        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val total = visits.size
        val totalPages = if (total == 0) 0 else (total + safeLimit - 1) / safeLimit
        val start = (safePage - 1) * safeLimit

        return VisitPage(
            items = visits.drop(start).take(safeLimit),
            page = safePage,
            limit = safeLimit,
            total = total,
            totalPages = totalPages,
        )
    }

    override suspend fun getVisits(
        page: Int,
        limit: Int,
        search: String?,
    ): Result<VisitPage> {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val normalizedSearch = search?.trim()?.takeIf { it.isNotBlank() }

        val result = safeApiCall {
            api.getVisits(
                page = safePage,
                limit = safeLimit,
                search = normalizedSearch,
            )
        }

        if (result is Result.Success) {
            val username = owner()

            if (username != null) {
                val entities = result.data.items.mapIndexed { index, dto ->
                    val existing = visitDao.getByServerId(username, dto.id)

                    VisitEntity(
                        owner = username,
                        localId = existing?.localId ?: dto.id,
                        serverId = dto.id,
                        idempotencyKey = existing?.idempotencyKey,
                        position = if (safePage == 1) {
                            index
                        } else {
                            existing?.position ?: (safePage - 1) * safeLimit + index
                        },
                        json = gson.toJson(dto),
                        syncStatus = "SYNCED",
                    )
                }

                if (safePage == 1 && normalizedSearch == null) {
                    visitDao.replaceSyncedVisits(username, entities)
                } else {
                    visitDao.upsertAll(entities)
                }

                // Kunjungan yang belum terkirim tetap ditampilkan bersama data server.
                val cached = visitDao.getAll(username)
                val localVisits = cached.mapNotNull { entity ->
                    if (entity.localId < 0 && entity.serverId == null) {
                        decodeVisitDomain(entity)
                    } else {
                        null
                    }
                }.filter { visit ->
                    normalizedSearch == null ||
                            visit.storeName.contains(normalizedSearch, ignoreCase = true) ||
                            visit.visitDate.contains(normalizedSearch, ignoreCase = true) ||
                            visit.marketAreaName.orEmpty()
                                .contains(normalizedSearch, ignoreCase = true)
                }

                val serverVisits = result.data.toDomain()
                val combined = (serverVisits.items + localVisits)
                    .distinctBy { it.id }

                return Result.Success(
                    serverVisits.copy(
                        items = combined,
                        total = serverVisits.total + localVisits.size,
                    ),
                )
            }

            return result.map { it.toDomain() }
        }

        val error = result as Result.Error
        val username = owner() ?: return error

        val cached = visitDao.getAll(username)
        if (cached.isEmpty()) return error

        return Result.Success(
            cachedVisits(
                username = username,
                page = safePage,
                limit = safeLimit,
                search = normalizedSearch,
            ),
        )
    }

    override suspend fun getVisit(id: Int): Result<Visit> {
        val username = owner()

        val cachedEntity = if (username != null) {
            if (id < 0) {
                visitDao.getByLocalId(username, id)
            } else {
                visitDao.getByServerId(username, id)
                    ?: visitDao.getByLocalId(username, id)
            }
        } else {
            null
        }

        // Kunjungan lokal yang belum memiliki ID server tidak perlu diminta ke server.
        if (cachedEntity != null && cachedEntity.serverId == null) {
            return decodeVisitDomain(cachedEntity)?.let {
                Result.Success(it)
            } ?: Result.Error("Data kunjungan lokal tidak dapat dibaca.")
        }

        val serverId = if (id < 0) {
            cachedEntity?.serverId
        } else {
            id
        }

        if (serverId == null) {
            return Result.Error("Data kunjungan tidak ditemukan.")
        }

        val result = safeApiCall {
            api.getVisit(serverId)
        }

        if (result is Result.Success) {
            if (username != null) {
                saveServerVisit(username, result.data)
            }
            return result.map { it.toDomain() }
        }

        val error = result as Result.Error
        val fallback = cachedEntity
            ?: if (username != null) {
                visitDao.getByServerId(username, serverId)
            } else {
                null
            }

        val cachedVisit = fallback?.let { decodeVisitDomain(it) }

        return if (cachedVisit != null) {
            Result.Success(cachedVisit)
        } else {
            error
        }
    }

    override suspend fun getMarketAreas(): Result<List<VisitMarketArea>> {
        val username = owner()
        val result = safeApiCall {
            api.getMarketAreas()
        }

        if (result is Result.Success) {
            if (username != null) {
                marketAreaDao.replaceAll(
                    owner = username,
                    items = result.data.mapIndexed { index, dto ->
                        VisitMarketAreaEntity(
                            owner = username,
                            id = dto.id,
                            name = dto.name,
                            code = dto.code,
                            position = index,
                        )
                    },
                )
            }

            return result.map { areas ->
                areas.map { it.toDomain() }
            }
        }

        val error = result as Result.Error
        val cached = username?.let {
            marketAreaDao.getAll(it)
        }.orEmpty()

        if (cached.isEmpty()) return error

        return Result.Success(
            cached.map { item ->
                VisitMarketArea(
                    id = item.id,
                    name = item.name,
                    code = item.code,
                )
            },
        )
    }

    override suspend fun createVisit(visit: NewVisit): Result<Visit> {
        val idempotencyKey = UUID.randomUUID().toString()
        val request = visit.toRequest()
        val username = owner()

        val result = safeApiCall {
            api.createVisit(
                idempotencyKey = idempotencyKey,
                body = request,
            )
        }

        if (result is Result.Success) {
            val dto = result.data

            if (username != null) {
                saveServerVisit(username, dto)
            }

            return result.map { it.toDomain() }
        }

        val error = result as Result.Error

        // Kesalahan validasi atau izin tidak boleh dimasukkan ke antrean.
        if (!error.isTransient()) {
            return error
        }

        if (username == null) {
            return error.copy(
                message = "${error.message} Login diperlukan untuk menyimpan kunjungan offline.",
            )
        }

        val existing = visitDao.getByIdempotencyKey(
            owner = username,
            key = idempotencyKey,
        )

        val all = visitDao.getAll(username)
        val localId = existing?.localId
            ?: ((all.minOfOrNull { it.localId } ?: 0) - 1)

        val area = marketAreaDao.getAll(username)
            .firstOrNull { it.id == visit.marketAreaId }

        val localDto = VisitDto(
            id = localId,
            storeName = visit.storeName,
            visitDate = visit.visitDate,
            marketAreaId = visit.marketAreaId,
            marketArea = area?.let {
                VisitMarketAreaDto(
                    id = it.id,
                    name = it.name,
                    code = it.code,
                )
            },
            note = visit.note,
            status = "DIRENCANAKAN",
        )

        visitDao.upsert(
            VisitEntity(
                owner = username,
                localId = localId,
                serverId = null,
                idempotencyKey = idempotencyKey,
                position = nextPosition(all),
                json = gson.toJson(localDto),
                syncStatus = "PENDING",
            ),
        )

        syncQueueDao.insert(
            SyncQueueEntity(
                id = idempotencyKey,
                owner = username,
                kind = "visit-create",
                path = "visit",
                body = gson.toJson(request),
                method = SyncQueueEntity.METHOD_POST,
            ),
        )

        syncScheduler.schedule()

        return Result.Success(localDto.toDomain())
    }

    override suspend fun updateStatus(
        id: Int,
        status: String,
    ): Result<Visit> {
        val username = owner()

        val cachedEntity = if (username != null) {
            if (id < 0) {
                visitDao.getByLocalId(username, id)
            } else {
                visitDao.getByServerId(username, id)
            }
        } else {
            null
        }

        if (id < 0 && cachedEntity?.serverId == null) {
            return Result.Error(
                "Kunjungan masih menunggu dikirim ke server. Status belum dapat diubah.",
            )
        }

        val serverId = if (id < 0) {
            cachedEntity?.serverId
        } else {
            id
        } ?: return Result.Error("ID kunjungan server tidak ditemukan.")

        val result = safeApiCall {
            api.updateStatus(
                id = serverId,
                body = UpdateVisitStatusRequest(status),
            )
        }

        if (result is Result.Success) {
            val dto = result.data

            if (username != null) {
                saveServerVisit(username, dto)
            }

            return result.map { it.toDomain() }
        }

        val error = result as Result.Error

        if (!error.isTransient()) {
            return error
        }

        if (username == null || cachedEntity == null) {
            return error
        }

        val oldDto = decodeVisit(cachedEntity)
            ?: return error

        val updatedDto = oldDto.copy(status = status)
        val idempotencyKey = UUID.randomUUID().toString()

        visitDao.upsert(
            cachedEntity.copy(
                json = gson.toJson(updatedDto),
                syncStatus = "PENDING",
            ),
        )

        syncQueueDao.insert(
            SyncQueueEntity(
                id = idempotencyKey,
                owner = username,
                kind = "visit-status",
                path = "visit/$serverId/update-status",
                body = gson.toJson(UpdateVisitStatusRequest(status)),
                method = SyncQueueEntity.METHOD_PATCH,
            ),
        )

        syncScheduler.schedule()

        return Result.Success(updatedDto.toDomain())
    }
}
