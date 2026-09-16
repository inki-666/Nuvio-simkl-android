package com.nuvio.integration

/** Shared event contract for the Nuvio app -> Nuvio Simkl Android bridge. */
data class NuvioPresenceEvent(
    val event: String,
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
    val username: String? = null,
    val avatar: String? = null,
    val startTimestampMs: Long? = null,
)
