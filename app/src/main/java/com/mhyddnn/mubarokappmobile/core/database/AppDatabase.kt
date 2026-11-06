package com.mhyddnn.mubarokappmobile.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueApi
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueDao
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueEntity
import com.mhyddnn.mubarokappmobile.feature.plan.data.PlanDao
import com.mhyddnn.mubarokappmobile.feature.plan.data.PlanEntity
import com.mhyddnn.mubarokappmobile.feature.plan.data.MasterDataEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Database lokal `mubarok.db`: cache baca (daftar plan, master form plan) + antrean sinkron
 * (data tulis yang menunggu sinkron). Semua tabel disaring per `username`.
 *
 * Ubah schema (entity baru / kolom baru) — saat ini v1, belum ada Migration:
 * 1. Tambahkan ke `entities`, NAIKKAN `version`.
 * 2. Tulis `MIGRATION_<lama>_<baru>` dan daftarkan di [DatabaseModule.provideDatabase].
 *    DDL disalin PERSIS dari `createSql` di `app/schemas/.../<baru>.json` (Room memvalidasi schema
 *    saat DB dibuka dan crash bila beda). Kolom NOT NULL baru wajib DEFAULT agar baris antrean sinkron
 *    lama tetap valid.
 * 3. Build → commit `app/schemas/.../<versi>.json` (dihasilkan plugin Room).
 * 4. Buat `MigrationTest` (androidTest, `MigrationTestHelper`) lalu jalankan `connectedDebugAndroidTest`.
 *
 * Cache boleh hilang (diambil ulang dari server), antrean sinkron TIDAK — karena itu tidak ada migrasi
 * destruktif sama sekali.
 */
@Database(
    entities = [SyncQueueEntity::class, PlanEntity::class, MasterDataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao // data offline
    abstract fun planDao(): PlanDao // data dari server
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

    // DAO tanpa @Singleton: Room sudah meng-cache instance DAO di dalam AppDatabase.
    @Provides
    fun provideSyncQueueDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()

    @Provides
    fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()

    /** Di sini (bukan modul terpisah) karena `core/sync` tidak punya modul Hilt sendiri. */
    @Provides
    @Singleton
    fun provideSyncQueueApi(retrofit: Retrofit): SyncQueueApi = retrofit.create(SyncQueueApi::class.java)
}
