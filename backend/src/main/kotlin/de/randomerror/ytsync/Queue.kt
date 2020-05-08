package de.randomerror.ytsync

import com.google.gson.Gson
import com.google.gson.JsonParser
import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.lang.ClassCastException
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val videoInfoFetcher: ExecutorService = Executors.newCachedThreadPool()

private val logger = KotlinLogging.logger {}

private data class VideoInfo(
    val id: String,
    val title: String,
    val thumbnail: String,
    val extractor: String
)

fun enqueue(session: Session, query: String): String {
    val room = getRoom(session)
    videoInfoFetcher.execute {
        val video = fetchVideoInfo(query)
        if (video == null || video.extractor != "youtube") {
            log(session, "queue err not-found")
            session.remote.sendStringByFuture("queue err not-found")
            return@execute
        }
        val queueItem = QueueItem(video.id, video.title, video.thumbnail)
        synchronized(room.queue) {
            if (room.queue.any { it.videoId == video.id }) {
                session.remote.sendStringByFuture("queue err duplicate")
                return@execute
            }
            room.queue.add(queueItem)
        }
        if (room.queue.size == 1) {
            room.broadcastAll("video ${video.id}")
        } else {
            room.broadcastAll("queue add ${gson.toJson(queueItem)}")
        }
    }
    return "queue"
}

private fun fetchVideoInfo(query: String): VideoInfo? {
    val process = Runtime.getRuntime().exec(
        arrayOf("youtube-dl", "--default-search", "ytsearch", "--dump-json", query)
    )
    if (!process.waitFor(5, TimeUnit.SECONDS)) {
        process.destroy()
        return null
    }
    if (process.exitValue() != 0) {
        logger.warn(process.errorStream.bufferedReader().readText())
        return null
    }
    val videoData = process.inputStream.bufferedReader().readText().trim()
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