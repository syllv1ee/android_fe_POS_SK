package com.mhyddnn.mubarokappmobile.feature.sale.data

import com.mhyddnn.mubarokappmobile.feature.sale.domain.SaleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SaleModule {

    @Binds
    @Singleton
    abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository

    companion object {
        @Provides
        @Singleton
        fun provideSalesKanvasApi(retrofit: Retrofit): SalesKanvasApi =
            retrofit.create(SalesKanvasApi::class.java)
    }
}
