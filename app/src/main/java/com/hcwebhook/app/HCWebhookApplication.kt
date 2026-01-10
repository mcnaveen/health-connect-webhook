package com.hcwebhook.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class HCWebhookApplication : Application() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)

        // Schedule periodic sync work
        scheduleSyncWork()
    }

    fun scheduleSyncWork() {
        val syncIntervalMinutes = preferencesManager.getSyncIntervalMinutes()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = syncIntervalMinutes.toLong(),
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update existing work with new configuration
            syncWorkRequest
        )
    }

    companion object {
        private const val SYNC_WORK_NAME = "health_data_sync"
    }
}