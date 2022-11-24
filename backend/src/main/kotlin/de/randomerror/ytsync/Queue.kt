package de.randomerror.ytsync

import com.google.gson.JsonParser
import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.io.StringWriter
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.text.RegexOption.IGNORE_CASE

val youtubeUrlRegex: Regex =
    Regex("""https://(?:www\.)?youtu(?:\.be|be\.com)/(?:watch\?v=|embed/)?([^?&]+)(?:.*)?""", IGNORE_CASE)
val videoInfoFetcher: ExecutorService = Executors.newCachedThreadPool()

private val logger = KotlinLogging.logger {}

fun enqueue(session: Session, query: String): String {
    val room = getRoom(session)

    val youtubeIdMatch = youtubeUrlRegex.find(query)?.let { it.groups[1]!!.value }
    if (youtubeIdMatch != null) {
        synchronized(room.queue) {
            if (room.queue.size == 0) {
                val fallbackVideo = getFallbackYoutubeVideo(query, youtubeIdMatch)
                // this is the first video it does not go into the queue, we don't need any video info
                room.queue.add(fallbackVideo)
                room.broadcastAll(session, "video ${fallbackVideo.url}")
                return "queue"
            }
        }
    }
    videoInfoFetcher.execute {
        val fromYoutube = youtubeIdMatch != null || !query.matches(Regex("^(ftp|https?)://.*"))
        // try to get video info, but if it fails, use the fallback info so that the video at least plays
        val video = fetchVideoInfo(query, fromYoutube) ?: youtubeIdMatch?.let { getFallbackYoutubeVideo(query, it) }
        if (video == null) {
            log(session, "queue err not-found")
            session.remote.sendStringByFuture("queue err not-found")
            return@execute
        }
        synchronized(room.queue) {
            if (room.queue.any { it.url == video.url || it.originalQuery == query }) {
                session.remote.sendStringByFuture("queue err duplicate")
                return@execute
            }
            room.queue.add(video)
            if (room.queue.size == 1) {
                room.broadcastAll(session, "video ${video.url}")
            } else {
                room.broadcastAll(session, "queue add ${gson.toJson(video)}")
            }
        }
    }
    return "queue"
}

private fun getFallbackYoutubeVideo(query: String, match: String): QueueItem {
    return QueueItem(
        "https://www.youtube.com/watch?v=$match",
        query,
        "Unknown video $match",
        "https://i.ytimg.com/vi/$match/maxresdefault.jpg"
    )
}

fun dequeue(session: Session, queueId: String): String {
    val room = getRoom(session)
    // first in queue is currently playing song
    if (room.queue.isNotEmpty() && room.queue[0].url == queueId) {
        return "queue rm deny"
    }
    room.queue.removeAll { it.id == queueId }
    room.broadcastAll(session, "queue rm $queueId")
    return "queue rm"
}

fun reorder(session: Session, order: String): String {
    val room = getRoom(session)
    val queue = room.queue
    val oldOrder = queue.drop(1).map { it.id }
    val newOrder = order.split(',')

    // mustn't change queue using reorder
    if (oldOrder.toSet() != newOrder.toSet()) {
        return "queue order deny"
    }

    queue.subList(1, queue.size).sortBy { video -> newOrder.indexOf(video.id) }
    room.broadcastAll(session, "queue order $order")

    return "queue order ok"
}

private fun fetchVideoInfo(query: String, fromYoutube: Boolean): QueueItem? {
    val process = Runtime.getRuntime().exec(buildYtDlpCommand(fromYoutube, query))
    val result = StringWriter()
    process.inputStream.bufferedReader().copyTo(result)
    if (!process.waitFor(5, TimeUnit.SECONDS)) {
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
    val video = JsonParser.parseString(videoData).asJsonObject
    return try {
        val urlElement = if (fromYoutube) {
            video["webpage_url"]
        } else {
            video["manifest_url"] ?: video["url"] ?: return null
        }
        val title = video["title"].asString
        val thumbnail = video["thumbnail"]?.asString
        QueueItem(urlElement.asString, query, title, thumbnail)
    } catch (e: Exception) {
        null
    }
}

private fun buildYtDlpCommand(fromYoutube: Boolean, query: String): Array<String> {
    val command = mutableListOf(
        "yt-dlp",
        "--default-search", "ytsearch",
        "--no-playlist",
        "--dump-json",
    )
    if (!fromYoutube) {
        // only allow pre-merged formats except from youtube
        // m3u8 can be problematic if the hoster does not set a access-control-allow-origin header
        command.add("-f")
        command.add("b")
    }
    command.add("--")
    command.add(query)
    println("command = $command")
    return command.toTypedArray()
}

fun skip(session: Session): String {
    val room = getRoom(session)
    synchronized(room.queue) {
        // first in queue is currently playing song
        if (room.queue.size < 2) {
            return "skip deny"
        }

        val ignoreEndTill = room.ignoreEndTill
        if (ignoreEndTill != null && ignoreEndTill.isAfter(Instant.now())) {
            return "skip ignore"
        }

        room.ignoreEndTill = Instant.now().plusSeconds(2)
        playNext(session, room)
    }

    return "skip"
}
