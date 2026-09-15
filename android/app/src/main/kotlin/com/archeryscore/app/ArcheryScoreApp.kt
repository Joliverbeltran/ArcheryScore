package com.archeryscore.app

import android.app.Application
import com.archeryscore.app.data.sync.SyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ArcheryScoreApp : Application(), androidx.work.Configuration.Provider {

    @Inject lateinit var syncWorkerFactory: SyncWorkerFactory

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(syncWorkerFactory)
            .build()
}