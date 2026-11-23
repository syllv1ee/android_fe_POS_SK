package com.mhyddnn.mubarokappmobile.feature.visit.domain

import com.mhyddnn.mubarokappmobile.core.common.Result

interface VisitRepository {

    suspend fun getVisits(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null,
    ): Result<VisitPage>

    suspend fun getVisit(id: Int): Result<Visit>

    suspend fun getMarketAreas(): Result<List<VisitMarketArea>>

    suspend fun createVisit(visit: NewVisit): Result<Visit>

    suspend fun updateStatus(
        id: Int,
        status: String,
    ): Result<Visit>
}