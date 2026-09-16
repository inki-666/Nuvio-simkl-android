package com.nuvio.simkl.android

object ConnectionConfig {

    /*
     * LOCAL:
     * Replace the IP with the machine/device running
     * the local Nuvio RPC bridge.
     */
    const val LOCAL_WS_URL = "ws://192.168.1.100:8765"

    /*
     * CLOUD:
     * After deploying Cloud Run, put its URL here.
     *
     * Example:
     * wss://nuvio-rpc-relay-xxxxx-uc.a.run.app
     */
    const val CLOUD_WS_URL = "wss://PASTE-CLOUD-RUN-URL-HERE"

    /*
     * DO NOT commit your real token to a public GitHub repository.
     *
     * This is only a temporary placeholder.
     */
    const val PAIRING_TOKEN = "PASTE-PAIRING-TOKEN-HERE"

    const val HEARTBEAT_INTERVAL_MS = 15_000L

    const val CONNECTION_TIMEOUT_MS = 10_000L

    const val RECONNECT_DELAY_MS = 3_000L
}
