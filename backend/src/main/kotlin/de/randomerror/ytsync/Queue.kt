package de.randomerror.ytsync

import com.google.gson.JsonParser
import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.io.StringWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val youtubeUrlRegex: Regex = Regex("""https://(?:www)?\.youtu(?:\.be|be\.com)/watch\?v=([^&]+)(?:.*)?""")
val videoInfoFetcher: ExecutorService = Executors.newCachedThreadPool()

private val logger = KotlinLogging.logger {}

private data class VideoInfo(
    val id: String,
    val title: String,
    val thumbnail: String?,
    val extractor: String
)

fun enqueue(session: Session, query: String): String {
    val room = getRoom(session)
    val fallbackInfo = tryExtractVideoId(query)
    if (fallbackInfo != null) {
        synchronized(room.queue) {
            if (room.queue.size == 0) {
                val queueItem = QueueItem(fallbackInfo.id, fallbackInfo.title, fallbackInfo.thumbnail)
                room.queue.add(queueItem)
                room.broadcastAll(session, "video ${fallbackInfo.id}")
                return "queue"
            }
        }
    }
    videoInfoFetcher.execute {
        val video = fetchVideoInfo(query) ?: fallbackInfo
        if (video == null || video.extractor != "youtube") {
            log(session, "queue err not-found")
            session.remote.sendStringByFuture("queue err not-found")
            return@execute
        }
        val queueItem = QueueItem(video.id, video.title, video.thumbnail)
        synchronized(room.queue) {
            if (room.queue.any { it.id == video.id }) {
                session.remote.sendStringByFuture("queue err duplicate")
                return@execute
            }
            room.queue.add(queueItem)
            if (room.queue.size == 1) {
                room.broadcastAll(session, "video ${video.id}")
            } else {
                room.broadcastAll(session, "queue add ${gson.toJson(queueItem)}")
            }
        }
    }
    return "queue"
}

private fun tryExtractVideoId(query: String): VideoInfo? {
    val match = youtubeUrlRegex.find(query) ?: return null
    val id = match.groups[1]!!.value
    return VideoInfo(
        id,
        "unknown video $id",
        "https://i.ytimg.com/vi/$id/maxresdefault.jpg",
        "youtube"
    )
}

fun dequeue(session: Session, videoId: String): String {
    val room = getRoom(session)
    // first in queue is currently playing song
    if (room.queue.isNotEmpty() && room.queue[0].id == videoId) {
        return "queue rm deny"
    }
    room.queue.removeAll { it.id == videoId }
    room.broadcastAll(session, "queue rm $videoId")
    return "queue rm"
}

fun reorder(session: Session, order: String): String {
    val room = getRoom(session)
    val queue = room.queue
    val oldOrder = queue.drop(1).map { it.id }
    val newOrder = order.split(',')

    println(oldOrder)
    println(newOrder)
    println(oldOrder.toSet() == newOrder.toSet())
    if (oldOrder.toSet() != newOrder.toSet()) {
        return "queue order deny"
    }

    queue.subList(1, queue.size).sortBy { video -> newOrder.indexOf(video.id) }
    room.broadcastAll(session, "queue order $order")

    return "queue order ok"
}

private fun fetchVideoInfo(query: String): VideoInfo? {
    val process = Runtime.getRuntime().exec(
        arrayOf(
            "youtube-dl",
            "--default-search", "ytsearch",
            "--no-playlist",
            "--dump-json",
            query
        )
    )
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
        val id = video.get("id").asString
        val title = video["title"].asString
        val thumbnail = video["thumbnail"].asString
        val extractor = video["extractor"].asString
        VideoInfo(id, title, thumbnail, extractor)
    } catch (e: Exception) {
        null
    }
}

fun skip(session: Session): String {
    val room = getRoom(session)
    // first in queue is currently playing song
    if (room.queue.size < 2) {
        return "skip deny"
    }
    playNext(session, room)
    return "skip"
}
