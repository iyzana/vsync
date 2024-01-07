package de.randomerror.ytsync

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import mu.KotlinLogging
import java.io.FileNotFoundException
import java.io.StringWriter
import java.lang.AssertionError
import java.lang.UnsupportedOperationException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val YT_DLP_TIMEOUT = 15L

private val logger = KotlinLogging.logger {}

fun getInitialVideoInfo(query: String, youtubeId: String?): QueueItem {
    val favicon = getInitialFavicon(query, youtubeId)
    if (youtubeId !== null) {
        return QueueItem(
            VideoSource(
                "https://www.youtube.com/watch?v=$youtubeId",
                null
            ),
            query,
            null,
            "https://i.ytimg.com/vi/$youtubeId/mqdefault.jpg",
            favicon,
            true
        )
    }
    return QueueItem(null, query, null, null, favicon, true)
}

fun getFallbackYoutubeVideo(query: String, youtubeId: String): QueueItem {
    return QueueItem(
        VideoSource(
            "https://www.youtube.com/watch?v=$youtubeId",
            null
        ),
        query,
        "Unknown video $youtubeId",
        "https://i.ytimg.com/vi/$youtubeId/mqdefault.jpg",
        getInitialFavicon(query, youtubeId),
        false
    )
}

fun fetchVideoInfo(query: String, youtubeId: String?): QueueItem? {
    if (youtubeId != null) {
        fetchVideoInfoYouTubeOEmbed(query, youtubeId)?.let { return it }
    }
    return fetchVideoInfoYtDlp(youtubeId, query)
}

private fun fetchVideoInfoYouTubeOEmbed(query: String, youtubeId: String): QueueItem? {
    val videoData = try {
        URL("https://www.youtube.com/oembed?url=$query").readText()
    } catch (ignored: FileNotFoundException) {
        return null
    }
    return try {
        val video = JsonParser.parseString(videoData).asJsonObject
        val title = video.getNullable("title")?.asString
        val thumbnail = "https://i.ytimg.com/vi/$youtubeId/mqdefault.jpg"
        QueueItem(VideoSource(query, null), query, title, thumbnail, null, false)
    } catch (e: JsonParseException) {
        logger.warn("failed to parse oembed response for query $query", e)
        null
    } catch (e: AssertionError) {
        logger.warn("failed to parse oembed response for query $query", e)
        null
    } catch (e: UnsupportedOperationException) {
        logger.warn("failed to parse oembed response for query $query", e)
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
        logger.warn("ytdl timeout")
        process.destroy()
        return null
    }
    if (process.exitValue() != 0) {
        logger.warn("ytdl err")
        logger.warn(process.errorStream.bufferedReader().readText())
        return null
    }
    reader.join()
    val videoData = result.buffer.toString()
    return parseYtDlpOutput(videoData, query, isYoutube)
}

private fun parseYtDlpOutput(
    videoData: String,
    query: String,
    isYoutube: Boolean
): QueueItem? {
    return try {
        val video = JsonParser.parseString(videoData).asJsonObject
        val url = if (isYoutube) {
            video.getNullable("webpage_url")
        } else {
            video.getNullable("manifest_url") ?: video.getNullable("url")
        }?.asString ?: return null
        val title = video.getNullable("title")?.asString
        val thumbnail = video.getNullable("thumbnail")?.asString
        val contentType = if (isYoutube) null else getContentType(url)
        QueueItem(VideoSource(url, contentType), query, title, thumbnail, null, false)
    } catch (e: JsonParseException) {
        logger.warn("failed to parse ytdlp output for query $query", e)
        null
    } catch (e: AssertionError) {
        logger.warn("failed to parse ytdlp output for query $query", e)
        null
    } catch (e: UnsupportedOperationException) {
        logger.warn("failed to parse ytdlp output for query $query", e)
        null
    }
}

private fun getContentType(url: String): String? {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "HEAD"
    return connection.getHeaderField("Content-Type")
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
        "--dump-json",
    )
    if (!fromYoutube) {
        // only allow pre-merged formats except from youtube, videojs can't play split streams
        // m3u8 can be problematic if the hoster does not set an access-control-allow-origin header
        command.add("-f")
        command.add("b")
    }
    command.add("--")
    command.add(query)
    println("command = $command")
    return command.toTypedArray()
}

