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
 */
data class PresenceSessionState(
    val sessionId: String,
    val profileId: String? = null,
    val active: Boolean = true,
    val away: Boolean = false,
    val playing: Boolean = false,
    val title: String? = null,
    val event: String = "BROWSING",
    val catalogId: String? = null,
    val catalogName: String? = null,
    val mediaType: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val progress: Int? = null,
    val positionSeconds: Long? = null,
    val durationSeconds: Long? = null,
    val artwork: String? = null,
    val avatar: String? = null,
    val username: String? = null,
    val startTimestampMs: Long? = null,
    val priorityOrder: Long
)
