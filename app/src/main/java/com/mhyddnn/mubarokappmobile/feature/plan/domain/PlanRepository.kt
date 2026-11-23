
package com.mhyddnn.mubarokappmobile.feature.plan.domain

import com.mhyddnn.mubarokappmobile.core.common.Result

interface PlanRepository {

    suspend fun getPlans(): Result<List<Plan>>

    suspend fun cachedPlans(): List<Plan>

    suspend fun cachedPlan(id: Int): Plan?

    suspend fun getPlan(id: Int): Result<Plan>

    suspend fun getMasterData(): Result<PlanMasterData>

    suspend fun cachedMasterData(): PlanMasterData?

    suspend fun createPlan(plan: NewPlan): Result<Plan>

    suspend fun approvePlan(id: Int): Result<Plan>

    suspend fun cancelPlan(id: Int): Result<Plan>

    suspend fun completePlan(
        id: Int,
        warehouseId: Int? = null,
    ): Result<Plan>

    suspend fun cancelLoading(id: Int): Result<Plan>
}
