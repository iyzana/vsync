package de.randomerror.ytsync

import com.google.gson.JsonParser
import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.net.URL
import java.io.StringWriter
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val youtubeUrlRegex: Regex = Regex("""https://(?:www\.)?youtu(?:\.be|be\.com)/watch\?v=([^&]+)(?:.*)?""")
val videoInfoFetcher: ExecutorService = Executors.newCachedThreadPool()

private val logger = KotlinLogging.logger {}

private data class VideoInfo(
    val id: String,
    val title: String,
    val thumbnail: String?
)

fun enqueue(session: Session, query: String): String {
    val room = getRoom(session)
    val fallbackInfo = tryExtractVideoId(query)
    if (fallbackInfo != null) {
        synchronized(room.queue) {
            if (room.queue.size == 0) {
                // this is the first video it does not go into the queue, we don't need any video info
                val queueItem = QueueItem(fallbackInfo.id, fallbackInfo.title, fallbackInfo.thumbnail)
                room.queue.add(queueItem)
                room.broadcastAll(session, "video ${fallbackInfo.id}")
                return "queue"
            }
        }
    }
    videoInfoFetcher.execute {
        // try to get video info, but if it fails, use the fallback info so that the video at least plays
        val video = fetchVideoInfo(query) ?: fallbackInfo
        if (video == null) {
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
        "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
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

    // mustn't change queue using reorder
    if (oldOrder.toSet() != newOrder.toSet()) {
        return "queue order deny"
    }

    queue.subList(1, queue.size).sortBy { video -> newOrder.indexOf(video.id) }
    room.broadcastAll(session, "queue order $order")

    return "queue order ok"
}

private fun fetchVideoInfo(query: String): VideoInfo? {
    //request oembed object
    val videoData = URL("https://www.youtube.com/oembed?url=$query").readText()

    if (videoData == "Not Found") {
        return null
    }

    val video = JsonParser.parseString(videoData).asJsonObject
    return try {
        val id = video.get("id").asString
        val title = video["title"].asString
        val thumbnail = video["thumbnail_url"].asString
        VideoInfo(id, title, thumbnail)
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

    val ignoreEndTill = room.ignoreEndTill
    if (ignoreEndTill != null && ignoreEndTill.isAfter(Instant.now())) {
        return "skip ignore"
    }

    room.ignoreEndTill = Instant.now().plusSeconds(2)
    playNext(session, room)

    return "skip"
}
