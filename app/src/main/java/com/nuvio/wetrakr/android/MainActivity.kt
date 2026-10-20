package com.nuvio.wetrakr.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var store: ConfigStore

    private lateinit var port: EditText
    private lateinit var tmdb: EditText
    private lateinit var tvdb: EditText
    private lateinit var discordApp: EditText
    private lateinit var username: EditText
    private lateinit var avatar: EditText
    private lateinit var discordEnabled: CheckBox
    private lateinit var status: TextView

    private val isTelevision: Boolean
        get() = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ConfigStore(this)
        requestNotificationPermission()
        buildUi()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun field(
        label: String,
        value: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT
    ): EditText {
        return EditText(this).apply {
            hint = label
            setText(value)
            this.inputType = inputType
            textSize = if (isTelevision) 18f else 16f
            minHeight = dp(if (isTelevision) 58 else 48)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            isFocusable = true
            isFocusableInTouchMode = true
            contentDescription = label
        }
    }

    private fun label(text: String, size: Float = 16f): TextView =
        TextView(this).apply {
            this.text = text
            textSize = if (isTelevision) size + 2f else size
            setPadding(0, dp(12), 0, dp(6))
        }

    private fun buildUi() {
        val config = store.load()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(if (isTelevision) 36 else 20),
                dp(if (isTelevision) 28 else 20),
                dp(if (isTelevision) 36 else 20),
                dp(if (isTelevision) 36 else 20)
            )
        }

        content.addView(TextView(this).apply {
            text = getString(com.nuvio.wetrakr.android.R.string.app_name)
            textSize = if (isTelevision) 34f else 28f
            setPadding(0, 0, 0, dp(8))
            contentDescription = "Nuvio WeTrakr settings"
        })

        content.addView(label(
            "Nuvio companion service • Discord Rich Presence",
            16f
        ))

        content.addView(label(
            "Local service",
            18f
        ))
        port = field(
            "Local HTTP port (default 7000)",
            config.port.toString(),
            InputType.TYPE_CLASS_NUMBER
        )
        content.addView(port)

        content.addView(label("Metadata resolver keys", 18f))
        content.addView(TextView(this).apply {
            text = "Metadata lookup is not enabled in this build. These fields are retained for the later provider integration."
            textSize = if (isTelevision) 16f else 14f
            setPadding(0, 0, 0, dp(8))
        })

        tmdb = field("TMDB API key (optional)", config.tmdbApiKey)
        content.addView(tmdb)

        tvdb = field("TVDB API key (optional)", config.tvdbApiKey)
        content.addView(tvdb)

        content.addView(label("Discord Rich Presence", 18f))
        discordApp = field(
            "Discord Application ID",
            if (config.discordApplicationId == 0L) "" else config.discordApplicationId.toString(),
            InputType.TYPE_CLASS_NUMBER
        )
        content.addView(discordApp)

        username = field("Nuvio username (optional)", config.profileUsername)
        content.addView(username)

        avatar = field("Nuvio avatar URL (optional)", config.profileAvatarUrl)
        content.addView(avatar)

        discordEnabled = CheckBox(this).apply {
            text = "Enable Discord Rich Presence"
            textSize = if (isTelevision) 18f else 16f
            isChecked = config.discordEnabled
            isFocusable = true
            isFocusableInTouchMode = true
            minHeight = dp(if (isTelevision) 56 else 48)
        }
        content.addView(discordEnabled)

        content.addView(TextView(this).apply {
            text = "Tracker provider: not connected yet. WeTrakr API integration is intentionally deferred until the final stage."
            textSize = if (isTelevision) 16f else 14f
            setPadding(0, dp(12), 0, dp(8))
        })

        status = TextView(this).apply {
            text = "Service status: not started"
            textSize = if (isTelevision) 18f else 16f
            setPadding(0, dp(18), 0, dp(8))
            isFocusable = true
        }

        val save = Button(this).apply {
            text = "Save & Start Service"
            minHeight = dp(if (isTelevision) 60 else 48)
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener {
                val portValue = port.text.toString().toIntOrNull()
                if (portValue == null || portValue !in 1024..65535) {
                    port.error = "Enter a port from 1024 to 65535"
                    port.requestFocus()
                    return@setOnClickListener
                }

                val appIdText = discordApp.text.toString().trim()
                val appId = if (appIdText.isBlank()) {
                    0L
                } else {
                    appIdText.toLongOrNull()
                }

                if (appId == null) {
                    discordApp.error = "Enter a numeric Discord Application ID"
                    discordApp.requestFocus()
                    return@setOnClickListener
                }

                // WeTrakr API integration is intentionally deferred.
                store.save(
                    AppConfig(
                        port = portValue,
                        tmdbApiKey = tmdb.text.toString().trim(),
                        tvdbApiKey = tvdb.text.toString().trim(),
                        discordApplicationId = appId,
                        discordEnabled = discordEnabled.isChecked,
                        profileUsername = username.text.toString().trim(),
                        profileAvatarUrl = avatar.text.toString().trim()
                    )
                )

                stopService(Intent(this, NuvioService::class.java))
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, NuvioService::class.java)
                )
                status.text = "Service status: starting on port $portValue"
                Toast.makeText(
                    this,
                    "Configuration saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        content.addView(save)

        val stop = Button(this).apply {
            text = "Stop Service"
            minHeight = dp(if (isTelevision) 60 else 48)
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener {
                stopService(Intent(this@MainActivity, NuvioService::class.java))
                status.text = "Service status: stopped"
            }
        }
        content.addView(stop)
        content.addView(status)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isFocusable = false
            isFocusableInTouchMode = false
            addView(
                content,
                ScrollView.LayoutParams(
                    if (isTelevision) dp(760) else LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val frame = FrameLayout(this)
        frame.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(frame)

        if (isTelevision) {
            save.post { save.requestFocus() }
        }
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                42
            )
        }
    }
}
