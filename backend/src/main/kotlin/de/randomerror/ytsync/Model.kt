package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
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
    var ignoreSkipTill: Instant? = null
) {
    fun getUser(session: Session) = participants.find { it.session == session }!!

    fun broadcastActive(session: Session, message: String) {
        log(session, "broadcast: $message")
        participants
            .filter { it.syncState != SyncState.NotStarted }
            .forEach { member -> member.session.remote.sendStringByFuture(message) }
    }

    fun broadcastAll(session: Session, message: String) {
        log(session, "broadcast all: $message")
        participants
            .forEach { member -> member.session.remote.sendStringByFuture(message) }
    }

    fun setSyncState(state: SyncState) {
        participants
            .filter { it.syncState != SyncState.NotStarted }
            .forEach { it.syncState = state }
    }
}

data class VideoSource(
    val url: String,
    val mimeType: String?,
)

data class QueueItem(
    val source: VideoSource,
    val originalQuery: String,
    val title: String?,
    val thumbnail: String?,
    val id: String = UUID.nameUUIDFromBytes(source.url.toByteArray()).toString(),
)

data class User(
    val session: Session,
    var syncState: SyncState = SyncState.NotStarted
)

class Disconnect(message: String = "invalid command") : RuntimeException(message)

@JvmInline
value class TimeStamp(val second: Double)

fun String.asTimeStamp(): TimeStamp {
    return TimeStamp(toDouble())
}

sealed class SyncState {
    object NotStarted : SyncState()
    class Paused(val timestamp: TimeStamp = TimeStamp(0.0)) : SyncState()
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

