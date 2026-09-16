package com.nuvio.simkl.android

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class ConnectionManager(
    private val onEvent: (NuvioEvent) -> Unit
) {

    companion object {
        private const val TAG = "ConnectionManager"
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(
            ConnectionConfig.HEARTBEAT_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
        .connectTimeout(
            ConnectionConfig.CONNECTION_TIMEOUT_MS,
            TimeUnit.MILLISECONDS
        )
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val deviceId: String =
        UUID.randomUUID().toString()

    private var socket: WebSocket? = null

    private var usingCloud = false

    private var stopped = false

    private var lastMessageAt = 0L

    fun start() {
        stopped = false
        connectLocal()
    }

    fun stop() {
        stopped = true

        socket?.close(
            1000,
            "Client stopped"
        )

        socket = null
    }

    private fun connectLocal() {

        if (stopped) return

        usingCloud = false

        connect(
            ConnectionConfig.LOCAL_WS_URL
        )
    }

    private fun connectCloud() {

        if (stopped) return

        usingCloud = true

        connect(
            ConnectionConfig.CLOUD_WS_URL
        )
    }

    private fun connect(url: String) {

        if (url.isBlank()) {
            scheduleReconnect()
            return
        }

        Log.d(
            TAG,
            "Connecting: $url"
        )

        val request = Request.Builder()
            .url(url)
            .build()

        socket?.cancel()

        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {

                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {
                    Log.d(
                        TAG,
                        "Connected: $url"
                    )

                    lastMessageAt =
                        System.currentTimeMillis()

                    sendAuthentication(
                        webSocket
                    )
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {

                    lastMessageAt =
                        System.currentTimeMillis()

                    handleMessage(text)
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {

                    Log.d(
                        TAG,
                        "Closing: $code $reason"
                    )
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {

                    Log.d(
                        TAG,
                        "Closed: $code $reason"
                    )

                    handleDisconnect()
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {

                    Log.e(
                        TAG,
                        "Connection failed",
                        t
                    )

                    handleDisconnect()
                }
            }
        )
    }

    private fun sendAuthentication(
        webSocket: WebSocket
    ) {

        val json = JSONObject()

        json.put(
            "type",
            "auth"
        )

        json.put(
            "token",
            ConnectionConfig.PAIRING_TOKEN
        )

        json.put(
            "deviceId",
            deviceId
        )

        webSocket.send(
            json.toString()
        )
    }

    private fun handleMessage(
        text: String
    ) {

        try {

            val json =
                JSONObject(text)

            when (
                json.optString("type")
            ) {

                "profile_changed" -> {

                    val profileId =
                        json.optString(
                            "profileId"
                        )

                    if (
                        profileId.isNotBlank()
                    ) {

                        onEvent(
                            NuvioEvent.ProfileChanged(
                                profileId
                            )
                        )
                    }
                }

                "playback_started" -> {

                    val profileId =
                        json.optString(
                            "profileId"
                        )

                    val title =
                        json.optString(
                            "title"
                        )

                    if (
                        profileId.isNotBlank() &&
                        title.isNotBlank()
                    ) {

                        onEvent(
                            NuvioEvent.PlaybackStarted(
                                profileId,
                                title
                            )
                        )
                    }
                }

                "playback_stopped" -> {

                    val profileId =
                        json.optString(
                            "profileId"
                        )

                    if (
                        profileId.isNotBlank()
                    ) {

                        onEvent(
                            NuvioEvent.PlaybackStopped(
                                profileId
                            )
                        )
                    }
                }

                "nuvio_activity" -> {

                    val active =
                        json.optBoolean(
                            "active",
                            false
                        )

                    onEvent(
                        NuvioEvent.ActivityChanged(
                            active
                        )
                    )
                }

                "ping" -> {

                    socket?.send(
                        JSONObject()
                            .put(
                                "type",
                                "pong"
                            )
                            .toString()
                    )
                }
            }

        } catch (
            exception: Exception
        ) {

            Log.e(
                TAG,
                "Invalid event",
                exception
            )
        }
    }

    private fun handleDisconnect() {

        socket = null

        if (stopped) {
            return
        }

        /*
         * Local connection failed:
         * immediately try cloud.
         */
        if (!usingCloud) {

            connectCloud()

            return
        }

        /*
         * Cloud also failed:
         * retry local first.
         */
        scheduleReconnect()
    }

    private fun scheduleReconnect() {

        if (stopped) return

        Thread {

            try {

                Thread.sleep(
                    ConnectionConfig.RECONNECT_DELAY_MS
                )

            } catch (
                interrupted: InterruptedException
            ) {

                Thread.currentThread()
                    .interrupt()

                return@Thread
            }

            if (!stopped) {
                connectLocal()
            }

        }.start()
    }
}
