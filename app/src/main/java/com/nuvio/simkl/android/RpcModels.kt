package com.nuvio.simkl.android

data class NuvioProfile(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val rpcEnabled: Boolean = true
)

data class RpcState(
    val profileId: String? = null,
    val nuvioActive: Boolean = false,
    val playing: Boolean = false,
    val title: String? = null
)

sealed class NuvioEvent {

    data class ProfileChanged(
        val profileId: String
    ) : NuvioEvent()

    data class PlaybackStarted(
        val profileId: String,
        val title: String
    ) : NuvioEvent()

    data class PlaybackStopped(
        val profileId: String
    ) : NuvioEvent()

    data class ActivityChanged(
        val active: Boolean
    ) : NuvioEvent()
}

/**
 * Snapshot of one independently tracked Nuvio session.
 *
 * priorityOrder is assigned when the session first becomes active.
 * Lower numbers have higher Discord priority.
 *
 * A session can be:
 *
 * PLAYING
 * PAUSED
 * BROWSING
 * BROWSING_METADATA
 * HOME
 *
 * HOME is temporary. The session remains active during its
 * 60-second grace period before becoming inactive.
 */
data class PresenceSessionState(
    val sessionId: String,

    val profileId: String? = null,

    /**
     * Whether this session is currently considered active.
     *
     * HOME keeps this true during the 60-second grace period.
     * After the timer expires, this becomes false.
     */
    val active: Boolean = true,

    /**
     * True while Nuvio is temporarily on its Home screen.
     */
    val away: Boolean = false,

    /**
     * True only while media is actively playing.
     */
    val playing: Boolean = false,

    /**
     * True while media is paused.
     *
     * playing and paused are mutually exclusive.
     */
    val paused: Boolean = false,

    /**
     * Current title being watched or browsed.
     */
    val title: String? = null,

    /**
     * Current logical presence event.
     *
     * Expected values include:
     *
     * PLAYING
     * PAUSED
     * BROWSING
     * BROWSING_METADATA
     * HOME
     */
    val event: String = "BROWSING",

    /**
     * Stable catalogue identity.
     */
    val catalogId: String? = null,

    val catalogName: String? = null,

    /**
     * Movie / series / anime / etc.
     */
    val mediaType: String? = null,

    /**
     * Episode metadata.
     */
    val season: Int? = null,

    val episode: Int? = null,

    /**
     * Playback progress percentage.
     */
    val progress: Int? = null,

    /**
     * Current playback position.
     */
    val positionSeconds: Long? = null,

    /**
     * Total media duration.
     */
    val durationSeconds: Long? = null,

    /**
     * Artwork associated with the current title.
     */
    val artwork: String? = null,

    /**
     * Nuvio profile avatar.
     */
    val avatar: String? = null,

    /**
     * Nuvio profile username.
     */
    val username: String? = null,

    /**
     * Timestamp used by Discord for elapsed/remaining playback.
     */
    val startTimestampMs: Long? = null,

    /**
     * Determines which session gets Discord priority.
     *
     * Lower number = higher priority.
     *
     * The value is preserved when a session changes profile,
     * pauses, resumes, browses metadata, or temporarily goes Home.
     */
    val priorityOrder: Long
)
