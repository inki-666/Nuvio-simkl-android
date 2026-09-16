package com.nuvio.simkl.android

import android.util.Log

data class PresenceState(
    val details: String,
    val state: String,
    val largeImageUrl: String? = null,
    val smallImageUrl: String? = null,
    val startTimestampMs: Long = System.currentTimeMillis(),
    val positionSeconds: Long? = null,
    val durationSeconds: Long? = null
)

class DiscordBridge(private val config: ConfigStore) {
    companion object {
        private const val TAG = "NuvioDiscord"
        init {
            System.loadLibrary("nuvio_discord_bridge")
        }
    }

    private external fun nativeAvailable(): Boolean
    private external fun nativeSetPresence(
        applicationId: Long,
        details: String,
        state: String,
        largeImageUrl: String?,
        smallImageUrl: String?,
        startTimestampMs: Long,
        endTimestampMs: Long
    ): Boolean
    private external fun nativeClearPresence()

    fun update(p: PresenceState): Boolean {
        val c = config.load()
        if (!c.discordEnabled || c.discordApplicationId == 0L) return false
        if (!nativeAvailable()) {
            Log.w(TAG, "Discord Social SDK is not linked. See DISCORD_SDK.md")
            return false
        }
        return nativeSetPresence(
            c.discordApplicationId,
            p.details,
            p.state,
            p.largeImageUrl,
            p.smallImageUrl ?: c.profileAvatarUrl,
            p.startTimestampMs,
            if (p.positionSeconds != null && p.durationSeconds != null && p.durationSeconds > 0) p.startTimestampMs + p.durationSeconds * 1000L else 0L
        )
    }

    fun clear() {
        runCatching { nativeClearPresence() }
    }
}
