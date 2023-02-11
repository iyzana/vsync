package de.randomerror.ytsync

import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.text.RegexOption.IGNORE_CASE

private val youtubeUrlRegex: Regex =
    Regex("""https://(?:www\.)?youtu(?:\.be|be\.com)/(?:watch\?v=|embed/)?([^?&]+)(?:.*)?""", IGNORE_CASE)
private val videoInfoFetcher: ExecutorService = Executors.newCachedThreadPool()

fun enqueue(session: Session, query: String): String {
    val room = getRoom(session)

    val youtubeId = youtubeUrlRegex.find(query)?.let { it.groups[1]!!.value }
    if (youtubeId != null) {
        synchronized(room.queue) {
            if (room.queue.size == 0) {
                val fallbackVideo = getFallbackYoutubeVideo(query, youtubeId)
                // this is the first video it does not go into the queue, we don't need any video info
                room.queue.add(fallbackVideo)
                room.broadcastAll(session, "video ${fallbackVideo.url}")
                return "queue"
            }
        }
    }
    videoInfoFetcher.execute {
        // try to get video info, but if it fails, use the fallback info so that the video at least plays
        val video = fetchVideoInfo(query, youtubeId) ?: youtubeId?.let { getFallbackYoutubeVideo(query, it) }
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
