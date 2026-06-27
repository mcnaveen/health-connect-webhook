package com.hcwebhook.app

import android.app.Application
import com.feedbackjar.sdk.FeedbackJar
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class HCWebhookApplication : Application() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        FeedbackJar.init(this, FEEDBACKJAR_WIDGET_ID)
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

        if (preferencesManager.isLocalTcpEnabled()) {
            LocalHttpServerService.start(this)
        } else {
            LocalHttpServerService.stop(this)
        }
    }

    fun scheduleSyncWork() {
        // Only schedule if sync mode is INTERVAL
        if (preferencesManager.getSyncMode() != SyncMode.INTERVAL) {
            return
        }

        val syncIntervalMinutes = preferencesManager.getSyncIntervalMinutes()

        // No network constraint: let SyncWorker fail gracefully and retry via
        // backoff when there is no connection, rather than deferring the job
        // indefinitely (which is what NetworkType.CONNECTED causes on Samsung
        // devices with aggressive battery management).
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = syncIntervalMinutes.toLong(),
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5,
                TimeUnit.MINUTES
            )
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
        const val FEEDBACKJAR_WIDGET_ID = "cmk94cys90006cwfp1b67iip5"
    }
}