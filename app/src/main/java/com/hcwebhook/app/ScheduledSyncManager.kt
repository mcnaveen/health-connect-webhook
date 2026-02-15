package com.hcwebhook.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Manages AlarmManager exact alarms for guaranteed daily syncs.
 * This works as a safety net alongside WorkManager to ensure syncs happen
 * even when Doze mode or OEM battery optimization suppresses background work.
 */
class ScheduledSyncManager(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferencesManager = PreferencesManager(context)
    
    companion object {
        const val ACTION_SCHEDULED_SYNC = "com.hcwebhook.app.SCHEDULED_SYNC"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }
    
    /**
     * Schedule all alarms based on the list of scheduled syncs
     */
    fun scheduleAllAlarms() {
        if (!preferencesManager.isScheduledSyncEnabled()) {
            return
        }
        
        // Only schedule if sync mode is SCHEDULED
        if (preferencesManager.getSyncMode() != SyncMode.SCHEDULED) {
            return
        }
        
        val schedules = preferencesManager.getScheduledSyncs()
        // Only schedule enabled schedules
        schedules.filter { it.enabled }.forEach { schedule ->
            scheduleAlarm(schedule)
        }
    }
    
    /**
     * Schedule an alarm for a specific scheduled sync
     */
    fun scheduleAlarm(schedule: ScheduledSync) {
        if (!preferencesManager.isScheduledSyncEnabled()) {
            return
        }
        
        // Only schedule if sync mode is SCHEDULED
        if (preferencesManager.getSyncMode() != SyncMode.SCHEDULED) {
            return
        }
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val intent = Intent(context, ScheduledSyncReceiver::class.java).apply {
            action = ACTION_SCHEDULED_SYNC
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
        }
        val pendingIntent = buildAlarmPendingIntent(intent, schedule.id.hashCode())
        
        // Use exact alarm if permission is granted (Android 12+)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            // Fallback to inexact alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    /**
     * Cancel all scheduled alarms
     */
    fun cancelAllAlarms() {
        val schedules = preferencesManager.getScheduledSyncs()
        schedules.forEach { schedule ->
            cancelAlarm(schedule.id)
        }
    }
    
    /**
     * Cancel a specific alarm by schedule ID
     */
    fun cancelAlarm(scheduleId: String) {
        val intent = Intent(context, ScheduledSyncReceiver::class.java).apply {
            action = ACTION_SCHEDULED_SYNC
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        }
        val pendingIntent = buildAlarmPendingIntent(intent, scheduleId.hashCode())
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Check if the app can schedule exact alarms (required on Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // No permission required on older versions
        }
    }
    
    private fun buildAlarmPendingIntent(intent: Intent, requestCode: Int): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )
    }
}
