package com.nuvio.simkl.android

import android.os.Handler
import android.os.Looper

class PresenceController(
    private val profilesProvider: () -> List<NuvioProfile>,
    private val discordBridge: DiscordBridge
) {

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var currentProfileId: String? = null

    private var nuvioActive = false

    private var playing = false

    private var currentTitle: String? = null

    private var awayRunnable: Runnable? = null

    fun handleEvent(
        event: NuvioEvent
    ) {

        when (event) {

            is NuvioEvent.ProfileChanged -> {

                handleProfileChanged(
                    event.profileId
                )
            }

            is NuvioEvent.PlaybackStarted -> {

                handlePlaybackStarted(
                    event.profileId,
                    event.title
                )
            }

            is NuvioEvent.PlaybackStopped -> {

                handlePlaybackStopped(
                    event.profileId
                )
            }

            is NuvioEvent.ActivityChanged -> {

                handleActivityChanged(
                    event.active
                )
            }
        }
    }

    private fun handleProfileChanged(
        profileId: String
    ) {

        /*
         * Profile switch is NOT the same thing
         * as pressing Home.
         *
         * Old profile is immediately stopped.
         */
        cancelAwayTimer()

        currentProfileId =
            profileId

        playing = false

        currentTitle = null

        nuvioActive = true

        render()
    }

    private fun handlePlaybackStarted(
        profileId: String,
        title: String
    ) {

        if (
            currentProfileId != null &&
            currentProfileId != profileId
        ) {
            return
        }

        currentProfileId =
            profileId

        playing = true

        currentTitle =
            title

        nuvioActive = true

        cancelAwayTimer()

        render()
    }

    private fun handlePlaybackStopped(
        profileId: String
    ) {

        if (
            currentProfileId != null &&
            currentProfileId != profileId
        ) {
            return
        }

        playing = false

        currentTitle = null

        /*
         * IMPORTANT:
         *
         * Stopping playback does NOT mean
         * Nuvio was closed.
         *
         * We remain in browsing state.
         */
        nuvioActive = true

        render()
    }

    private fun handleActivityChanged(
        active: Boolean
    ) {

        if (active) {

            cancelAwayTimer()

            nuvioActive = true

            render()

        } else {

            /*
             * Home is separate from profile switching.
             *
             * Give the user 60 seconds to return.
             */
            startAwayTimer()
        }
    }

    private fun startAwayTimer() {

        cancelAwayTimer()

        val runnable =
            Runnable {

                nuvioActive = false

                playing = false

                currentTitle = null

                discordBridge.clear()
            }

        awayRunnable =
            runnable

        mainHandler.postDelayed(
            runnable,
            60_000L
        )
    }

    private fun cancelAwayTimer() {

        awayRunnable?.let {

            mainHandler.removeCallbacks(
                it
            )
        }

        awayRunnable = null
    }

    private fun render() {

        val profile =
            profilesProvider()
                .firstOrNull {
                    it.id == currentProfileId
                }

        if (profile == null) {

            discordBridge.clear()

            return
        }

        /*
         * Profile-level RPC switch.
         */
        if (!profile.rpcEnabled) {

            discordBridge.clear()

            return
        }

        /*
         * Nuvio is actually inactive.
         */
        if (!nuvioActive) {

            discordBridge.clear()

            return
        }

        /*
         * Watching.
         */
        if (
            playing &&
            !currentTitle.isNullOrBlank()
        ) {

            discordBridge.setWatching(
                currentTitle!!
            )

            return
        }

        /*
         * Browsing.
         */
        discordBridge.setBrowsing()
    }
}
