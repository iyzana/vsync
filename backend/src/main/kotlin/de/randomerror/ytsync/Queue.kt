package de.randomerror.ytsync

import com.google.gson.Gson
import com.google.gson.JsonParser
import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val videoInfoFetcher: ExecutorService = Executors.newCachedThreadPool()

private val logger = KotlinLogging.logger {}
private val gson = Gson()

private data class VideoInfo(val id: String, val title: String, val extractor: String)

fun enqueue(session: Session, query: String): String {
    val room = getRoom(session)
    videoInfoFetcher.execute {
        val video = fetchVideoInfo(query)
        if (video == null || video.extractor != "youtube") {
            session.remote.sendString("queue err not-found")
            return@execute
        }
        synchronized(room.queue) {
            if (room.queue.any { it.videoId == video.id }) {
                session.remote.sendString("queue err duplicate")
                return@execute
            }
            room.queue.add(QueueItem(video.id, video.title))
        }
        if (room.queue.size == 1) {
            room.broadcastAll("video ${video.id}")
        } else {
            room.broadcastAll("queue add ${video.id} ${video.title}")
        }
    }
    return "queue"
}

private fun fetchVideoInfo(query: String): VideoInfo? {
    val process = Runtime.getRuntime().exec(
        arrayOf("youtube-dl", "--default-search", "ytsearch", "--dump-json", query)
    )
    if (process.waitFor() != 0) {
        logger.warn(process.errorStream.bufferedReader().readText())
        return null
    }
    val videoData = process.inputStream.bufferedReader().readText().trim()
    val video = JsonParser.parseString(videoData).asJsonObject
    val id = video["id"].asString
    val title = video["title"].asString
    val extractor = video["extractor"].asString
    return VideoInfo(id, title, extractor)
}