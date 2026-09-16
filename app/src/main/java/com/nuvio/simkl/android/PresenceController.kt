package com.nuvio.simkl.android

import android.os.SystemClock

class PresenceController(
    private val config: ConfigStore,
    private val discord: DiscordBridge
) {
    @Volatile private var lastKey: String? = null
    @Volatile private var lastSentElapsedMs: Long = 0L
    @Volatile private var current: PresenceEvent? = null

    fun handle(event: PresenceEvent): Boolean {
        val normalized = normalize(event)
        if (normalized.event.equals("CLEAR", true)) {
            clear()
            return true
        }

        val key = normalized.key()
        val now = SystemClock.elapsedRealtime()
        // Ignore exact duplicates, but permit a refresh after 2 seconds.
        if (key == lastKey && now - lastSentElapsedMs < 2000L) return true

        val c = config.load()
        val username = normalized.username?.takeIf { it.isNotBlank() } ?: c.profileUsername
        val small = normalized.avatar?.takeIf { it.isNotBlank() } ?: c.profileAvatarUrl
        val state = when (normalized.event.uppercase()) {
            "HOME" -> username.ifBlank { "Nuvio" }
            "CATALOG", "CATALOG_FOCUSED" -> username.ifBlank { "Nuvio" }
            "DETAIL" -> normalized.mediaType?.takeIf { it.isNotBlank() } ?: "Nuvio"
            "PLAYBACK" -> playbackState(normalized)
            else -> username.ifBlank { "Nuvio" }
        }
        val details = when (normalized.event.uppercase()) {
            "HOME" -> "Browsing Nuvio"
            "CATALOG", "CATALOG_FOCUSED" -> "Browsing ${normalized.catalogName ?: normalized.catalogId ?: "catalogue"}"
            "DETAIL" -> normalized.title ?: "Viewing Nuvio"
            "PLAYBACK" -> normalized.title ?: "Watching Nuvio"
            else -> normalized.title ?: "Browsing Nuvio"
        }

        val ok = discord.update(
            PresenceState(
                details = details,
                state = state,
                positionSeconds = normalized.positionSeconds,
                durationSeconds = normalized.durationSeconds,
                largeImageUrl = normalized.artwork,
                smallImageUrl = small,
                startTimestampMs = normalized.startTimestampMs ?: System.currentTimeMillis()
            )
        )
        if (ok) {
            lastKey = key
            lastSentElapsedMs = now
            current = normalized
        }
        return ok
    }

    fun current(): PresenceEvent? = current

    fun clear() {
        current = null
        lastKey = null
        lastSentElapsedMs = 0L
        discord.clear()
    }

    private fun normalize(e: PresenceEvent): PresenceEvent = e.copy(
        event = e.event.trim().uppercase(),
        catalogId = e.catalogId?.trim()?.takeIf { it.isNotEmpty() },
        catalogName = e.catalogName?.trim()?.takeIf { it.isNotEmpty() },
        title = e.title?.trim()?.takeIf { it.isNotEmpty() },
        mediaType = e.mediaType?.trim()?.takeIf { it.isNotEmpty() },
        username = e.username?.trim()?.takeIf { it.isNotEmpty() },
        artwork = e.artwork?.trim()?.takeIf { it.isNotEmpty() },
        avatar = e.avatar?.trim()?.takeIf { it.isNotEmpty() },
        progress = e.progress?.coerceIn(0, 100),
        positionSeconds = e.positionSeconds?.coerceAtLeast(0L),
        durationSeconds = e.durationSeconds?.coerceAtLeast(0L)
    )

    private fun playbackState(e: PresenceEvent): String {
        val ep = if (e.season != null && e.episode != null) "S%02dE%02d".format(e.season, e.episode) else null
        val progressText = if (e.positionSeconds != null && e.durationSeconds != null && e.durationSeconds > 0) {
            "${formatTime(e.positionSeconds)} / ${formatTime(e.durationSeconds)}"
        } else e.progress?.let { "$it%" }
        return listOfNotNull(ep, progressText).joinToString(" • ").ifBlank { e.mediaType ?: "Watching" }
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun PresenceEvent.key(): String = listOf(
        event, catalogId, catalogName, title, mediaType, season, episode, progress, positionSeconds, durationSeconds, artwork, avatar, username
    ).joinToString("|")
}
