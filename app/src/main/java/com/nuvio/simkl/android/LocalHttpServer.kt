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
    @Volatile
    private var running = false

    private var server: ServerSocket? = null

    private val pool = Executors.newCachedThreadPool()

    fun start() {
        if (running) return

        running = true
        server = ServerSocket(port)

        thread(
            name = "nuvio-http",
            isDaemon = true
        ) {
            while (running) {
                try {
                    val socket = server?.accept() ?: break
                    pool.execute {
                        handle(socket)
                    }
                } catch (_: Exception) {
                    if (running) {
                        // Keep server loop alive if possible.
                    }
                }
            }
        }
    }

    fun stop() {
        running = false

        runCatching {
            server?.close()
        }

        server = null

        pool.shutdownNow()
    }

    private fun handle(socket: Socket) {
        socket.use { s ->

            val reader = BufferedReader(
                InputStreamReader(
                    s.getInputStream(),
                    StandardCharsets.UTF_8
                )
            )

            val firstLine = reader.readLine() ?: return

            var contentLength = 0

            while (true) {
                val line = reader.readLine() ?: break

                if (line.isEmpty()) {
                    break
                }

                val lower = line.lowercase()

                if (lower.startsWith("content-length:")) {
                    contentLength =
                        line
                            .substringAfter(":")
                            .trim()
                            .toIntOrNull()
                            ?: 0
                }
            }

            val body =
                if (contentLength > 0) {
                    CharArray(contentLength)
                        .also { reader.read(it) }
                        .concatToString()
                } else {
                    ""
                }

            val parts = firstLine.split(" ")

            val method =
                parts.getOrNull(0)
                    ?: "GET"

            val rawPath =
                parts.getOrNull(1)
                    ?: "/"

            val path =
                rawPath.substringBefore("?")

            val response = when {

                method == "OPTIONS" -> {
                    json("{}", 204)
                }

                path == "/health" -> {
                    json(
                        JSONObject()
                            .put("ok", true)
                            .put(
                                "service",
                                "nuvio-simkl-android"
                            )
                            .put(
                                "discordConfigured",
                                isDiscordConfigured()
                            )
                            .toString()
                    )
                }

                path == "/presence" && method == "POST" -> {
                    handlePresenceJson(body)
                }

                path == "/presence" && method == "GET" -> {
                    handlePresenceQuery(rawPath)
                }

                path == "/presence/state" -> {
                    currentPresence()
                }

                path == "/presence/clear" -> {
                    presence.clear()

                    json(
                        "{\"ok\":true}"
                    )
                }

                path == "/manifest.json" -> {
                    json(manifest())
                }

                path.startsWith("/meta/") -> {
                    json(
                        """
                        {
                          "ok":false,
                          "error":"Android companion does not fake metadata. Connect the verified Simkl/provider resolver before enabling meta locally."
                        }
                        """.trimIndent(),
                        501
                    )
                }

                else -> {
                    json(
                        """
                        {
                          "ok":true,
                          "service":"Nuvio Simkl Android",
                          "endpoints":[
                            "POST /presence",
                            "GET /presence",
                            "GET /presence/state",
                            "GET /presence/clear",
                            "GET /health"
                          ]
                        }
                        """.trimIndent()
                    )
                }
            }

            s.getOutputStream().use { output ->

                output.write(
                    response.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

                output.flush()
            }
        }
    }

    private fun handlePresenceJson(
        body: String
    ): String {

        return runCatching {

            val jsonObject =
                JSONObject(
                    body.ifBlank {
                        "{}"
                    }
                )

            val event =
                PresenceEvent(
                    event =
                        jsonObject.optString(
                            "event",
                            "HOME"
                        ),

                    catalogId =
                        jsonObject.optNullableString(
                            "catalogId"
                        ),

                    catalogName =
                        jsonObject.optNullableString(
                            "catalogName"
                        ),

                    title =
                        jsonObject.optNullableString(
                            "title"
                        ),

                    mediaType =
                        jsonObject.optNullableString(
                            "mediaType"
                        ),

                    season =
                        jsonObject.optIntOrNull(
                            "season"
                        ),

                    episode =
                        jsonObject.optIntOrNull(
                            "episode"
                        ),

                    progress =
                        jsonObject.optIntOrNull(
                            "progress"
                        ),

                    positionSeconds =
                        jsonObject.optLongOrNull(
                            "positionSeconds"
                        ),

                    durationSeconds =
                        jsonObject.optLongOrNull(
                            "durationSeconds"
                        ),

                    artwork =
                        jsonObject.optNullableString(
                            "artwork"
                        ),

                    avatar =
                        jsonObject.optNullableString(
                            "avatar"
                        ),

                    username =
                        jsonObject.optNullableString(
                            "username"
                        ),

                    startTimestampMs =
                        jsonObject.optLongOrNull(
                            "startTimestampMs"
                        )
                )

            val ok =
                presence.handle(event)

            json(
                JSONObject()
                    .put("ok", ok)
                    .put(
                        "event",
                        event.event.uppercase()
                    )
                    .toString()
            )

        }.getOrElse { error ->

            json(
                JSONObject()
                    .put("ok", false)
                    .put(
                        "error",
                        error.message
                            ?: "Invalid JSON"
                    )
                    .toString(),
                400
            )
        }
    }

    private fun handlePresenceQuery(
        rawPath: String
    ): String {

        val query =
            rawPath.substringAfter(
                "?",
                ""
            )

        val map =
            query
                .split("&")
                .mapNotNull { parameter ->

                    val pair =
                        parameter.split(
                            "=",
                            limit = 2
                        )

                    if (pair.size == 2) {

                        URLDecoder.decode(
                            pair[0],
                            "UTF-8"
                        ) to
                            URLDecoder.decode(
                                pair[1],
                                "UTF-8"
                            )

                    } else {
                        null
                    }
                }
                .toMap()

        val event =
            PresenceEvent(
                event =
                    map["event"]
                        ?: "HOME",

                catalogId =
                    map["catalogId"],

                catalogName =
                    map["catalogName"],

                title =
                    map["title"],

                mediaType =
                    map["mediaType"],

                season =
                    map["season"]
                        ?.toIntOrNull(),

                episode =
                    map["episode"]
                        ?.toIntOrNull(),

                progress =
                    map["progress"]
                        ?.toIntOrNull(),

                positionSeconds =
                    map["positionSeconds"]
                        ?.toLongOrNull(),

                durationSeconds =
                    map["durationSeconds"]
                        ?.toLongOrNull(),

                artwork =
                    map["artwork"]
                        ?: map["large"],

                avatar =
                    map["avatar"]
                        ?: map["small"],

                username =
                    map["username"]
            )

        val ok =
            presence.handle(event)

        return json(
            "{\"ok\":$ok}"
        )
    }

    private fun currentPresence(): String {

        val event =
            presence.current()

        if (event == null) {
            return json(
                "{\"active\":false}"
            )
        }

        return JSONObject()
            .put(
                "active",
                true
            )
            .put(
                "event",
                event.event
            )
            .apply {

                event.catalogId?.let {
                    put(
                        "catalogId",
                        it
                    )
                }

                event.catalogName?.let {
                    put(
                        "catalogName",
                        it
                    )
                }

                event.title?.let {
                    put(
                        "title",
                        it
                    )
                }

                event.mediaType?.let {
                    put(
                        "mediaType",
                        it
                    )
                }

                event.season?.let {
                    put(
                        "season",
                        it
                    )
                }

                event.episode?.let {
                    put(
                        "episode",
                        it
                    )
                }

                event.progress?.let {
                    put(
                        "progress",
                        it
                    )
                }

                event.positionSeconds?.let {
                    put(
                        "positionSeconds",
                        it
                    )
                }

                event.durationSeconds?.let {
                    put(
                        "durationSeconds",
                        it
                    )
                }

                event.artwork?.let {
                    put(
                        "artwork",
                        it
                    )
                }

                event.avatar?.let {
                    put(
                        "avatar",
                        it
                    )
                }

                event.username?.let {
                    put(
                        "username",
                        it
                    )
                }

                event.startTimestampMs?.let {
                    put(
                        "startTimestampMs",
                        it
                    )
                }
            }
            .toString()
            .let {
                json(it)
            }
    }

    private fun isDiscordConfigured(): Boolean {

        val configValue =
            config.load()

        return configValue.discordEnabled &&
                configValue.discordApplicationId != 0L
    }

    private fun manifest(): String {

        return """
        {
          "id":"com.nuvio.simkl.android",
          "version":"1.1.0",
          "name":"Nuvio Simkl Android",
          "description":"Local Nuvio presence bridge; metadata is not fabricated locally.",
          "resources":["meta"],
          "types":["movie","series","anime"],
          "idPrefixes":[
            "simkl:",
            "tmdb:",
            "tvdb:",
            "imdb:",
            "mal:",
            "anilist:",
            "anidb:",
            "kitsu:"
          ],
          "catalogs":[],
          "behaviorHints":{
            "configurable":true
          }
        }
        """.trimIndent()
    }

    private fun json(
        body: String,
        code: Int = 200
    ): String {

        val reason =
            when (code) {

                204 ->
                    "No Content"

                400 ->
                    "Bad Request"

                501 ->
                    "Not Implemented"

                else ->
                    "OK"
            }

        val bytes =
            body.toByteArray(
                StandardCharsets.UTF_8
            )

        return buildString {

            append(
                "HTTP/1.1 "
            )

            append(code)

            append(" ")

            append(reason)

            append("\r\n")

            append(
                "Content-Type: application/json; charset=utf-8\r\n"
            )

            append(
                "Content-Length: "
            )

            append(bytes.size)

            append("\r\n")

            append(
                "Access-Control-Allow-Origin: *\r\n"
            )

            append(
                "Access-Control-Allow-Methods: GET,POST,OPTIONS\r\n"
            )

            append(
                "Access-Control-Allow-Headers: Content-Type\r\n"
            )

            append(
                "Connection: close\r\n"
            )

            append(
                "\r\n"
            )

            append(body)
        }
    }
}

private fun JSONObject.optNullableString(
    name: String
): String? {

    return optString(
        name,
        ""
    ).takeIf {
        it.isNotBlank()
    }
}

private fun JSONObject.optIntOrNull(
    name: String
): Int? {

    if (!has(name) || isNull(name)) {
        return null
    }

    val value =
        optInt(name)

    return value.takeIf {
        it != 0 ||
                optString(name) == "0"
    }
}

private fun JSONObject.optLongOrNull(
    name: String
): Long? {

    if (!has(name) || isNull(name)) {
        return null
    }

    val value =
        optLong(name)

    return value.takeIf {
        it != 0L ||
                optString(name) == "0"
    }
}
