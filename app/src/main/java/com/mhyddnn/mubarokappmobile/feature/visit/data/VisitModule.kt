package com.mhyddnn.mubarokappmobile.feature.visit.data

import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VisitModule {

    @Binds
    @Singleton
    abstract fun bindVisitRepository(
        impl: VisitRepositoryImpl,
    ): VisitRepository

    companion object {

        @Provides
        @Singleton
        fun provideVisitApi(
            retrofit: Retrofit,
        ): VisitApi =
            retrofit.create(VisitApi::class.java)
    }
}