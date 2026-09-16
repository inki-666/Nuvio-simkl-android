package com.nuvio.simkl.android

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var config: ConfigStore
    private lateinit var discord: DiscordBridge
    private lateinit var presence: PresenceController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = ConfigStore(this)
        discord = DiscordBridge(config)
        presence = PresenceController(config, discord)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Nuvio SIMKL Android"
            textSize = 24f
        }

        val status = TextView(this).apply {
            text = "Presence controller ready"
            textSize = 16f
        }

        root.addView(title)
        root.addView(status)

        setContentView(root)
    }

    override fun onDestroy() {
        if (::presence.isInitialized) {
            presence.clear()
        }

        super.onDestroy()
    }
}
