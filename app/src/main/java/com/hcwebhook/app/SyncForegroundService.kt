package com.hcwebhook.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A short-lived foreground service that performs a single background sync.
 *
 * This is used by [ScheduledSyncReceiver] instead of doing heavy I/O inside
 * BroadcastReceiver.goAsync(), which Android kills after ~10 s on API 34+.
 *
 * The service:
 *  1. Promotes itself to foreground with a transient notification.
 *  2. Calls [SyncManager.performSync].
 *  3. Reschedules the next alarm (for SCHEDULED mode).
 *  4. Calls stopSelf() — the notification disappears automatically.
 */
class SyncForegroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel(this)
        val notification = buildNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scheduleId = intent?.getStringExtra(EXTRA_SCHEDULE_ID)
        Log.d(TAG, "Starting foreground sync (scheduleId=$scheduleId)")

        scope.launch {
            try {
                val syncManager = SyncManager(this@SyncForegroundService)
                syncManager.performSync(syncType = "auto")

                // Reschedule the daily alarm for the next occurrence
                if (scheduleId != null) {
                    val prefsManager = PreferencesManager(this@SyncForegroundService)
                    val schedule = prefsManager.getScheduledSyncs().find { it.id == scheduleId }
                    if (schedule != null && schedule.enabled) {
                        ScheduledSyncManager(this@SyncForegroundService).scheduleAlarm(schedule)
                        Log.d(TAG, "Rescheduled alarm for next day: $scheduleId")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed in foreground service: ${e.message}", e)
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SyncForegroundService"
        private const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "hc_sync_service"
        const val EXTRA_SCHEDULE_ID = "schedule_id"

        fun start(context: Context, scheduleId: String? = null) {
            val intent = Intent(context, SyncForegroundService::class.java).apply {
                scheduleId?.let { putExtra(EXTRA_SCHEDULE_ID, it) }
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure { e ->
                Log.e("SyncForegroundService", "Failed to start foreground service: ${e.message}", e)
            }
        }

        private fun ensureNotificationChannel(context: Context) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.sync_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.sync_service_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        private fun buildNotification(context: Context): Notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.sync_service_notification_title))
                .setContentText(context.getString(R.string.sync_service_notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
    }
}
