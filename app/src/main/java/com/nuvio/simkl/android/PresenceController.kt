package com.nuvio.simkl.android

import android.os.Handler
import android.os.Looper
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicLong

class PresenceController(
    private val config: ConfigStore,
    private val discordBridge: DiscordBridge
) {

    private val mainHandler =
        Handler(Looper.getMainLooper())

    /**
     * All currently known sessions.
     *
     * The map key is sessionId.
     */
    private val sessions =
        LinkedHashMap<String, PresenceSessionState>()

    /**
     * One HOME timeout per session.
     */
    private val awayRunnables =
        mutableMapOf<String, Runnable>()

    /**
     * Monotonically increasing priority number.
     *
     * The first active session gets 1,
     * the next gets 2, etc.
     */
    private val priorityCounter =
        AtomicLong(0L)

    /**
     * Prevents unnecessary duplicate Discord updates.
     */
    private var lastRenderedSessionId: String? = null

    private var lastRenderedEventKey: String? = null

    /**
     * Receives an event from Nuvio.
     */
    @Synchronized
    fun handle(
        event: PresenceEvent
    ): Boolean {

        val sessionId =
            event.sessionId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "default"

        val normalizedEvent =
            event.event
                .trim()
                .uppercase()

        when (normalizedEvent) {

            "HOME" -> {
                handleHome(
                    sessionId,
                    event
                )
            }

            "PLAYING",
            "WATCHING",
            "PLAYBACK_STARTED" -> {
                handleWatching(
                    sessionId,
                    event
                )
            }

            "STOPPED",
            "PLAYBACK_STOPPED",
            "BROWSING" -> {
                handleBrowsing(
                    sessionId,
                    event
                )
            }

            "PROFILE_CHANGED" -> {
                handleProfileChanged(
                    sessionId,
                    event
                )
            }

            "CLEAR",
            "DISCONNECTED",
            "SESSION_ENDED" -> {
                removeSession(
                    sessionId
                )
            }

            else -> {
                handleBrowsing(
                    sessionId,
                    event
                )
            }
        }

        renderPrioritySession()

        return true
    }

    /**
     * Clears every session.
     */
    @Synchronized
    fun clear() {

        val sessionIds =
            sessions.keys.toList()

        sessionIds.forEach {
            cancelAwayTimer(it)
        }

        sessions.clear()

        lastRenderedSessionId = null
        lastRenderedEventKey = null

        discordBridge.clear()
    }

    /**
     * Returns the Discord-priority session.
     *
     * This preserves the old current() API used by LocalHttpServer.
     */
    @Synchronized
    fun current(): PresenceEvent? {

        val priority =
            prioritySession()
                ?: return null

        return sessionToEvent(priority)
    }

    /**
     * Returns all active/tracked sessions.
     *
     * Useful later for the UI.
     */
    @Synchronized
    fun sessions(): List<PresenceSessionState> {
        return sessions.values
            .sortedBy {
                it.priorityOrder
            }
    }

    /**
     * Returns the session currently controlling Discord.
     */
    @Synchronized
    fun prioritySession(): PresenceSessionState? {

        return sessions.values
            .filter {
                it.active
            }
            .minByOrNull {
                it.priorityOrder
            }
    }

    /**
     * Number of active sessions.
     */
    @Synchronized
    fun activeSessionCount(): Int {

        return sessions.values.count {
            it.active
        }
    }

    private fun handleHome(
        sessionId: String,
        event: PresenceEvent
    ) {

        val existing =
            sessions[sessionId]

        val state =
            if (existing == null) {

                PresenceSessionState(
                    sessionId = sessionId,
                    profileId = event.profileId,
                    active = true,
                    away = true,
                    playing = false,
                    title = null,
                    event = "HOME",
                    catalogId = event.catalogId,
                    catalogName = event.catalogName,
                    mediaType = event.mediaType,
                    season = event.season,
                    episode = event.episode,
                    progress = event.progress,
                    positionSeconds = event.positionSeconds,
                    durationSeconds = event.durationSeconds,
                    artwork = event.artwork,
                    avatar = event.avatar,
                    username = event.username,
                    startTimestampMs = event.startTimestampMs,
                    priorityOrder =
                        priorityCounter.incrementAndGet()
                )

            } else {

                existing.copy(
                    active = true,
                    away = true,
                    event = "HOME",
                    profileId =
                        event.profileId
                            ?: existing.profileId,
                    avatar =
                        event.avatar
                            ?: existing.avatar,
                    username =
                        event.username
                            ?: existing.username
                )
            }

        sessions[sessionId] =
            state

        startAwayTimer(
            sessionId
        )
    }

    private fun handleWatching(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelAwayTimer(
            sessionId
        )

        val existing =
            sessions[sessionId]

        val state =
            if (existing == null) {

                PresenceSessionState(
                    sessionId = sessionId,
                    profileId = event.profileId,
                    active = true,
                    away = false,
                    playing = true,
                    title = event.title,
                    event = "PLAYING",
                    catalogId = event.catalogId,
                    catalogName = event.catalogName,
                    mediaType = event.mediaType,
                    season = event.season,
                    episode = event.episode,
                    progress = event.progress,
                    positionSeconds = event.positionSeconds,
                    durationSeconds = event.durationSeconds,
                    artwork = event.artwork,
                    avatar = event.avatar,
                    username = event.username,
                    startTimestampMs = event.startTimestampMs,
                    priorityOrder =
                        priorityCounter.incrementAndGet()
                )

            } else {

                existing.copy(
                    active = true,
                    away = false,
                    playing = true,
                    title = event.title,
                    event = "PLAYING",
                    profileId =
                        event.profileId
                            ?: existing.profileId,
                    catalogId = event.catalogId,
                    catalogName = event.catalogName,
                    mediaType = event.mediaType,
                    season = event.season,
                    episode = event.episode,
                    progress = event.progress,
                    positionSeconds =
                        event.positionSeconds,
                    durationSeconds =
                        event.durationSeconds,
                    artwork =
                        event.artwork
                            ?: existing.artwork,
                    avatar =
                        event.avatar
                            ?: existing.avatar,
                    username =
                        event.username
                            ?: existing.username,
                    startTimestampMs =
                        event.startTimestampMs
                            ?: existing.startTimestampMs
                )
            }

        sessions[sessionId] =
            state
    }

    private fun handleBrowsing(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelAwayTimer(
            sessionId
        )

        val existing =
            sessions[sessionId]

        val state =
            if (existing == null) {

                PresenceSessionState(
                    sessionId = sessionId,
                    profileId = event.profileId,
                    active = true,
                    away = false,
                    playing = false,
                    title = null,
                    event = "BROWSING",
                    catalogId = event.catalogId,
                    catalogName = event.catalogName,
                    mediaType = event.mediaType,
                    season = event.season,
                    episode = event.episode,
                    progress = event.progress,
                    positionSeconds = event.positionSeconds,
                    durationSeconds = event.durationSeconds,
                    artwork = event.artwork,
                    avatar = event.avatar,
                    username = event.username,
                    startTimestampMs = event.startTimestampMs,
                    priorityOrder =
                        priorityCounter.incrementAndGet()
                )

            } else {

                existing.copy(
                    active = true,
                    away = false,
                    playing = false,
                    title = null,
                    event = "BROWSING",
                    profileId =
                        event.profileId
                            ?: existing.profileId,
                    catalogId =
                        event.catalogId
                            ?: existing.catalogId,
                    catalogName =
                        event.catalogName
                            ?: existing.catalogName,
                    mediaType =
                        event.mediaType
                            ?: existing.mediaType,
                    season =
                        event.season
                            ?: existing.season,
                    episode =
                        event.episode
                            ?: existing.episode,
                    progress =
                        event.progress
                            ?: existing.progress,
                    positionSeconds =
                        event.positionSeconds
                            ?: existing.positionSeconds,
                    durationSeconds =
                        event.durationSeconds
                            ?: existing.durationSeconds,
                    artwork =
                        event.artwork
                            ?: existing.artwork,
                    avatar =
                        event.avatar
                            ?: existing.avatar,
                    username =
                        event.username
                            ?: existing.username,
                    startTimestampMs =
                        event.startTimestampMs
                            ?: existing.startTimestampMs
                )
            }

        sessions[sessionId] =
            state
    }

    private fun handleProfileChanged(
        sessionId: String,
        event: PresenceEvent
    ) {

        cancelAwayTimer(
            sessionId
        )

        val existing =
            sessions[sessionId]

        /**
         * IMPORTANT:
         *
         * A profile switch does NOT create a new priority slot.
         *
         * The session keeps its original priority position.
         */
        val state =
            if (existing == null) {

                PresenceSessionState(
                    sessionId = sessionId,
                    profileId = event.profileId,
                    active = true,
                    away = false,
                    playing = false,
                    title = null,
                    event = "BROWSING",
                    catalogId = event.catalogId,
                    catalogName = event.catalogName,
                    mediaType = event.mediaType,
                    season = event.season,
                    episode = event.episode,
                    progress = event.progress,
                    positionSeconds = event.positionSeconds,
                    durationSeconds = event.durationSeconds,
                    artwork = event.artwork,
                    avatar = event.avatar,
                    username = event.username,
                    startTimestampMs = event.startTimestampMs,
                    priorityOrder =
                        priorityCounter.incrementAndGet()
                )

            } else {

                existing.copy(
                    active = true,
                    away = false,
                    playing = false,
                    title = null,
                    event = "BROWSING",
                    profileId =
                        event.profileId
                            ?: existing.profileId,
                    catalogId = event.catalogId,
                    catalogName = event.catalogName,
                    mediaType = event.mediaType,
                    artwork =
                        event.artwork
                            ?: existing.artwork,
                    avatar =
                        event.avatar
                            ?: existing.avatar,
                    username =
                        event.username
                            ?: existing.username
                )
            }

        sessions[sessionId] =
            state
    }

    private fun removeSession(
        sessionId: String
    ) {

        cancelAwayTimer(
            sessionId
        )

        sessions.remove(
            sessionId
        )
    }

    private fun startAwayTimer(
        sessionId: String
    ) {

        cancelAwayTimer(
            sessionId
        )

        val runnable =
            Runnable {

                synchronized(this) {

                    val state =
                        sessions[sessionId]
                            ?: return@synchronized

                    /**
                     * HOME grace period expired.
                     *
                     * The session is now completely inactive.
                     */
                    sessions[sessionId] =
                        state.copy(
                            active = false,
                            away = true,
                            playing = false,
                            event = "HOME"
                        )

                    awayRunnables.remove(
                        sessionId
                    )

                    renderPrioritySession()
                }
            }

        awayRunnables[sessionId] =
            runnable

        mainHandler.postDelayed(
            runnable,
            60_000L
        )
    }

    private fun cancelAwayTimer(
        sessionId: String
    ) {

        awayRunnables[sessionId]
            ?.let {
                mainHandler.removeCallbacks(
                    it
                )
            }

        awayRunnables.remove(
            sessionId
        )
    }

    private fun renderPrioritySession() {

        val priority =
            prioritySession()

        if (priority == null) {

            lastRenderedSessionId = null
            lastRenderedEventKey = null

            discordBridge.clear()

            return
        }

        val profileId =
            priority.profileId

        /**
         * If we know the profile, check its individual RPC setting.
         *
         * If the profile is unknown, fall back to the global switch.
         */
        val rpcAllowed =
            isRpcAllowed(
                profileId
            )

        if (!rpcAllowed) {

            lastRenderedSessionId =
                priority.sessionId

            lastRenderedEventKey =
                "DISABLED"

            discordBridge.clear()

            return
        }

        val eventKey =
            buildEventKey(
                priority
            )

        /**
         * Don't repeatedly send identical RPC updates.
         */
        if (
            lastRenderedSessionId ==
                priority.sessionId &&
            lastRenderedEventKey ==
                eventKey
        ) {
            return
        }

        lastRenderedSessionId =
            priority.sessionId

        lastRenderedEventKey =
            eventKey

        when {

            priority.playing &&
                    !priority.title.isNullOrBlank() -> {

                discordBridge.setWatching(
                    priority.title
                )
            }

            priority.event == "HOME" -> {

                /**
                 * The session is still inside the 60-second
                 * grace period.
                 *
                 * We keep the Discord presence for the moment.
                 */
                if (priority.away) {

                    discordBridge.setBrowsing()

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

        val globalConfig =
            config.load()

        if (!globalConfig.discordEnabled) {
            return false
        }

        /**
         * The existing project stores profiles separately.
         *
         * ProfileStore is intentionally loaded lazily here so
         * PresenceController's constructor stays compatible.
         */
        val profileIdValue =
            profileId
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }

        if (profileIdValue == null) {
            return true
        }

        /**
         * We cannot construct ProfileStore without a Context.
         * Therefore the profile-specific setting will be handled
         * by the integration layer once profiles are connected.
         *
         * For now global Discord enablement remains authoritative.
         */
        return true
    }

    private fun buildEventKey(
        state: PresenceSessionState
    ): String {

        return listOf(
            state.sessionId,
            state.profileId ?: "",
            state.event,
            state.playing.toString(),
            state.title ?: "",
            state.positionSeconds?.toString() ?: "",
            state.durationSeconds?.toString() ?: ""
        ).joinToString("|")
    }

    private fun sessionToEvent(
        state: PresenceSessionState
    ): PresenceEvent {

        return PresenceEvent(
            event = state.event,
            sessionId = state.sessionId,
            profileId = state.profileId,
            catalogId = state.catalogId,
            catalogName = state.catalogName,
            title = state.title,
            mediaType = state.mediaType,
            season = state.season,
            episode = state.episode,
            progress = state.progress,
            positionSeconds = state.positionSeconds,
            durationSeconds = state.durationSeconds,
            artwork = state.artwork,
            avatar = state.avatar,
            username = state.username,
            startTimestampMs = state.startTimestampMs
        )
    }
}
