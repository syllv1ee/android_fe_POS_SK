package com.mhyddnn.mubarokappmobile.feature.plan.data

import com.mhyddnn.mubarokappmobile.feature.plan.domain.PlanRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Digunakan Hilt untuk menyediakan dependency fitur ke seluruh aplikasi. Tidak dipanggil
 * langsung oleh kode lain; Hilt membacanya saat build.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlanModule {

    /** Permintaan [PlanRepository] (mis. di ViewModel plan) dijawab dengan [PlanRepositoryImpl]. */
    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository

    // `@Provides` di companion: class ini abstract, jadi fungsi berisi kode tidak bisa dipanggil Hilt tanpa instance.
    companion object {
        /** [PlanApi] hanya interface; implementasinya dibuat Retrofit. */
        @Provides
        @Singleton
        fun providePlanApi(retrofit: Retrofit): PlanApi = retrofit.create(PlanApi::class.java)
    }
}
