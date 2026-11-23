
package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.google.gson.Gson
import com.mhyddnn.mubarokappmobile.core.common.Result
import com.mhyddnn.mubarokappmobile.core.common.map
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.safeApiCall
import com.mhyddnn.mubarokappmobile.feature.plan.domain.NewPlan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.Plan
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanMasterData
import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PlanRepositoryImpl @Inject constructor(
    private val api: PlanApi,
    private val planDao: PlanDao,
    private val sessionManager: SessionManager,
) : PlanRepository {

    private val gson = Gson()

    private suspend fun owner(): String? =
        sessionManager.session.first()?.username

    private fun decodePlan(entity: PlanEntity): PlanDto? =
        runCatching {
            gson.fromJson(entity.json, PlanDto::class.java)
        }.getOrNull()

    override suspend fun getPlans(): Result<List<Plan>> {
        val result = safeApiCall { api.getPlans() }

        if (result is Result.Success) {
            val username = owner()

            if (username != null) {
                val entities = result.data.mapIndexed { index, dto ->
                    PlanEntity(
                        owner = username,
                        id = dto.id,
                        position = index,
                        json = gson.toJson(dto),
                    )
                }

                planDao.replace(username, entities)
            }
        }

        return result.map { list -> list.map(PlanDto::toDomain) }
    }

    override suspend fun cachedPlans(): List<Plan> {
        val username = owner() ?: return emptyList()
        val entities = planDao.all(username)

        val decoded = entities.map { entity ->
            decodePlan(entity)
        }

        // Cache lama tidak memiliki plan_no. Hapus hanya cache plan lama,
        // bukan antrean sinkron atau data master.
        if (decoded.any { it == null || it.planNo.isNullOrBlank() }) {
            planDao.clear(username)
            return emptyList()
        }

        return decoded.filterNotNull().map { it.toDomain() }
    }

    override suspend fun cachedPlan(id: Int): Plan? {
        val username = owner() ?: return null
        val entity = planDao.get(username, id) ?: return null
        val dto = decodePlan(entity)

        if (dto == null || dto.planNo.isNullOrBlank()) {
            planDao.clear(username)
            return null
        }

        return dto.toDomain()
    }

    override suspend fun getPlan(id: Int): Result<Plan> {
        val result = safeApiCall { api.getPlan(id) }

        if (result is Result.Success) {
            val username = owner()

            if (username != null) {
                val existing = planDao.get(username, id)

                if (existing != null) {
                    planDao.upsert(
                        listOf(
                            existing.copy(json = gson.toJson(result.data)),
                        ),
                    )
                }
            }
        }

        return result.map { it.toDomain() }
    }

    override suspend fun getMasterData(): Result<PlanMasterData> {
        val username = owner()
        val fresh = fetchMasterData()

        if (fresh is Result.Success) {
            if (username != null) {
                planDao.upsertMaster(
                    MasterDataEntity(
                        owner = username,
                        json = gson.toJson(fresh.data),
                    ),
                )
            }

            return Result.Success(fresh.data.toDomain())
        }

        val error = fresh as Result.Error
        val cached = username?.let { planDao.master(it) }
            ?: return error.copy(
                message = "${error.message} Buka form saat online untuk memuat data pilihan.",
            )

        val cachedDto = runCatching {
            gson.fromJson(cached.json, PlanMasterCacheDto::class.java)
        }.getOrNull() ?: return error

        return Result.Success(cachedDto.toDomain(error.message))
    }

    override suspend fun cachedMasterData(): PlanMasterData? {
        val username = owner() ?: return null
        val cached = planDao.master(username) ?: return null

        return runCatching {
            gson.fromJson(cached.json, PlanMasterCacheDto::class.java).toDomain()
        }.getOrNull()
    }

    private suspend fun fetchMasterData(): Result<PlanMasterCacheDto> =
        coroutineScope {
            val areas = async { safeApiCall { api.getMarketAreas() } }
            val vehicles = async { safeApiCall { api.getVehicles() } }
            val employees = async { safeApiCall { api.getEmployees() } }
            val outlets = async { safeApiCall { api.getOutlets() } }

            val results = listOf(
                areas.await(),
                vehicles.await(),
                employees.await(),
                outlets.await(),
            )

            val error = results.firstOrNull { it is Result.Error }

            if (error is Result.Error) {
                return@coroutineScope error
            }

            val areaData = (results[0] as Result.Success).data
            val vehicleData = (results[1] as Result.Success).data
            val employeeData = (results[2] as Result.Success).data
            val outletData = (results[3] as Result.Success).data

            Result.Success(
                PlanMasterCacheDto(
                    marketAreas = areaData,
                    vehicles = vehicleData,
                    employees = employeeData,
                    outlets = outletData,
                ),
            )
        }

    override suspend fun createPlan(plan: NewPlan): Result<Plan> {
        val request = plan.toRequest()

        return safeApiCall {
            api.createPlan(request)
        }.map { it.toDomain() }
    }

    override suspend fun approvePlan(id: Int): Result<Plan> =
        safeApiCall { api.approvePlan(id) }.map { it.toDomain() }

    override suspend fun cancelPlan(id: Int): Result<Plan> =
        safeApiCall { api.cancelPlan(id) }.map { it.toDomain() }

    override suspend fun completePlan(
        id: Int,
        warehouseId: Int?,
    ): Result<Plan> =
        safeApiCall {
            api.completePlan(
                id = id,
                body = CompletePlanRequest(warehouseId),
            )
        }.map { it.toDomain() }

    override suspend fun cancelLoading(id: Int): Result<Plan> =
        safeApiCall { api.cancelLoading(id) }.map { it.toDomain() }
}
