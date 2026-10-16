package io.github.miuzarte.scrcpyforandroid.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.miuzarte.scrcpyforandroid.MainActivity
import io.github.miuzarte.scrcpyforandroid.scrcpy.GamepadInputHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class GamepadService: Service() {

    companion object {
        private const val CHANNEL_ID = "gamepad_service"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "io.github.miuzarte.scrcpyforandroid.STOP_GAMEPAD"

        fun start(context: Context) {
            val intent = Intent(context, GamepadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GamepadService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var handler: GamepadInputHandler? = null
        private set

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        AppRuntime.gamepadService = this

val scrcpy = AppRuntime.scrcpy
handler = if (scrcpy == null) null else GamepadInputHandler(
scope = scope,
deviceName = { "Virtual Gamepad" },
onUhidCreate = { id, vid, pid, name, desc -> scrcpy.uhidCreate(id, vid, pid, name, desc) },
onUhidInput = { id, data -> scrcpy.uhidInput(id, data) },
onUhidDestroy = { id -> scrcpy.uhidDestroy(id) },
onDebugChanged = null,
)
AppRuntime.gamepadHandler = handler

AppRuntime.gamepadHandler = handler

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

override fun onDestroy() {
handler?.destroy()
handler = null
AppRuntime.gamepadHandler = null
AppRuntime.gamepadService = null
scope.cancel()
super.onDestroy()
}


    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "手柄服务",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GamepadService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VirtualGamepad")
            .setContentText("手柄运行中")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openIntent)
            .addAction(0, "停止", stopIntent)
            .setOngoing(true)
            .build()
    }
}

