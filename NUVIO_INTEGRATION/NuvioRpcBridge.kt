package com.nuvio.tv.rpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.json.JSONObject

class NuvioRpcBridge(
    private val sendMessage: (String) -> Unit,
    private val scope: CoroutineScope
) {

    fun start(
        activeProfileIdFlow: kotlinx.coroutines.flow.Flow<String?>
    ) {

        scope.launch(Dispatchers.Main) {

            activeProfileIdFlow
                .distinctUntilChanged()
                .collect { profileId ->

                    if (
                        profileId.isNullOrBlank()
                    ) {
                        return@collect
                    }

                    send(
                        JSONObject()
                            .put(
                                "type",
                                "profile_changed"
                            )
                            .put(
                                "profileId",
                                profileId
                            )
                    )
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
                    "profile_changed"
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
                    "playback_started"
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

    fun playbackStopped(
        profileId: String
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "playback_stopped"
                )
                .put(
                    "profileId",
                    profileId
                )
        )
    }

    fun activityChanged(
        active: Boolean
    ) {

        send(
            JSONObject()
                .put(
                    "type",
                    "nuvio_activity"
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
