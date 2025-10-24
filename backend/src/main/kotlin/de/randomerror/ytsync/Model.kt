package de.randomerror.ytsync

import io.javalin.websocket.WsContext
import java.time.Instant
import java.util.*
import kotlin.math.abs

@JvmInline
value class RoomId(val roomId: String)

data class Room(
    val participants: MutableList<User>,
    val queue: MutableList<QueueItem> = mutableListOf(),
    var shutdownThread: Thread? = null,
    var timeoutSyncAt: Instant? = null,
    var ignorePauseTill: Instant? = null,
    var ignoreSkipTill: Instant? = null,
    var maxConcurrentUsers: Int = 1,
    var numQueuedVideos: Int = 0,
) {
    fun getUser(ws: WsContext) = participants.find { it.ws == ws }!!

    fun broadcastActive(ws: WsContext, message: String) {
        log(ws, "broadcast: $message")
        synchronized(participants) {
            participants
                .filter { it.syncState != SyncState.NotStarted }
                .forEach { member -> member.ws.send(message) }
        }
    }

    fun broadcastAll(ws: WsContext, message: String) {
        log(ws, "broadcast all: $message")
        synchronized(participants) {
            participants
                .forEach { member -> member.ws.send(message) }
        }
    }

    fun setSyncState(state: SyncState) {
        synchronized(participants) {
            participants
                .filter { it.syncState != SyncState.NotStarted }
                .forEach { it.syncState = state }
        }
    }
}

data class VideoSource(
    val url: String,
    val mimeType: String?,
)

data class QueueItem(
    val source: VideoSource?,
    val originalQuery: String,
    val metadata: VideoMetadata?,
    val startTimeSeconds: Int?,
    val thumbnail: String?,
    val favicon: String?,
    val loading: Boolean,
    val id: String = UUID.randomUUID().toString(),
) {
    fun toVideoCommand(): VideoCommand? {
        val source = source ?: return null
        return VideoCommand(source, originalQuery, metadata, startTimeSeconds, favicon)
    }
}

data class VideoMetadata(
    val title: String?,
    val series: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val channel: String?,
)

data class VideoCommand(
    val source: VideoSource,
    val originalQuery: String,
    val metadata: VideoMetadata?,
    val startTimeSeconds: Int?,
    val favicon: String?,
)

data class User(
    val ws: WsContext,
    var syncState: SyncState = SyncState.NotStarted
)

class Disconnect(message: String = "invalid command") : RuntimeException(message)

@JvmInline
value class TimeStamp(val second: Double) {
    companion object {
        val ZERO: TimeStamp = TimeStamp(0.0)
    }

    constructor(second: Int) : this(second.toDouble())
}

fun String.asTimeStamp(): TimeStamp {
    return TimeStamp(toDouble())
}

sealed class SyncState {
    object NotStarted : SyncState()
    class Paused(val timestamp: TimeStamp = TimeStamp.ZERO) : SyncState()
    class AwaitReady(val timestamp: TimeStamp) : SyncState()
    class Ready(val timestamp: TimeStamp) : SyncState()
    class Playing(
        private val startTime: Instant,
        private val videoTimestamp: TimeStamp
    ) : SyncState() {
        val timestamp: TimeStamp
            get() {
                val timePlaying = Instant.now().epochSecond - startTime.epochSecond
                val currentTime = videoTimestamp.second + timePlaying
                return TimeStamp(currentTime)
            }
    }

    fun isReadyOrPlayingAt(timestamp: TimeStamp): Boolean {
        return when (this) {
            is Ready -> {
                val diff = abs(this.timestamp.second - timestamp.second)
                diff <= SYNC_THRESHOLD
            }

            is Playing -> {
                val diff = abs(this.timestamp.second - timestamp.second)
                diff <= SYNC_THRESHOLD
            }

            is NotStarted -> {
                true // ignore unstarted clients
            }

            else -> false
        }
    }

    fun isPlayingAt(timestamp: TimeStamp): Boolean {
        return when (this) {
            is Playing -> {
                val diff = abs(this.timestamp.second - timestamp.second)
                diff <= SYNC_THRESHOLD
            }

            is NotStarted -> {
                true // ignore unstarted clients
            }

            else -> false
        }
    }

}

private const val SYNC_THRESHOLD = 1.5

