package com.hcwebhook.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class HCWebhookApplication : Application() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)

        // Schedule syncs based on the selected sync mode
        when (preferencesManager.getSyncMode()) {
            SyncMode.INTERVAL -> {
                scheduleSyncWork()
                // Cancel scheduled alarms if they were previously set
                ScheduledSyncManager(this).cancelAllAlarms()
            }
            SyncMode.SCHEDULED -> {
                // Cancel WorkManager periodic sync if it was previously set
                cancelSyncWork()
                // Schedule guaranteed daily alarms (morning + evening)
                ScheduledSyncManager(this).scheduleAllAlarms()
            }
        }
    }

    fun scheduleSyncWork() {
        // Only schedule if sync mode is INTERVAL
        if (preferencesManager.getSyncMode() != SyncMode.INTERVAL) {
            return
        }

        val syncIntervalMinutes = preferencesManager.getSyncIntervalMinutes()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = syncIntervalMinutes.toLong(),
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update existing work with new configuration
            syncWorkRequest
        )
    }

    fun cancelSyncWork() {
        WorkManager.getInstance(this).cancelUniqueWork(SYNC_WORK_NAME)
    }

    companion object {
        private const val SYNC_WORK_NAME = "health_data_sync"
    }
}