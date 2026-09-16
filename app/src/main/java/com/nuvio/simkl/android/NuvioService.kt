package com.nuvio.simkl.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo

class NuvioService : Service() {
    private lateinit var server: LocalHttpServer
    private lateinit var store: ConfigStore
    private lateinit var discord: DiscordBridge
    private lateinit var presence: PresenceController

    override fun onCreate() {
        super.onCreate()
        store = ConfigStore(this)
        discord = DiscordBridge(store)
        presence = PresenceController(store, discord)

        val channel = NotificationChannel("nuvio_service", "Nuvio Simkl service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, "nuvio_service")
            .setContentTitle("Nuvio Simkl is running")
            .setContentText("Local bridge and Discord presence are active")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, 1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }

        server = LocalHttpServer(store.load().port, store, presence)
        server.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        if (::server.isInitialized) server.stop()
        if (::presence.isInitialized) presence.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
