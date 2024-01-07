package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.text.RegexOption.IGNORE_CASE

private val youtubeUrlRegex: Regex =
    Regex("""https://(?:www\.)?youtu(?:\.be|be\.com)/(?:watch\?v=|embed/|shorts/)?([^?&]+)(?:.*)?""", IGNORE_CASE)

private const val FETCHER_CORE_POOL = 4
private const val FETCHER_MAX_POOL = 16
private const val FETCHER_KEEPALIVE_SECONDS = 60L
private val videoInfoFetcher: ExecutorService =
    ThreadPoolExecutor(FETCHER_CORE_POOL, FETCHER_MAX_POOL, FETCHER_KEEPALIVE_SECONDS, SECONDS, LinkedBlockingQueue())

fun enqueue(session: Session, query: String): String {
    val room = getRoom(session)

    val youtubeId = youtubeUrlRegex.find(query)?.let { it.groups[1]!!.value }
    if (youtubeId != null) {
        synchronized(room.queue) {
            if (room.queue.size == 0) {
                // this is the first video, it does not go into the queue, we don't need any video info
                val fallbackVideo = getFallbackYoutubeVideo(query, youtubeId)
                room.queue.add(fallbackVideo)
                room.broadcastAll(session, "video ${gson.toJson(fallbackVideo.source)}")
                return "queue immediate"
            }
        }
    }
    if (room.queue.any { it.originalQuery == query }) {
        session.remote.sendStringByFuture("queue err duplicate")
        return "queue err duplicate"
    }
    val loadingQueueItem = getInitialVideoInfo(query, youtubeId)
    room.broadcastAll(session, "queue add ${gson.toJson(loadingQueueItem)}")
    videoInfoFetcher.execute {
        // try to get video info, but if it fails, use the fallback info so that the video at least plays
        val videoInfo = (fetchVideoInfo(query, youtubeId) ?: youtubeId?.let { getFallbackYoutubeVideo(query, it) })
        if (videoInfo == null) {
            log(session, "queue err not-found")
            room.broadcastAll(session, "queue rm ${loadingQueueItem.id}")
            session.remote.sendStringByFuture("queue err not-found")
            return@execute
        }
        val queueItem =
            videoInfo.copy(id = loadingQueueItem.id, favicon = videoInfo.favicon ?: loadingQueueItem.favicon)
        synchronized(room.queue) {
            if (room.queue.any { it.source != null && it.source.url == queueItem.source?.url }) {
                session.remote.sendStringByFuture("queue err duplicate")
                room.broadcastAll(session, "queue rm ${loadingQueueItem.id}")
                return@execute
            }
            room.queue.add(queueItem)
            if (room.queue.size == 1) {
                room.broadcastAll(session, "queue rm ${queueItem.id}")
                room.broadcastAll(session, "video ${gson.toJson(queueItem.source)}")
                return@execute
            } else {
                room.broadcastAll(session, "queue add ${gson.toJson(queueItem)}")
            }
        }
        val favicon = getFavicon(query, queueItem.source!!.url)
        println(favicon)
        if (favicon != null && favicon != queueItem.favicon) {
            synchronized(room.queue) {
                val index = room.queue.indexOfFirst { it.id == queueItem.id }
                if (index > 0) {
                    val queueItemWithFavicon = queueItem.copy(favicon = favicon)
                    room.broadcastAll(session, "queue add ${gson.toJson(queueItemWithFavicon)}")
                    room.queue[index] = queueItemWithFavicon
                }
            }
        }
    }
    return "queue fetching"
}

fun dequeue(session: Session, queueId: String): String {
    val room = getRoom(session)
    // first in queue is currently playing video
    if (room.queue.isNotEmpty() && room.queue[0].id == queueId) {
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
        // first in queue is currently playing video
        if (room.queue.size < 2) {
            return "skip deny"
        }

        val ignoreSkipTill = room.ignoreSkipTill
        if (ignoreSkipTill != null && ignoreSkipTill.isAfter(Instant.now())) {
            return "skip ignore"
        }
        room.ignoreSkipTill = Instant.now().plusSeconds(2)

        playNext(session, room)
    }

    return "skip"
}
