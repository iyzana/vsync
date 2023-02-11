package de.randomerror.ytsync

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import mu.KotlinLogging
import java.io.StringWriter
import java.lang.AssertionError
import java.lang.UnsupportedOperationException
import java.net.URL
import java.util.concurrent.TimeUnit

private const val YT_DLP_TIMEOUT = 5L

private val logger = KotlinLogging.logger {}

fun getFallbackYoutubeVideo(query: String, match: String): QueueItem {
    return QueueItem(
        "https://www.youtube.com/watch?v=$match",
        query,
        "Unknown video $match",
        "https://i.ytimg.com/vi/$match/mqdefault.jpg"
    )
}

fun fetchVideoInfo(query: String, youtubeId: String?): QueueItem? {
    if (youtubeId != null) {
        fetchVideoInfoYouTubeOEmbed(query, youtubeId)?.let { return it }
    }
    return fetchVideoInfoYtDlp(youtubeId, query)
}

private fun fetchVideoInfoYouTubeOEmbed(query: String, youtubeId: String): QueueItem? {
    val videoData = URL("https://www.youtube.com/oembed?url=$query").readText()
    if (videoData == "Not Found") {
        return null
    }
    return try {
        val video = JsonParser.parseString(videoData).asJsonObject
        val id = youtubeId
        val title = video["title"]?.asString
        val thumbnail = "https://i.ytimg.com/vi/$youtubeId/mqdefault.jpg"
        QueueItem(query, query, title, thumbnail, id)
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
    process.inputStream.bufferedReader().copyTo(result)
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
        val urlElement = if (isYoutube) {
            video["webpage_url"]
        } else {
            video["manifest_url"] ?: video["url"]
        }?.asString ?: return null
        val title = video["title"]?.asString
        val thumbnail = video["thumbnail"]?.asString
        QueueItem(urlElement, query, title, thumbnail)
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

