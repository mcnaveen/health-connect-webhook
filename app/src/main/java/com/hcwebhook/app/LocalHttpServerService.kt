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
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LocalHttpServerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                try {
                    val notification = buildNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                serviceScope.launch {
                    LocalHttpServerManager.syncWithPreferences(applicationContext)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Must not schedule stop() on serviceScope then cancel() — the job is cancelled
        // before the socket closes, so the port stays open until the process dies.
        runBlocking(Dispatchers.IO) {
            LocalHttpServerManager.stop()
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.local_http_server_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.local_http_server_notification_channel_desc)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val port = PreferencesManager(this).getLocalTcpPort()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.local_http_server_notification_title))
            .setContentText(getString(R.string.local_http_server_notification_text, port))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "local_http_server_channel"
        private const val NOTIFICATION_ID = 1742
        private const val ACTION_START = "com.hcwebhook.app.action.LOCAL_HTTP_SERVER_START"
        private const val ACTION_STOP = "com.hcwebhook.app.action.LOCAL_HTTP_SERVER_STOP"

        fun start(context: Context) {
            val intent = Intent(context, LocalHttpServerService::class.java).apply {
                action = ACTION_START
            }
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocalHttpServerService::class.java))
        }
    }
}
