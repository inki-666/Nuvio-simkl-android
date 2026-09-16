package com.nuvio.simkl.android

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LocalHttpServer(
    private val port: Int,
    private val config: ConfigStore,
    private val presence: PresenceController
) {
    @Volatile private var running = false
    private var server: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool()

    fun start() {
        if (running) return
        running = true
        server = ServerSocket(port)
        thread(name = "nuvio-http", isDaemon = true) {
            while (running) {
                try {
                    val s = server?.accept() ?: break
                    pool.execute { handle(s) }
                } catch (_: Exception) {
                    if (running) break
                }
            }
        }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        pool.shutdownNow()
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
            val first = reader.readLine() ?: return
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val lower = line.lowercase()
                if (lower.startsWith("content-length:")) contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
            }
            val body = if (contentLength > 0) CharArray(contentLength).also { reader.read(it) }.concatToString() else ""
            val parts = first.split(" ")
            val method = parts.getOrNull(0) ?: "GET"
            val rawPath = parts.getOrNull(1) ?: "/"
            val path = rawPath.substringBefore("?")

            val response = when {
                method == "OPTIONS" -> json("{}", 204)
                path == "/health" -> json(JSONObject().put("ok", true).put("service", "nuvio-simkl-android").put("discordConfigured", isDiscordConfigured()).toString())
                path == "/presence" && method == "POST" -> handlePresenceJson(body)
                path == "/presence" && method == "GET" -> handlePresenceQuery(rawPath)
                path == "/presence/state" -> currentPresence()
                path == "/presence/clear" -> { presence.clear(); json("{\"ok\":true}") }
                path == "/manifest.json" -> json(manifest())
                path.startsWith("/meta/") -> json("{\"ok\":false,\"error\":\"Android companion does not fake metadata. Connect the verified Simkl/provider resolver before enabling meta locally.\"}", 501)
                else -> json("{\"ok\":true,\"service\":\"Nuvio Simkl Android\",\"endpoints\":[\"POST /presence\",\"GET /presence\",\"GET /presence/state\",\"GET /presence/clear\",\"GET /health\"]}")
            }
            s.getOutputStream().use { out ->
                out.write(response.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            }
        }
    }

    private fun handlePresenceJson(body: String): String {
        return runCatching {
            val o = JSONObject(body.ifBlank { "{}" })
            val event = PresenceEvent(
                event = o.optString("event", "HOME"),
                catalogId = o.optNullableString("catalogId"),
                catalogName = o.optNullableString("catalogName"),
                title = o.optNullableString("title"),
                mediaType = o.optNullableString("mediaType"),
                season = o.optIntOrNull("season"),
                episode = o.optIntOrNull("episode"),
                progress = o.optIntOrNull("progress"),
                positionSeconds = o.optLongOrNull("positionSeconds"),
                durationSeconds = o.optLongOrNull("durationSeconds"),
                artwork = o.optNullableString("artwork"),
                avatar = o.optNullableString("avatar"),
                username = o.optNullableString("username"),
                startTimestampMs = o.optLongOrNull("startTimestampMs")
            )
            val ok = presence.handle(event)
            json(JSONObject().put("ok", ok).put("event", event.event.uppercase()).toString())
        }.getOrElse { json(JSONObject().put("ok", false).put("error", it.message ?: "Invalid JSON").toString(), 400) }
    }

    private fun handlePresenceQuery(rawPath: String): String {
        val query = rawPath.substringAfter("?", "")
        val map = query.split("&").mapNotNull {
            val p = it.split("=", limit = 2)
            if (p.size == 2) URLDecoder.decode(p[0], "UTF-8") to URLDecoder.decode(p[1], "UTF-8") else null
        }.toMap()
        val ok = presence.handle(
            PresenceEvent(
                event = map["event"] ?: "HOME",
                catalogId = map["catalogId"], catalogName = map["catalogName"], title = map["title"],
                mediaType = map["mediaType"], season = map["season"]?.toIntOrNull(), episode = map["episode"]?.toIntOrNull(),
                progress = map["progress"]?.toIntOrNull(), positionSeconds = map["positionSeconds"]?.toLongOrNull(), durationSeconds = map["durationSeconds"]?.toLongOrNull(), artwork = map["artwork"] ?: map["large"],
                avatar = map["avatar"] ?: map["small"], username = map["username"]
            )
        )
        return json("{\"ok\":$ok}")
    }

    private fun currentPresence(): String {
        val e = presence.current() ?: return "{\"active\":false}"
        return JSONObject().put("active", true).put("event", e.event).apply {
            e.catalogId?.let { put("catalogId", it) }; e.catalogName?.let { put("catalogName", it) }
            e.title?.let { put("title", it) }; e.mediaType?.let { put("mediaType", it) }
            e.season?.let { put("season", it) }; e.episode?.let { put("episode", it) }
            e.progress?.let { put("progress", it) }; e.positionSeconds?.let { put("positionSeconds", it) }; e.durationSeconds?.let { put("durationSeconds", it) }; e.artwork?.let { put("artwork", it) }
        }.toString().let { json(it) }
    }

    private fun isDiscordConfigured(): Boolean {
        val c = config.load()
        return c.discordEnabled && c.discordApplicationId != 0L
    }

    private fun manifest(): String = """
        {
          "id":"com.nuvio.simkl.android",
          "version":"1.1.0",
          "name":"Nuvio Simkl Android",
          "description":"Local Nuvio presence bridge; metadata is not fabricated locally.",
          "resources":["meta"],
          "types":["movie","series","anime"],
          "idPrefixes":["simkl:","tmdb:","tvdb:","imdb:","mal:","anilist:","anidb:","kitsu:"],
          "catalogs":[],
          "behaviorHints":{"configurable":true}
        }
    """.trimIndent()

    private fun json(body: String, code: Int = 200): String {
        val reason = when (code) { 204 -> "No Content"; 400 -> "Bad Request"; 501 -> "Not Implemented"; else -> "OK" }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        return "HTTP/1.1 $code $reason\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Methods: GET,POST,OPTIONS\r\nAccess-Control-Allow-Headers: Content-Type\r\nConnection: close\r\n\r\n$body"
    }
}

private fun JSONObject.optNullableString(name: String): String? = optString(name, "").takeIf { it.isNotBlank() }
private fun JSONObject.optIntOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name).takeIf { it != 0 || optString(name) == "0" } else null
private fun JSONObject.optLongOrNull(name: String): Long? = if (has(name) && !isNull(name)) optLong(name).takeIf { it != 0L || optString(name) == "0" } else null
