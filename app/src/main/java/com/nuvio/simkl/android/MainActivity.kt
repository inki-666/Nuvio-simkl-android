package com.nuvio.simkl.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var store: ConfigStore
    private lateinit var port: EditText
    private lateinit var simkl: EditText
    private lateinit var tmdb: EditText
    private lateinit var tvdb: EditText
    private lateinit var discordApp: EditText
    private lateinit var username: EditText
    private lateinit var avatar: EditText
    private lateinit var discordEnabled: CheckBox
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ConfigStore(this)
        requestNotificationPermission()
        buildUi()
    }

    private fun field(label: String, value: String): EditText {
        val box = EditText(this)
        box.hint = label
        box.setText(value)
        box.setPadding(24, 16, 24, 16)
        return box
    }

    private fun buildUi() {
        val c = store.load()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        val title = TextView(this).apply {
            text = "Nuvio Simkl"
            textSize = 28f
            setPadding(0, 0, 0, 12)
        }
        root.addView(title)
        root.addView(TextView(this).apply {
            text = "Local Android service • no Termux required"
            textSize = 15f
        })

        port = field("Local port", c.port.toString()); root.addView(port)
        simkl = field("Simkl Client ID", c.simklClientId); root.addView(simkl)
        tmdb = field("TMDB API key", c.tmdbApiKey); root.addView(tmdb)
        tvdb = field("TVDB API key", c.tvdbApiKey); root.addView(tvdb)
        discordApp = field("Discord Application ID", if (c.discordApplicationId == 0L) "" else c.discordApplicationId.toString()); root.addView(discordApp)
        username = field("Nuvio username", c.profileUsername); root.addView(username)
        avatar = field("Nuvio avatar URL", c.profileAvatarUrl); root.addView(avatar)

        discordEnabled = CheckBox(this).apply {
            text = "Enable Discord Rich Presence"
            isChecked = c.discordEnabled
        }
        root.addView(discordEnabled)

        val save = Button(this).apply { text = "Save & Start" }
        save.setOnClickListener {
            val cfg = AppConfig(
                port = port.text.toString().toIntOrNull() ?: 7000,
                simklClientId = simkl.text.toString().trim(),
                tmdbApiKey = tmdb.text.toString().trim(),
                tvdbApiKey = tvdb.text.toString().trim(),
                discordApplicationId = discordApp.text.toString().trim().toLongOrNull() ?: 0L,
                discordEnabled = discordEnabled.isChecked,
                profileUsername = username.text.toString().trim(),
                profileAvatarUrl = avatar.text.toString().trim()
            )
            store.save(cfg)
            stopService(Intent(this, NuvioService::class.java))
            ContextCompat.startForegroundService(this, Intent(this, NuvioService::class.java))
            status.text = "Running at http://127.0.0.1:${cfg.port}"
        }
        root.addView(save)

        val stop = Button(this).apply { text = "Stop Service" }
        stop.setOnClickListener {
            stopService(Intent(this, NuvioService::class.java))
            status.text = "Stopped"
        }
        root.addView(stop)

        status = TextView(this).apply {
            text = "Not started"
            textSize = 16f
            setPadding(0, 24, 0, 0)
        }
        root.addView(status)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }
}
