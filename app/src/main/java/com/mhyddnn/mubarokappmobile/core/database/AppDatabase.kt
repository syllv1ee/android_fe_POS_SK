package com.mhyddnn.mubarokappmobile.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mhyddnn.mubarokappmobile.core.sync.OutboxApi
import com.mhyddnn.mubarokappmobile.core.sync.OutboxDao
import com.mhyddnn.mubarokappmobile.core.sync.OutboxEntity
import com.mhyddnn.mubarokappmobile.feature.sale.data.SaleDao
import com.mhyddnn.mubarokappmobile.feature.sale.data.SaleEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Database lokal: cache baca (mis. daftar penjualan) + outbox (data tulis yang menunggu sinkron).
 * Entity baru → tambahkan ke `entities` dan NAIKKAN `version`.
 */
@Database(entities = [OutboxEntity::class, SaleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun saleDao(): SaleDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        // Sengaja TANPA fallbackToDestructiveMigration: itu menghapus antrean yang belum
        // terkirim. Ubah schema → wajib tulis Migration (app crash saat dibuka bila lupa, jadi ketahuan di dev).
        Room.databaseBuilder(context, AppDatabase::class.java, "mubarok.db").build()

    @Provides
    fun provideOutboxDao(db: AppDatabase): OutboxDao = db.outboxDao()

    @Provides
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

    @Provides
    @Singleton
    fun provideOutboxApi(retrofit: Retrofit): OutboxApi = retrofit.create(OutboxApi::class.java)
}
