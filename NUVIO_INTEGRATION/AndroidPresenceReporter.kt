package com.nuvio.integration

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android-side reporter to be called by Nuvio's actual UI/playback code.
 * This reports explicit state; it does not inspect pixels, accessibility, or screen contents.
 */
class AndroidPresenceReporter(private val baseUrl: String = "http://127.0.0.1:7000") {
    fun report(e: NuvioPresenceEvent) {
        val body = JSONObject().apply {
            put("event", e.event)
            e.catalogId?.let { put("catalogId", it) }
            e.catalogName?.let { put("catalogName", it) }
            e.title?.let { put("title", it) }
            e.mediaType?.let { put("mediaType", it) }
            e.season?.let { put("season", it) }
            e.episode?.let { put("episode", it) }
            e.progress?.let { put("progress", it) }
            e.positionSeconds?.let { put("positionSeconds", it) }
            e.durationSeconds?.let { put("durationSeconds", it) }
            e.artwork?.let { put("artwork", it) }
            e.username?.let { put("username", it) }
            e.avatar?.let { put("avatar", it) }
            e.startTimestampMs?.let { put("startTimestampMs", it) }
        }.toString()
        Thread {
            runCatching {
                val c = URL("$baseUrl/presence").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 1500
                c.readTimeout = 1500
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                c.inputStream.close()
                c.disconnect()
            }
        }.start()
    }

    fun clear() {
        Thread {
            runCatching { (URL("$baseUrl/presence/clear").openConnection() as HttpURLConnection).apply { connectTimeout = 1500; readTimeout = 1500 }.inputStream.close() }
        }.start()
    }
}
