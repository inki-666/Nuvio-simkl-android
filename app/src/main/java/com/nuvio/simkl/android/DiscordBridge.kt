package com.nuvio.simkl.android

class DiscordBridge(
    private val config: ConfigStore? = null
) {

    fun setWatching(title: String) {
        // TODO:
        // Connect the official Discord Social SDK here.
        //
        // Expected Discord state:
        // Watching <title>
    }

    fun setPaused(title: String) {
        // TODO:
        // Connect the official Discord Social SDK here.
        //
        // Expected Discord state:
        // Paused <title>
    }

    fun setBrowsing() {
        // TODO:
        // Connect the official Discord Social SDK here.
        //
        // Expected Discord state:
        // Browsing Nuvio
    }

    fun setBrowsingMetadata(title: String) {
        // TODO:
        // Connect the official Discord Social SDK here.
        //
        // Expected Discord state:
        // Browsing <title>
    }

    fun clear() {
        // TODO:
        // Connect the official Discord Social SDK here.
        //
        // Expected behavior:
        // Completely clear the Discord Rich Presence.
    }
}
