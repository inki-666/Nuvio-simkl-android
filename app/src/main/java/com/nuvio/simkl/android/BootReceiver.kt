package com.nuvio.simkl.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val config = ConfigStore(context).load()
        if (config.profileUsername.isBlank() && !config.discordEnabled && config.simklClientId.isBlank()) return
        ContextCompat.startForegroundService(context, Intent(context, NuvioService::class.java))
    }
}
