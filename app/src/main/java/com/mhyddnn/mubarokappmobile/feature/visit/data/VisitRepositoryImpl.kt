package com.mhyddnn.mubarokappmobile.feature.visit.data

import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.common.map
import com.mhyddnn.mubarokappmobile.core.network.safeApiCall
import com.mhyddnn.mubarokappmobile.feature.visit.domain.NewVisit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.Visit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitMarketArea
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitPage
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitRepository
import java.util.UUID
import javax.inject.Inject

class VisitRepositoryImpl @Inject constructor(
    private val api: VisitApi,
) : VisitRepository {

    override suspend fun getVisits(
        page: Int,
        limit: Int,
        search: String?,
    ): Result<VisitPage> =
        safeApiCall {
            api.getVisits(
                page = page,
                limit = limit,
                search = search?.trim()?.takeIf { it.isNotBlank() },
            )
        }.map { it.toDomain() }

    override suspend fun getVisit(id: Int): Result<Visit> =
        safeApiCall {
            api.getVisit(id)
        }.map { it.toDomain() }

    override suspend fun getMarketAreas(): Result<List<VisitMarketArea>> =
        safeApiCall {
            api.getMarketAreas()
        }.map { areas ->
            areas.map { it.toDomain() }
        }

    override suspend fun createVisit(visit: NewVisit): Result<Visit> =
        safeApiCall {
            api.createVisit(
                idempotencyKey = UUID.randomUUID().toString(),
                body = visit.toRequest(),
            )
        }.map { it.toDomain() }

    override suspend fun updateStatus(
        id: Int,
        status: String,
    ): Result<Visit> =
        safeApiCall {
            api.updateStatus(
                id = id,
                body = UpdateVisitStatusRequest(status),
            )
        }.map { it.toDomain() }
}