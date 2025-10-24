package de.randomerror.ytsync

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.FileNotFoundException
import java.io.StringWriter
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val YT_DLP_TIMEOUT = 15L

private val logger = KotlinLogging.logger {}

fun getInitialVideoInfo(query: String, youtubeId: String?, loading: Boolean = true): QueueItem {
    val favicon = getInitialFavicon(query, youtubeId)
    val startTime = findStartTimeSeconds(query)
    if (youtubeId !== null) {
        return QueueItem(
            VideoSource(
                "https://www.youtube.com/watch?v=$youtubeId",
                null,
            ),
            query,
            null,
            startTime,
            "https://i.ytimg.com/vi/$youtubeId/mqdefault.jpg",
            favicon,
            loading
        )
    }
    return QueueItem(
        null, query,
        null, null, null, favicon, loading
    )
}

fun getFallbackYoutubeVideo(query: String, youtubeId: String): QueueItem {
    return getInitialVideoInfo(query, youtubeId, loading = false)
}

private val startTimeRegex: Regex = Regex("""[&?]t=(([0-9]+[hms]?)+)( |&|$)""", RegexOption.IGNORE_CASE)

fun findStartTimeSeconds(query: String): Int? {
    val startTimeFragment: MatchResult = startTimeRegex.find(query) ?: return null
    val startTimeSpec: String = startTimeFragment.groups[1]?.value ?: return null
    var totalDuration = Duration.ZERO
    var segmentStart = 0
    startTimeSpec.forEachIndexed { index, char ->
        if (char.isDigit()) {
            return@forEachIndexed
        }
        val value = startTimeSpec.substring(segmentStart, index).toInt()
        totalDuration += when (char.lowercaseChar()) {
            'h' -> value.hours
            'm' -> value.minutes
            else -> value.seconds
        }
        segmentStart = index + 1
    }
    if (segmentStart < startTimeSpec.chars().count()) {
        totalDuration += startTimeSpec.substring(segmentStart).toInt().seconds
    }
    return totalDuration.inWholeSeconds.toInt()
}

fun fetchVideoInfo(query: String, youtubeId: String?): QueueItem? {
    if (youtubeId != null) {
        fetchVideoInfoYouTubeOEmbed(query, youtubeId)?.let { return it }
    }
    return fetchVideoInfoYtDlp(youtubeId, query)
}

private fun fetchVideoInfoYouTubeOEmbed(query: String, youtubeId: String): QueueItem? {
    val videoData = try {
        URI("https://www.youtube.com/oembed?url=$query").toURL().readText()
    } catch (_: FileNotFoundException) {
        return null
    }
    return try {
        val video = JsonParser.parseString(videoData).asJsonObject
        val startTime = findStartTimeSeconds(query)
        val title = video.getNullable("title")?.asString
        val channel = video.getNullable("author_name")?.asString
        val thumbnail = "https://i.ytimg.com/vi/$youtubeId/mqdefault.jpg"
        QueueItem(
            VideoSource(query, null), query,
            VideoMetadata(title, null, null, null, channel),
            startTime, thumbnail, null, false
        )
    } catch (e: JsonParseException) {
        logger.warn(e) { "failed to parse oembed response for query $query" }
        null
    } catch (e: AssertionError) {
        logger.warn(e) { "failed to parse oembed response for query $query" }
        null
    } catch (e: UnsupportedOperationException) {
        logger.warn(e) { "failed to parse oembed response for query $query" }
        null
    }
}

private fun fetchVideoInfoYtDlp(youtubeId: String?, query: String): QueueItem? {
    val isYoutube = youtubeId != null || !query.matches(Regex("^(ftp|https?)://.*"))
    val process = Runtime.getRuntime().exec(buildYtDlpCommand(query, isYoutube))
    val result = StringWriter()
    val reader = thread(isDaemon = true) {
        process.inputStream.bufferedReader().copyTo(result)
    }
    if (!process.waitFor(YT_DLP_TIMEOUT, TimeUnit.SECONDS)) {
        logger.warn { "ytdl timeout" }
        process.destroy()
        return null
    }
    if (process.exitValue() != 0) {
        logger.warn { "ytdl err ${process.exitValue()}" }
        logger.warn { process.errorStream.bufferedReader().readText() }
        return null
    }
    reader.join()
    val videoData = result.buffer.toString()
    return parseYtDlpOutput(videoData, query, isYoutube)
}

data class YtDlpFormat(
    val url: String,
    val manifestUrl: String?,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val ext: String?,
    val protocol: String?,
)

private fun parseYtDlpOutput(
    videoData: String,
    query: String,
    isYoutube: Boolean
): QueueItem? {
    return try {
        val video = JsonParser.parseString(videoData).asJsonObject
        val videoSource = getVideoSource(video, isYoutube) ?: return null
        val title = video.getNullable("title")?.asString
        val series = video.getNullable("series")?.asString?.trim()
        val seasonNumber = video.getNullable("season_number")?.asInt
        val episodeNumber = video.getNullable("episode_number")?.asInt
        val channel = video.getNullable("channel")?.asString
        val thumbnail = video.getNullable("thumbnail")?.asString
        val startTime = video.getNullable("start_time")?.asInt ?: findStartTimeSeconds(query)
        QueueItem(
            videoSource, query,
            VideoMetadata(title, series, seasonNumber, episodeNumber, channel),
            startTime, thumbnail, null, false
        )
    } catch (e: Exception) {
        logger.warn(e) { "failed to parse ytdlp output for query $query" }
        null
    }
}

private fun getVideoSource(video: JsonObject, isYoutube: Boolean): VideoSource? {
    if (isYoutube) {
        return VideoSource(video.get("webpage_url").asString, null)
    }
    val mainManifestUrl = video.getNullable("manifest_url")?.asString
    if (mainManifestUrl != null) {
        return toVideoSource(mainManifestUrl)
    }
    val formats = parseYtDlpFormats(video)
    if (formats.all { !it.hasVideo } || formats.all { !it.hasAudio }) {
        val format = formats.first()
        return toVideoSource(format)
    }
    val fullManifests = formats
        .filter { it.manifestUrl != null }
        .groupBy { it.manifestUrl }
        .filter { (_, manifestFormats) ->
            manifestFormats.find { it.hasVideo } != null
                && manifestFormats.find { it.hasAudio } != null
        }
        .map { (manifestUrl, _) -> manifestUrl }
    val format = formats
        .find { format -> (format.hasVideo && format.hasAudio) || fullManifests.contains(format.manifestUrl) }
        ?: return null
    return toVideoSource(format)
}

private fun toVideoSource(url: String): VideoSource {
    return VideoSource(url, fetchContentType(url))
}

private fun toVideoSource(format: YtDlpFormat): VideoSource {
    val url = format.manifestUrl ?: format.url
    var contentType = fetchContentType(url)
    if (contentType == null
        || contentType == "binary/octet-stream"
        || contentType == "application/octet-stream"
        || contentType.startsWith("text/")) {
        if (format.protocol in listOf("https", "http") && format.ext == "mp4") {
            contentType = "video/mp4"
        }
        if (format.protocol in listOf("https", "http") && format.ext == "m4a") {
            contentType = "audio/m4a"
        }
    }
    return VideoSource(url, contentType)
}

private fun parseYtDlpFormats(video: JsonObject): List<YtDlpFormat> {
    return video.get("formats").asJsonArray.map { it.asJsonObject }.map { format ->
        var vcodec = format.getNullable("vcodec")?.asString
        var acodec = format.getNullable("acodec")?.asString
        YtDlpFormat(
            format.get("url").asString,
            format.getNullable("manifest_url")?.asString,
            vcodec != "none",
            acodec != "none",
            format.getNullable("ext")?.asString,
            format.getNullable("protocol")?.asString,
        )
    }.reversed()
}

private fun fetchContentType(url: String): String? {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    AutoCloseable { connection.disconnect() }.use {
        connection.requestMethod = "GET"
        return connection.getHeaderField("Content-Type")
    }
}

fun JsonObject.getNullable(key: String): JsonElement? {
    val value: JsonElement = this.get(key) ?: return null
    return if (value.isJsonNull) {
        null
    } else {
        value
    }
}

private fun buildYtDlpCommand(query: String, fromYoutube: Boolean): Array<String> {
    val command = mutableListOf(
        "yt-dlp",
        "--default-search", "ytsearch",
        "--no-playlist",
        "--playlist-items", "1:1",
        "--dump-json",
    )
    command.add("--")
    command.add(query)
    return command.toTypedArray()
}
