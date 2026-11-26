
package com.mhyddnn.mubarokappmobile.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueApi
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueDao
import com.mhyddnn.mubarokappmobile.core.sync.SyncQueueEntity
import com.mhyddnn.mubarokappmobile.feature.plan.data.MasterDataEntity
import com.mhyddnn.mubarokappmobile.feature.plan.data.PlanDao
import com.mhyddnn.mubarokappmobile.feature.plan.data.PlanEntity
import com.mhyddnn.mubarokappmobile.feature.visit.data.VisitDao
import com.mhyddnn.mubarokappmobile.feature.visit.data.VisitEntity
import com.mhyddnn.mubarokappmobile.feature.visit.data.VisitMarketAreaDao
import com.mhyddnn.mubarokappmobile.feature.visit.data.VisitMarketAreaEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Database lokal mubarok.db.
 *
 * Versi 2 menambahkan visit_cache.
 * Versi 3 menambahkan visit_market_area_cache.
 *
 * Migrasi mempertahankan data Plan, Visit, dan antrean sinkronisasi.
 */
@Database(
    entities = [
        SyncQueueEntity::class,
        PlanEntity::class,
        MasterDataEntity::class,
        VisitEntity::class,
        VisitMarketAreaEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun syncQueueDao(): SyncQueueDao

    abstract fun planDao(): PlanDao

    abstract fun visitDao(): VisitDao

    abstract fun visitMarketAreaDao(): VisitMarketAreaDao
}

/**
 * Migrasi database versi 1 ke versi 2.
 * Menambahkan tabel visit_cache.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `visit_cache` (
                `owner` TEXT NOT NULL,
                `localId` INTEGER NOT NULL,
                `serverId` INTEGER,
                `idempotencyKey` TEXT,
                `position` INTEGER NOT NULL,
                `json` TEXT NOT NULL,
                `syncStatus` TEXT NOT NULL,
                PRIMARY KEY(`owner`, `localId`)
            )
            """.trimIndent()
        )
    }
}

/**
 * Migrasi database versi 2 ke versi 3.
 * Menambahkan cache wilayah pasar Visit.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `visit_market_area_cache` (
                `owner` TEXT NOT NULL,
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `code` TEXT,
                `position` INTEGER NOT NULL,
                PRIMARY KEY(`owner`, `id`)
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mubarok.db",
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
            )
            .build()

    @Provides
    fun provideSyncQueueDao(db: AppDatabase): SyncQueueDao =
        db.syncQueueDao()

    @Provides
    fun providePlanDao(db: AppDatabase): PlanDao =
        db.planDao()

    @Provides
    fun provideVisitDao(db: AppDatabase): VisitDao =
        db.visitDao()

    @Provides
    fun provideVisitMarketAreaDao(db: AppDatabase): VisitMarketAreaDao =
        db.visitMarketAreaDao()

    @Provides
    @Singleton
    fun provideSyncQueueApi(retrofit: Retrofit): SyncQueueApi =
        retrofit.create(SyncQueueApi::class.java)
}
