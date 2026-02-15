package com.hcwebhook.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles scheduled sync alarms and device boot.
 * Triggers syncs when alarms fire and reschedules them after boot or app updates.
 */
class ScheduledSyncReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ScheduledSyncReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            ScheduledSyncManager.ACTION_SCHEDULED_SYNC -> {
                // Trigger sync asynchronously
                val scheduleId = intent.getStringExtra(ScheduledSyncManager.EXTRA_SCHEDULE_ID)
                Log.d(TAG, "Triggering scheduled sync for schedule: $scheduleId")
                val pendingResult = goAsync()
                
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val syncManager = SyncManager(context)
                        syncManager.performSync()
                        
                        // Reschedule the alarm for the next day
                        if (scheduleId != null) {
                            val preferencesManager = PreferencesManager(context)
                            val schedule = preferencesManager.getScheduledSyncs()
                                .find { it.id == scheduleId }
                            
                            if (schedule != null && schedule.enabled) {
                                val scheduledSyncManager = ScheduledSyncManager(context)
                                scheduledSyncManager.scheduleAlarm(schedule)
                                Log.d(TAG, "Rescheduled alarm for next day: $scheduleId")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Sync failed: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Reschedule all alarms after boot or app update
                Log.d(TAG, "Rescheduling alarms after boot/update")
                val scheduledSyncManager = ScheduledSyncManager(context)
                scheduledSyncManager.scheduleAllAlarms()
            }
        }
    }
}
