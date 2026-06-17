package com.hcwebhook.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that handles scheduled sync alarms and device boot.
 *
 * For ACTION_SCHEDULED_SYNC we immediately delegate to [SyncForegroundService]
 * rather than doing heavy I/O inside goAsync(). Android 14+ kills goAsync()
 * coroutines after ~10 seconds, which is not enough time for Health Connect
 * reads + HTTP webhook delivery. The foreground service has no such limit.
 *
 * Alarm rescheduling for the next day is handled inside [SyncForegroundService]
 * once the sync completes, so we don't need to do it here.
 */
class ScheduledSyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduledSyncReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")

        when (intent.action) {
            ScheduledSyncManager.ACTION_SCHEDULED_SYNC -> {
                val scheduleId = intent.getStringExtra(ScheduledSyncManager.EXTRA_SCHEDULE_ID)
                Log.d(TAG, "Delegating scheduled sync to SyncForegroundService (scheduleId=$scheduleId)")
                SyncForegroundService.start(context, scheduleId)
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Rescheduling after boot/update")
                val prefsManager = PreferencesManager(context)
                when (prefsManager.getSyncMode()) {
                    SyncMode.SCHEDULED -> {
                        ScheduledSyncManager(context).scheduleAllAlarms()
                    }
                    SyncMode.INTERVAL -> {
                        // WorkManager survives reboots on its own, but re-enqueue
                        // to be safe and pick up any interval changes.
                        (context.applicationContext as? HCWebhookApplication)?.scheduleSyncWork()
                    }
                }
            }
        }
    }
}
