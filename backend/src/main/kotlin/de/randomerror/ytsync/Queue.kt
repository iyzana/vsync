package de.randomerror.ytsync

import io.javalin.websocket.WsContext
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

fun enqueue(ws: WsContext, query: String): String {
    val room = getRoom(ws)

    val youtubeId = youtubeUrlRegex.find(query)?.let { it.groups[1]!!.value }
    if (youtubeId != null) {
        synchronized(room.queue) {
            if (room.queue.size == 0) {
                enqueueFallbackVideo(room, ws, query, youtubeId)
                return "queue immediate"
            }
        }
    }
    if (room.queue.any { it.originalQuery == query }) {
        ws.send("queue err duplicate")
        return "queue err duplicate"
    }
    val loadingQueueItem = getInitialVideoInfo(query, youtubeId)
    room.broadcastAll(ws, "queue add ${gson.toJson(loadingQueueItem)}")
    videoInfoFetcher.execute {
        // try to get video info, but if it fails, use the fallback info so that the video at least plays
        val videoInfo = fetchVideoInfo(query, youtubeId) ?: youtubeId?.let { getFallbackYoutubeVideo(query, it) }
        enqueueVideo(room, ws, videoInfo, loadingQueueItem)
    }
    return "queue fetching"
}

fun enqueueFallbackVideo(room: Room, ws: WsContext, query: String, youtubeId: String) {
    // this is the first video, it does not go into the queue, we don't need any video info
    val fallbackVideo = getFallbackYoutubeVideo(query, youtubeId)
    room.queue.add(fallbackVideo)
    room.broadcastAll(ws, "video ${gson.toJson(fallbackVideo.source)}")
    room.numQueuedVideos += 1
}

fun enqueueVideo(room: Room, ws: WsContext, videoInfo: QueueItem?, loadingQueueItem: QueueItem) {
    if (videoInfo == null) {
        log(ws, "queue err not-found")
        room.broadcastAll(ws, "queue rm ${loadingQueueItem.id}")
        ws.send("queue err not-found")
        return
    }
    val queueItem = videoInfo.copy(
        id = loadingQueueItem.id,
        favicon = videoInfo.favicon ?: loadingQueueItem.favicon
    )
    synchronized(room.queue) {
        if (room.queue.any { it.source != null && it.source.url == queueItem.source?.url }) {
            ws.send("queue err duplicate")
            room.broadcastAll(ws, "queue rm ${loadingQueueItem.id}")
            return
        }
        room.queue.add(queueItem)
        room.numQueuedVideos += 1
        if (room.queue.size == 1) {
            room.broadcastAll(ws, "queue rm ${queueItem.id}")
            room.broadcastAll(ws, "video ${gson.toJson(queueItem.source)}")
            return
        } else {
            room.broadcastAll(ws, "queue add ${gson.toJson(queueItem)}")
        }
    }
    val favicon = getFavicon(queueItem.originalQuery, queueItem.source!!.url)
    if (favicon != null && favicon != queueItem.favicon) {
        synchronized(room.queue) {
            val index = room.queue.indexOfFirst { it.id == queueItem.id }
            if (index > 0) {
                val queueItemWithFavicon = queueItem.copy(favicon = favicon)
                room.broadcastAll(ws, "queue add ${gson.toJson(queueItemWithFavicon)}")
                room.queue[index] = queueItemWithFavicon
            }
        }
    }
}

fun dequeue(ws: WsContext, queueId: String): String {
    val room = getRoom(ws)
    // first in queue is currently playing video
    if (room.queue.isNotEmpty() && room.queue[0].id == queueId) {
        return "queue rm deny"
    }
    room.queue.removeAll { it.id == queueId }
    room.broadcastAll(ws, "queue rm $queueId")
    return "queue rm"
}

fun reorder(ws: WsContext, order: String): String {
    val room = getRoom(ws)
    val queue = room.queue
    val oldOrder = queue.drop(1).map { it.id }
    val newOrder = order.split(',')

    // mustn't change queue using reorder
    if (oldOrder.toSet() != newOrder.toSet()) {
        return "queue order deny"
    }

    queue.subList(1, queue.size).sortBy { video -> newOrder.indexOf(video.id) }
    room.broadcastAll(ws, "queue order $order")

    return "queue order ok"
}

fun skip(ws: WsContext): String {
    val room = getRoom(ws)
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

        playNext(ws, room)
    }

    return "skip"
}
