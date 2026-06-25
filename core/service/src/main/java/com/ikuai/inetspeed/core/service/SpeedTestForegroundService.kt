package com.ikuai.inetspeed.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.ikuai.inetspeed.core.service.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * 前台服务 - 长时间测速任务
 * 专家模式长时测试、后台继续测试时使用
 */
@AndroidEntryPoint
class SpeedTestForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "speed_test_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.ikuai.inetspeed.STOP_TEST"

        fun start(context: Context, serverName: String, protocol: String) {
            val intent = Intent(context, SpeedTestForegroundService::class.java).apply {
                putExtra("server_name", serverName)
                putExtra("protocol", protocol)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpeedTestForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val serverName = intent?.getStringExtra("server_name") ?: "未知服务器"
        val protocol = intent?.getStringExtra("protocol") ?: "TCP"

        val notification = buildNotification(serverName, protocol)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "测速任务",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "正在执行网络测速"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(serverName: String, protocol: String): Notification {
        val stopIntent = Intent(this, SpeedTestForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("INetSpeed 测速中")
            .setContentText("$protocol · $serverName")
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(
                Notification.Action.Builder(
                    null, "停止",
                    PendingIntent.getService(
                        this, 1, stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).build()
            )
            .setOngoing(true)
            .build()
    }
}
