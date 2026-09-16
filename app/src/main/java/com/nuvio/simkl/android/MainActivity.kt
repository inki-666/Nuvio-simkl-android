package com.nuvio.simkl.android

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var profileStore: ProfileStore

    private lateinit var presenceController:
            PresenceController

    private lateinit var connectionManager:
            ConnectionManager

    private lateinit var statusText:
            TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        profileStore =
            ProfileStore(this)

        val discordBridge =
            DiscordBridge()

        presenceController =
            PresenceController(
                profilesProvider = {
                    profileStore.loadProfiles()
                },
                discordBridge =
                    discordBridge
            )

        connectionManager =
            ConnectionManager {

                event ->

                runOnUiThread {

                    presenceController
                        .handleEvent(event)

                    updateStatus(event)
                }
            }

        createUi()

        connectionManager.start()
    }

    private fun createUi() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            40,
            40,
            40,
            40
        )

        val title =
            TextView(this)

        title.text =
            "Nuvio Discord RPC"

        title.textSize =
            24f

        root.addView(
            title
        )

        statusText =
            TextView(this)

        statusText.text =
            "Connecting..."

        statusText.textSize =
            16f

        root.addView(
            statusText
        )

        val profileCount =
            TextView(this)

        val profiles =
            profileStore.loadProfiles()

        profileCount.text =
            "RPC profiles: ${profiles.count { it.rpcEnabled }} / ${profiles.size}"

        root.addView(
            profileCount
        )

        val reconnectButton =
            Button(this)

        reconnectButton.text =
            "Reconnect"

        reconnectButton.setOnClickListener {

            connectionManager.stop()

            connectionManager.start()

            statusText.text =
                "Reconnecting..."
        }

        root.addView(
            reconnectButton
        )

        setContentView(
            root
        )
    }

    private fun updateStatus(
        event: NuvioEvent
    ) {

        statusText.text =
            when (event) {

                is NuvioEvent.ProfileChanged ->
                    "Profile: ${event.profileId}"

                is NuvioEvent.PlaybackStarted ->
                    "Watching: ${event.title}"

                is NuvioEvent.PlaybackStopped ->
                    "Browsing Nuvio"

                is NuvioEvent.ActivityChanged ->
                    if (event.active) {
                        "Nuvio active"
                    } else {
                        "Home — waiting 60 seconds"
                    }
            }
    }

    override fun onDestroy() {

        connectionManager.stop()

        super.onDestroy()
    }
}
