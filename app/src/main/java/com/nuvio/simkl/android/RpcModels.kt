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
