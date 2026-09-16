package com.nuvio.simkl.android

import android.content.Context

data class AppConfig(
    val port: Int = 7000,
    val simklClientId: String = "",
    val tmdbApiKey: String = "",
    val tvdbApiKey: String = "",
    val discordApplicationId: Long = 0L,
    val discordEnabled: Boolean = false,
    val profileUsername: String = "",
    val profileAvatarUrl: String = ""
)

class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("nuvio_simkl", Context.MODE_PRIVATE)

    fun load(): AppConfig = AppConfig(
        port = prefs.getInt("port", 7000),
        simklClientId = prefs.getString("simkl", "") ?: "",
        tmdbApiKey = prefs.getString("tmdb", "") ?: "",
        tvdbApiKey = prefs.getString("tvdb", "") ?: "",
        discordApplicationId = prefs.getLong("discord_app", 0L),
        discordEnabled = prefs.getBoolean("discord_enabled", false),
        profileUsername = prefs.getString("username", "") ?: "",
        profileAvatarUrl = prefs.getString("avatar", "") ?: ""
    )

    fun save(c: AppConfig) {
        prefs.edit()
            .putInt("port", c.port)
            .putString("simkl", c.simklClientId)
            .putString("tmdb", c.tmdbApiKey)
            .putString("tvdb", c.tvdbApiKey)
            .putLong("discord_app", c.discordApplicationId)
            .putBoolean("discord_enabled", c.discordEnabled)
            .putString("username", c.profileUsername)
            .putString("avatar", c.profileAvatarUrl)
            .apply()
    }
}
