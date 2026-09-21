package com.nuvio.simkl.android

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap

class PresenceController(
    private val store: ConfigStore,
    private val discordBridge: DiscordBridge
) {

    private val handler = Handler(Looper.getMainLooper())

    private val sessions =
        ConcurrentHashMap<String, PresenceSessionState>()

    private val homeTimers =
        ConcurrentHashMap<String, Runnable>()

    private var nextPriorityOrder = 1L

    private var lastRenderedKey: String? = null

    fun handle(event: PresenceEvent) {
        val sessionId = event.sessionId ?: "default"

        when (event.event.uppercase()) {
            "PLAYING" -> handlePlaying(sessionId, event)
            "PAUSED" -> handlePaused(sessionId, event)
            "STOPPED" -> handleStopped(sessionId, event)
            "BROWSING" -> handleBrowsing(sessionId, event)

            "BROWSING_METADATA",
            "METADATA",
            "DETAILS",
            "CATALOG_DETAILS" -> handleMetadataBrowsing(sessionId, event)

            "PROFILE_CHANGED" -> handleProfileChanged(sessionId, event)
            "HOME" -> handleHome(sessionId, event)

            "SESSION_ENDED",
            "CLEAR",
            "DISCONNECTED" -> handleSessionEnded(sessionId)

            else -> {}
        }
    }

    private fun handlePlaying(
        sessionId: String,
        event: PresenceEvent
    ) {
        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]
        val priority = existing?.priorityOrder ?: allocatePriority()

        val previous =
            existing
                ?: PresenceSessionState(
                    sessionId = sessionId,
                    priorityOrder = priority
                )

        val updated = previous.copy(
            sessionId = sessionId,
            profileId = event.profileId ?: previous.profileId,
            active = true,
            away = false,
            playing = true,
            paused = false,
            title = event.title ?: previous.title,
            event = "PLAYING",
            catalogId = event.catalogId ?: previous.catalogId,
            catalogName = event.catalogName ?: previous.catalogName,
            mediaType = event.mediaType ?: previous.mediaType,
            season = event.season ?: previous.season,
            episode = event.episode ?: previous.episode,
            progress = event.progress ?: previous.progress,
            positionSeconds =
                event.positionSeconds ?: previous.positionSeconds,
            durationSeconds =
                event.durationSeconds ?: previous.durationSeconds,
            artwork = event.artwork ?: previous.artwork,
            avatar = event.avatar ?: previous.avatar,
            username = event.username ?: previous.username,
            startTimestampMs =
                event.startTimestampMs ?: previous.startTimestampMs,
            priorityOrder = priority
        )

        sessions[sessionId] = updated
        renderPrioritySession()
    }

    private fun handlePaused(
        sessionId: String,
        event: PresenceEvent
    ) {
        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]
        val priority = existing?.priorityOrder ?: allocatePriority()

        val previous =
            existing
                ?: PresenceSessionState(
                    sessionId = sessionId,
                    priorityOrder = priority
                )

        val updated = previous.copy(
            sessionId = sessionId,
            profileId = event.profileId ?: previous.profileId,
            active = true,
            away = false,
            playing = false,
            paused = true,
            title = event.title ?: previous.title,
            event = "PAUSED",
            catalogId = event.catalogId ?: previous.catalogId,
            catalogName = event.catalogName ?: previous.catalogName,
            mediaType = event.mediaType ?: previous.mediaType,
            season = event.season ?: previous.season,
            episode = event.episode ?: previous.episode,
            progress = event.progress ?: previous.progress,
            positionSeconds =
                event.positionSeconds ?: previous.positionSeconds,
            durationSeconds =
                event.durationSeconds ?: previous.durationSeconds,
            artwork = event.artwork ?: previous.artwork,
            avatar = event.avatar ?: previous.avatar,
            username = event.username ?: previous.username,
            startTimestampMs =
                event.startTimestampMs ?: previous.startTimestampMs,
            priorityOrder = priority
        )

        sessions[sessionId] = updated
        renderPrioritySession()
    }

    private fun handleStopped(
        sessionId: String,
        event: PresenceEvent
    ) {
        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]
        val priority = existing?.priorityOrder ?: allocatePriority()

        val previous =
            existing
                ?: PresenceSessionState(
                    sessionId = sessionId,
                    priorityOrder = priority
                )

        val updated = previous.copy(
            sessionId = sessionId,
            profileId = event.profileId ?: previous.profileId,
            active = true,
            away = false,
            playing = false,
            paused = false,
            event = "BROWSING",
            priorityOrder = priority
        )

        sessions[sessionId] = updated
        renderPrioritySession()
    }

    private fun handleBrowsing(
        sessionId: String,
        event: PresenceEvent
    ) {
        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]
        val priority = existing?.priorityOrder ?: allocatePriority()

        val previous =
            existing
                ?: PresenceSessionState(
                    sessionId = sessionId,
                    priorityOrder = priority
                )

        val updated = previous.copy(
            sessionId = sessionId,
            profileId = event.profileId ?: previous.profileId,
            active = true,
            away = false,
            playing = false,
            paused = false,
            title = event.title ?: previous.title,
            event = "BROWSING",
            catalogId = event.catalogId ?: previous.catalogId,
            catalogName = event.catalogName ?: previous.catalogName,
            mediaType = event.mediaType ?: previous.mediaType,
            season = event.season ?: previous.season,
            episode = event.episode ?: previous.episode,
            artwork = event.artwork ?: previous.artwork,
            avatar = event.avatar ?: previous.avatar,
            username = event.username ?: previous.username,
            priorityOrder = priority
        )

        sessions[sessionId] = updated
        renderPrioritySession()
    }

    private fun handleMetadataBrowsing(
        sessionId: String,
        event: PresenceEvent
    ) {
        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]
        val priority = existing?.priorityOrder ?: allocatePriority()

        val previous =
            existing
                ?: PresenceSessionState(
                    sessionId = sessionId,
                    priorityOrder = priority
                )

        val updated = previous.copy(
            sessionId = sessionId,
            profileId = event.profileId ?: previous.profileId,
            active = true,
            away = false,
            playing = false,
            paused = false,
            title = event.title ?: previous.title,
            event = "BROWSING_METADATA",
            catalogId = event.catalogId ?: previous.catalogId,
            catalogName = event.catalogName ?: previous.catalogName,
            mediaType = event.mediaType ?: previous.mediaType,
            season = event.season ?: previous.season,
            episode = event.episode ?: previous.episode,
            progress = event.progress ?: previous.progress,
            positionSeconds =
                event.positionSeconds ?: previous.positionSeconds,
            durationSeconds =
                event.durationSeconds ?: previous.durationSeconds,
            artwork = event.artwork ?: previous.artwork,
            avatar = event.avatar ?: previous.avatar,
            username = event.username ?: previous.username,
            startTimestampMs =
                event.startTimestampMs ?: previous.startTimestampMs,
            priorityOrder = priority
        )

        sessions[sessionId] = updated
        renderPrioritySession()
    }

    private fun handleProfileChanged(
        sessionId: String,
        event: PresenceEvent
    ) {
        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]
        val priority = existing?.priorityOrder ?: allocatePriority()

        val previous =
            existing
                ?: PresenceSessionState(
                    sessionId = sessionId,
                    priorityOrder = priority
                )

        val updated = previous.copy(
            sessionId = sessionId,
            profileId = event.profileId ?: previous.profileId,
            active = true,
            away = false,
            priorityOrder = priority
        )

        sessions[sessionId] = updated
        renderPrioritySession()
    }

    private fun handleHome(
        sessionId: String,
        event: PresenceEvent
    ) {
        val existing = sessions[sessionId]
        val priority = existing?.priorityOrder ?: allocatePriority()

        val previous =
            existing
                ?: PresenceSessionState(
                    sessionId = sessionId,
                    priorityOrder = priority
                )

        val homeEvent: String
        val homePlaying: Boolean
        val homePaused: Boolean

        when {
            previous.playing -> {
                homeEvent = "PAUSED"
                homePlaying = false
                homePaused = true
            }

            previous.paused -> {
                homeEvent = "PAUSED"
                homePlaying = false
                homePaused = true
            }

            previous.event == "BROWSING_METADATA" -> {
                homeEvent = "BROWSING_METADATA"
                homePlaying = false
                homePaused = false
            }

            else -> {
                homeEvent = "BROWSING"
                homePlaying = false
                homePaused = false
            }
        }

        val updated = previous.copy(
            sessionId = sessionId,
            profileId = event.profileId ?: previous.profileId,
            active = true,
            away = true,
            playing = homePlaying,
            paused = homePaused,
            event = homeEvent,
            avatar = event.avatar ?: previous.avatar,
            username = event.username ?: previous.username,
            priorityOrder = priority
        )

        sessions[sessionId] = updated

        startHomeTimer(sessionId)
        renderPrioritySession()
    }

    private fun handleSessionEnded(
        sessionId: String
    ) {
        cancelHomeTimer(sessionId)

        sessions.remove(sessionId)

        renderPrioritySession()
    }

    private fun startHomeTimer(
        sessionId: String
    ) {
        cancelHomeTimer(sessionId)

        val runnable = Runnable {
            val current = sessions[sessionId]

            if (current != null && current.away) {
                sessions.remove(sessionId)
                homeTimers.remove(sessionId)
                renderPrioritySession()
            }
        }

        homeTimers[sessionId] = runnable

        handler.postDelayed(
            runnable,
            HOME_GRACE_PERIOD_MS
        )
    }

    private fun cancelHomeTimer(
        sessionId: String
    ) {
        homeTimers.remove(sessionId)?.let {
            handler.removeCallbacks(it)
        }
    }

    private fun allocatePriority(): Long {
        return nextPriorityOrder++
    }

    private fun findPrioritySession(): PresenceSessionState? {
        return sessions.values
            .filter { it.active }
            .minByOrNull { it.priorityOrder }
    }

    private fun renderPrioritySession() {
        val priority = findPrioritySession()

        if (priority == null) {
            if (lastRenderedKey != "CLEAR") {
                discordBridge.clear()
                lastRenderedKey = "CLEAR"
            }

            return
        }

        if (!isRpcAllowed(priority.profileId)) {
            if (lastRenderedKey != "CLEAR") {
                discordBridge.clear()
                lastRenderedKey = "CLEAR"
            }

            return
        }

        val eventKey = buildEventKey(priority)

        if (eventKey == lastRenderedKey) {
            return
        }

        renderState(priority)

        lastRenderedKey = eventKey
    }

    private fun renderState(
        state: PresenceSessionState
    ) {
        when {
            state.playing -> {
                val title = state.title

                if (!title.isNullOrBlank()) {
                    discordBridge.setWatching(title)
                } else {
                    discordBridge.setBrowsing()
                }
            }

            state.paused -> {
                val title = state.title

                if (!title.isNullOrBlank()) {
                    discordBridge.setPaused(title)
                } else {
                    discordBridge.setBrowsing()
                }
            }

            state.event == "BROWSING_METADATA" -> {
                val title = state.title

                if (!title.isNullOrBlank()) {
                    discordBridge.setBrowsingMetadata(title)
                } else {
                    discordBridge.setBrowsing()
                }
            }

            else -> {
                discordBridge.setBrowsing()
            }
        }
    }

    private fun isRpcAllowed(
        profileId: String?
    ): Boolean {
        return store.load().discordEnabled
    }

    private fun buildEventKey(
        state: PresenceSessionState
    ): String {
        return listOf(
            state.sessionId,
            state.profileId,
            state.event,
            state.playing,
            state.paused,
            state.title,
            state.catalogId,
            state.catalogName,
            state.mediaType,
            state.season,
            state.episode,
            state.progress,
            state.positionSeconds,
            state.durationSeconds,
            state.artwork,
            state.away
        ).joinToString("|")
    }

    fun getPrioritySession(): PresenceSessionState? {
        return findPrioritySession()
    }

    fun getSessions(): List<PresenceSessionState> {
        return sessions.values
            .sortedBy { it.priorityOrder }
            .toList()
    }

    fun clear() {
        homeTimers.values.forEach {
            handler.removeCallbacks(it)
        }

        homeTimers.clear()
        sessions.clear()
        lastRenderedKey = null

        discordBridge.clear()
    }

    companion object {
        private const val HOME_GRACE_PERIOD_MS = 60_000L
    }
}
