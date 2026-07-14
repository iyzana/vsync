package de.randomerror.ytsync

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.FileNotFoundException
import java.io.IOException
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
    } catch (e: IOException) {
        logger.warn(e) { "failed to parse oembed response for query $query" }
        null
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

