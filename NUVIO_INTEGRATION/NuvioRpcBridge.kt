package com.nuvio.tv.rpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.json.JSONObject

class NuvioRpcBridge(
    private val sendMessage: (String) -> Unit,
    private val scope: CoroutineScope
) {

    fun start(
        activeProfileIdFlow: Flow<String?>
    ) {

        scope.launch(Dispatchers.Main) {

            activeProfileIdFlow
                .distinctUntilChanged()
                .collect { profileId ->

                    if (profileId.isNullOrBlank()) {
                        return@collect
                    }

                    profileChanged(profileId)
                }
        }
    }

    fun profileChanged(
        profileId: String
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "PROFILE_CHANGED"
                )
                .put(
                    "profileId",
                    profileId
                )
        )
    }

    fun playbackStarted(
        profileId: String,
        title: String
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "PLAYING"
                )
                .put(
                    "profileId",
                    profileId
                )
                .put(
                    "title",
                    title
                )
        )
    }

    fun playbackPaused(
        profileId: String,
        title: String? = null,
        positionSeconds: Long? = null,
        durationSeconds: Long? = null
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "PAUSED"
                )
                .put(
                    "profileId",
                    profileId
                )
                .apply {

                    title?.let {
                        put(
                            "title",
                            it
                        )
                    }

                    positionSeconds?.let {
                        put(
                            "positionSeconds",
                            it
                        )
                    }

                    durationSeconds?.let {
                        put(
                            "durationSeconds",
                            it
                        )
                    }
                }
        )
    }

    fun playbackStopped(
        profileId: String
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "STOPPED"
                )
                .put(
                    "profileId",
                    profileId
                )
        )
    }

    fun browsing(
        profileId: String,
        title: String? = null
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "BROWSING"
                )
                .put(
                    "profileId",
                    profileId
                )
                .apply {

                    title?.let {
                        put(
                            "title",
                            it
                        )
                    }
                }
        )
    }

    fun browsingMetadata(
        profileId: String,
        title: String,
        catalogId: String? = null,
        catalogName: String? = null,
        mediaType: String? = null,
        season: Int? = null,
        episode: Int? = null,
        artwork: String? = null
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "BROWSING_METADATA"
                )
                .put(
                    "profileId",
                    profileId
                )
                .put(
                    "title",
                    title
                )
                .apply {

                    catalogId?.let {
                        put(
                            "catalogId",
                            it
                        )
                    }

                    catalogName?.let {
                        put(
                            "catalogName",
                            it
                        )
                    }

                    mediaType?.let {
                        put(
                            "mediaType",
                            it
                        )
                    }

                    season?.let {
                        put(
                            "season",
                            it
                        )
                    }

                    episode?.let {
                        put(
                            "episode",
                            it
                        )
                    }

                    artwork?.let {
                        put(
                            "artwork",
                            it
                        )
                    }
                }
        )
    }

    fun home(
        profileId: String
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "HOME"
                )
                .put(
                    "profileId",
                    profileId
                )
        )
    }

    fun sessionEnded(
        profileId: String? = null,
        sessionId: String? = null
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "SESSION_ENDED"
                )
                .apply {

                    profileId?.let {
                        put(
                            "profileId",
                            it
                        )
                    }

                    sessionId?.let {
                        put(
                            "sessionId",
                            it
                        )
                    }
                }
        )
    }

    fun activityChanged(
        active: Boolean
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    if (active) {
                        "BROWSING"
                    } else {
                        "SESSION_ENDED"
                    }
                )
                .put(
                    "active",
                    active
                )
        )
    }

    private fun send(
        objectJson: JSONObject
    ) {

        sendMessage(
            objectJson.toString()
        )
    }
}
