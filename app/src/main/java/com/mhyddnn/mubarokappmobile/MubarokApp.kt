package com.mhyddnn.mubarokappmobile

import android.app.Application
import com.mhyddnn.mubarokappmobile.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Entry point Hilt untuk seluruh object graph aplikasi. */
@HiltAndroidApp
class MubarokApp : Application() {

    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        // Sisa antrean dari sesi sebelumnya ikut dikirim begitu ada sinyal.
        syncScheduler.schedule()
    }
}
