package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt: [PlanRepository] → [PlanRepositoryImpl] (singleton) dan Retrofit [PlanApi]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlanModule {

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository

    companion object {
        @Provides
        @Singleton
        fun providePlanApi(retrofit: Retrofit): PlanApi = retrofit.create(PlanApi::class.java)
    }
}
