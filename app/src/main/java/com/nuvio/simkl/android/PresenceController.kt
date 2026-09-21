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

    /**
     * Receives an event from Nuvio.
     */
    fun handle(event: PresenceEvent) {

        val sessionId = event.sessionId ?: "default"

        when (event.event.uppercase()) {

            "PLAYING" -> {
                handlePlaying(
                    sessionId = sessionId,
                    event = event
                )
            }

            "PAUSED" -> {
                handlePaused(
                    sessionId = sessionId,
                    event = event
                )
            }

            "STOPPED" -> {
                handleStopped(
                    sessionId = sessionId,
                    event = event
                )
            }

            "BROWSING" -> {
                handleBrowsing(
                    sessionId = sessionId,
                    event = event
                )
            }

            "BROWSING_METADATA",
            "METADATA",
            "DETAILS",
            "CATALOG_DETAILS" -> {
                handleMetadataBrowsing(
                    sessionId = sessionId,
                    event = event
                )
            }

            "PROFILE_CHANGED" -> {
                handleProfileChanged(
                    sessionId = sessionId,
                    event = event
                )
            }

            "HOME" -> {
                handleHome(
                    sessionId = sessionId,
                    event = event
                )
            }

            "SESSION_ENDED",
            "CLEAR",
            "DISCONNECTED" -> {
                handleSessionEnded(sessionId)
            }

            else -> {
                /*
                 * Unknown events are intentionally ignored.
                 *
                 * This prevents a new/unsupported Nuvio event from
                 * accidentally clearing or changing Discord presence.
                 */
            }
        }
    }

    /**
     * PLAYING
     *
     * Media is actively playing.
     */
    private fun handlePlaying(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]

        val priority =
            existing?.priorityOrder
                ?: allocatePriority()

        val previous =
            existing ?: PresenceSessionState(
                sessionId = sessionId,
                priorityOrder = priority
            )

        val updated =
            previous.copy(
                sessionId = sessionId,

                profileId =
                    event.profileId
                        ?: previous.profileId,

                active = true,

                away = false,

                playing = true,

                paused = false,

                title =
                    event.title
                        ?: previous.title,

                event = "PLAYING",

                catalogId =
                    event.catalogId
                        ?: previous.catalogId,

                catalogName =
                    event.catalogName
                        ?: previous.catalogName,

                mediaType =
                    event.mediaType
                        ?: previous.mediaType,

                season =
                    event.season
                        ?: previous.season,

                episode =
                    event.episode
                        ?: previous.episode,

                progress =
                    event.progress
                        ?: previous.progress,

                positionSeconds =
                    event.positionSeconds
                        ?: previous.positionSeconds,

                durationSeconds =
                    event.durationSeconds
                        ?: previous.durationSeconds,

                artwork =
                    event.artwork
                        ?: previous.artwork,

                avatar =
                    event.avatar
                        ?: previous.avatar,

                username =
                    event.username
                        ?: previous.username,

                startTimestampMs =
                    event.startTimestampMs
                        ?: previous.startTimestampMs,

                priorityOrder = priority
            )

        sessions[sessionId] = updated

        renderPrioritySession()
    }

    /**
     * PAUSED
     *
     * Media is paused but the Nuvio session remains active.
     */
    private fun handlePaused(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]

        val priority =
            existing?.priorityOrder
                ?: allocatePriority()

        val previous =
            existing ?: PresenceSessionState(
                sessionId = sessionId,
                priorityOrder = priority
            )

        val updated =
            previous.copy(
                sessionId = sessionId,

                profileId =
                    event.profileId
                        ?: previous.profileId,

                active = true,

                away = false,

                playing = false,

                paused = true,

                title =
                    event.title
                        ?: previous.title,

                event = "PAUSED",

                catalogId =
                    event.catalogId
                        ?: previous.catalogId,

                catalogName =
                    event.catalogName
                        ?: previous.catalogName,

                mediaType =
                    event.mediaType
                        ?: previous.mediaType,

                season =
                    event.season
                        ?: previous.season,

                episode =
                    event.episode
                        ?: previous.episode,

                progress =
                    event.progress
                        ?: previous.progress,

                positionSeconds =
                    event.positionSeconds
                        ?: previous.positionSeconds,

                durationSeconds =
                    event.durationSeconds
                        ?: previous.durationSeconds,

                artwork =
                    event.artwork
                        ?: previous.artwork,

                avatar =
                    event.avatar
                        ?: previous.avatar,

                username =
                    event.username
                        ?: previous.username,

                startTimestampMs =
                    event.startTimestampMs
                        ?: previous.startTimestampMs,

                priorityOrder = priority
            )

        sessions[sessionId] = updated

        renderPrioritySession()
    }

    /**
     * STOPPED
     *
     * Playback has ended, but Nuvio is still open.
     *
     * This does NOT end the session.
     *
     * Discord becomes a generic browsing presence.
     */
    private fun handleStopped(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]

        val priority =
            existing?.priorityOrder
                ?: allocatePriority()

        val previous =
            existing ?: PresenceSessionState(
                sessionId = sessionId,
                priorityOrder = priority
            )

        val updated =
            previous.copy(
                sessionId = sessionId,

                profileId =
                    event.profileId
                        ?: previous.profileId,

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

    /**
     * Generic Nuvio browsing.
     */
    private fun handleBrowsing(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]

        val priority =
            existing?.priorityOrder
                ?: allocatePriority()

        val previous =
            existing ?: PresenceSessionState(
                sessionId = sessionId,
                priorityOrder = priority
            )

        val updated =
            previous.copy(
                sessionId = sessionId,

                profileId =
                    event.profileId
                        ?: previous.profileId,

                active = true,

                away = false,

                playing = false,

                paused = false,

                title =
                    event.title
                        ?: previous.title,

                event = "BROWSING",

                catalogId =
                    event.catalogId
                        ?: previous.catalogId,

                catalogName =
                    event.catalogName
                        ?: previous.catalogName,

                mediaType =
                    event.mediaType
                        ?: previous.mediaType,

                season =
                    event.season
                        ?: previous.season,

                episode =
                    event.episode
                        ?: previous.episode,

                artwork =
                    event.artwork
                        ?: previous.artwork,

                avatar =
                    event.avatar
                        ?: previous.avatar,

                username =
                    event.username
                        ?: previous.username,

                priorityOrder = priority
            )

        sessions[sessionId] = updated

        renderPrioritySession()
    }

    /**
     * BROWSING_METADATA
     *
     * The user has opened a movie/show/anime metadata page.
     *
     * This is different from playback.
     */
    private fun handleMetadataBrowsing(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]

        val priority =
            existing?.priorityOrder
                ?: allocatePriority()

        val previous =
            existing ?: PresenceSessionState(
                sessionId = sessionId,
                priorityOrder = priority
            )

        val updated =
            previous.copy(
                sessionId = sessionId,

                profileId =
                    event.profileId
                        ?: previous.profileId,

                active = true,

                away = false,

                playing = false,

                paused = false,

                title =
                    event.title
                        ?: previous.title,

                event = "BROWSING_METADATA",

                catalogId =
                    event.catalogId
                        ?: previous.catalogId,

                catalogName =
                    event.catalogName
                        ?: previous.catalogName,

                mediaType =
                    event.mediaType
                        ?: previous.mediaType,

                season =
                    event.season
                        ?: previous.season,

                episode =
                    event.episode
                        ?: previous.episode,

                progress =
                    event.progress
                        ?: previous.progress,

                positionSeconds =
                    event.positionSeconds
                        ?: previous.positionSeconds,

                durationSeconds =
                    event.durationSeconds
                        ?: previous.durationSeconds,

                artwork =
                    event.artwork
                        ?: previous.artwork,

                avatar =
                    event.avatar
                        ?: previous.avatar,

                username =
                    event.username
                        ?: previous.username,

                startTimestampMs =
                    event.startTimestampMs
                        ?: previous.startTimestampMs,

                priorityOrder = priority
            )

        sessions[sessionId] = updated

        renderPrioritySession()
    }

    /**
     * Profile changed within the same Nuvio session.
     *
     * The session keeps its existing priority.
     */
    private fun handleProfileChanged(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelHomeTimer(sessionId)

        val existing = sessions[sessionId]

        val priority =
            existing?.priorityOrder
                ?: allocatePriority()

        val previous =
            existing ?: PresenceSessionState(
                sessionId = sessionId,
                priorityOrder = priority
            )

        val updated =
            previous.copy(
                sessionId = sessionId,

                profileId =
                    event.profileId
                        ?: previous.profileId,

                active = true,

                away = false,

                priorityOrder = priority
            )

        sessions[sessionId] = updated

        renderPrioritySession()
    }

    /**
     * HOME
     *
     * Going Home does not immediately end the session.
     *
     * If the session was PLAYING:
     *
     *     PLAYING -> PAUSED
     *
     * If it was browsing metadata:
     *
     *     BROWSING_METADATA -> keep browsing metadata
     *
     * The current Discord presence remains visible for 60 seconds.
     *
     * If the user returns before 60 seconds, the timer is cancelled.
     *
     * If 60 seconds pass, the session ends.
     */
    private fun handleHome(
        sessionId: String,
        event: PresenceEvent
    ) {

        val existing = sessions[sessionId]

        val priority =
            existing?.priorityOrder
                ?: allocatePriority()

        val previous =
            existing ?: PresenceSessionState(
                sessionId = sessionId,
                priorityOrder = priority
            )

        /*
         * Determine what presence should remain visible while Home
         * is active.
         */
        val homeEvent: String
        val homePlaying: Boolean
        val homePaused: Boolean

        when {
            previous.playing -> {
                /*
                 * Playing -> Home means Discord becomes PAUSED.
                 */
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

        val updated =
            previous.copy(
                sessionId = sessionId,

                profileId =
                    event.profileId
                        ?: previous.profileId,

                active = true,

                away = true,

                playing = homePlaying,

                paused = homePaused,

                event = homeEvent,

                avatar =
                    event.avatar
                        ?: previous.avatar,

                username =
                    event.username
                        ?: previous.username,

                priorityOrder = priority
            )

        sessions[sessionId] = updated

        startHomeTimer(sessionId)

        renderPrioritySession()
    }

    /**
     * Ends one complete Nuvio session.
     */
    private fun handleSessionEnded(
        sessionId: String
    ) {

        cancelHomeTimer(sessionId)

        sessions.remove(sessionId)

        renderPrioritySession()
    }

    /**
     * Starts/restarts the 60-second Home grace period.
     */
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

    /**
     * Cancels an existing Home timer.
     */
    private fun cancelHomeTimer(
        sessionId: String
    ) {

        homeTimers.remove(sessionId)?.let {
            handler.removeCallbacks(it)
        }
    }

    /**
     * Allocates a permanent priority number for a new session.
     */
    private fun allocatePriority(): Long {
        return nextPriorityOrder++
    }

    /**
     * Finds the currently highest-priority active session.
     *
     * Lower priorityOrder wins.
     */
    private fun getPrioritySession(): PresenceSessionState? {

        return sessions.values
            .filter { it.active }
            .minByOrNull { it.priorityOrder }
    }

    /**
     * Renders the highest-priority session to Discord.
     */
    private fun renderPrioritySession() {

        val priority =
            getPrioritySession()

        if (priority == null) {

            if (lastRenderedKey != "CLEAR") {
                discordBridge.clear()
                lastRenderedKey = "CLEAR"
            }

            return
        }

        /*
         * Profile-level RPC permission.
         */
        if (!isRpcAllowed(priority.profileId)) {

            if (lastRenderedKey != "CLEAR") {
                discordBridge.clear()
                lastRenderedKey = "CLEAR"
            }

            return
        }

        /*
         * HOME deliberately does not cause another Discord update.
         *
         * The state has already been converted:
         *
         * PLAYING -> PAUSED
         * BROWSING_METADATA -> BROWSING_METADATA
         * BROWSING -> BROWSING
         *
         * The existing presence remains visible while the timer runs.
         */
        if (priority.away) {

            val homeKey =
                buildEventKey(priority)

            if (homeKey != lastRenderedKey) {

                renderState(priority)

                lastRenderedKey = homeKey
            }

            return
        }

        val eventKey =
            buildEventKey(priority)

        if (eventKey == lastRenderedKey) {
            return
        }

        renderState(priority)

        lastRenderedKey = eventKey
    }

    /**
     * Converts one session state into a Discord presence.
     */
    private fun renderState(
        state: PresenceSessionState
    ) {

        when {

            state.playing -> {

                val title =
                    state.title

                if (!title.isNullOrBlank()) {
                    discordBridge.setWatching(title)
                } else {
                    discordBridge.setBrowsing()
                }
            }

            state.paused -> {

                val title =
                    state.title

                if (!title.isNullOrBlank()) {
                    discordBridge.setPaused(title)
                } else {
                    discordBridge.setBrowsing()
                }
            }

            state.event == "BROWSING_METADATA" -> {

                val title =
                    state.title

                if (!title.isNullOrBlank()) {
                    discordBridge.setBrowsingMetadata(
                        title = title
                    )
                } else {
                    discordBridge.setBrowsing()
                }
            }

            else -> {
                discordBridge.setBrowsing()
            }
        }
    }

    /**
     * Checks whether Discord RPC is enabled.
     *
     * Profile-specific permissions will be wired to ProfileStore
     * once profile configuration is connected to this controller.
     */
    private fun isRpcAllowed(
        profileId: String?
    ): Boolean {

        return store.load().discordEnabled
    }

    /**
     * Builds a key representing everything that affects the
     * currently rendered Discord state.
     *
     * This prevents duplicate Discord updates.
     */
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

    /**
     * Returns the current highest-priority session.
     */
    fun getPrioritySession(): PresenceSessionState? {
        return getPrioritySession()
    }

    /**
     * Returns a snapshot of all currently tracked sessions.
     */
    fun getSessions(): List<PresenceSessionState> {
        return sessions.values
            .sortedBy { it.priorityOrder }
            .toList()
    }

    /**
     * Clears everything.
     */
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
