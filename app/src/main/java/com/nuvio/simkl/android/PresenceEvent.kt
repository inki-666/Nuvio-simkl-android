package com.nuvio.simkl.android

/**
 * Events emitted by Nuvio itself.
 *
 * sessionId:
 * Identifies one running Nuvio session/device.
 *
 * profileId:
 * Identifies the Nuvio profile currently using that session.
 *
 * The companion never tries to infer Nuvio state from screenshots,
 * accessibility nodes, or visible UI.
 */
data class PresenceEvent(
    val event: String,

    val sessionId: String? = null,

    val profileId: String? = null,

    val catalogId: String? = null,

    val catalogName: String? = null,

    val title: String? = null,

    val mediaType: String? = null,

    val season: Int? = null,

    val episode: Int? = null,

    val progress: Int? = null,

    val positionSeconds: Long? = null,

    val durationSeconds: Long? = null,

    val artwork: String? = null,

    val avatar: String? = null,

    val username: String? = null,

    val startTimestampMs: Long? = null
)
