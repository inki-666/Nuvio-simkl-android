package com.nuvio.simkl.android

import android.os.Handler
import android.os.Looper

class PresenceController(
    private val config: ConfigStore,
    private val discordBridge: DiscordBridge
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentEvent: PresenceEvent? = null
    private var awayRunnable: Runnable? = null

    fun handle(event: PresenceEvent): Boolean {
        cancelAwayTimer()

        currentEvent = event

        when (event.event.uppercase()) {
            "HOME" -> {
                startAwayTimer()
                return true
            }

            "PLAYING",
            "WATCHING",
            "PLAYBACK_STARTED" -> {
                renderWatching(event)
                return true
            }

            "STOPPED",
            "PLAYBACK_STOPPED",
            "BROWSING" -> {
                renderBrowsing(event)
                return true
            }

            "PROFILE_CHANGED" -> {
                discordBridge.clear()
                return true
            }

            else -> {
                renderBrowsing(event)
                return true
            }
        }
    }

    fun clear() {
        cancelAwayTimer()
        currentEvent = null
        discordBridge.clear()
    }

    fun current(): PresenceEvent? {
        return currentEvent
    }

    private fun renderWatching(event: PresenceEvent) {
        val configValue = config.load()

        if (!configValue.discordEnabled) {
            discordBridge.clear()
            return
        }

        val title = event.title

        if (title.isNullOrBlank()) {
            discordBridge.setBrowsing()
            return
        }

        discordBridge.setWatching(title)
    }

    private fun renderBrowsing(event: PresenceEvent) {
        val configValue = config.load()

        if (!configValue.discordEnabled) {
            discordBridge.clear()
            return
        }

        discordBridge.setBrowsing()
    }

    private fun startAwayTimer() {
        cancelAwayTimer()

        val runnable = Runnable {
            currentEvent = null
            discordBridge.clear()
        }

        awayRunnable = runnable

        mainHandler.postDelayed(
            runnable,
            60_000L
        )
    }

    private fun cancelAwayTimer() {
        awayRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        awayRunnable = null
    }
}
