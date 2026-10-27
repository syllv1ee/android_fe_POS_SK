package com.mhyddnn.mubarokappmobile

import android.app.Application
import androidx.work.ExistingWorkPolicy
import com.mhyddnn.mubarokappmobile.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Entry point Hilt untuk seluruh object graph aplikasi. */
@HiltAndroidApp
class MubarokApp : Application() {

    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        // Sisa antrean dari sesi sebelumnya ikut dikirim begitu ada sinyal. KEEP: work yang sudah
        // menunggu jaringan dipakai ulang.
        syncScheduler.schedule(ExistingWorkPolicy.KEEP)
    }
}
