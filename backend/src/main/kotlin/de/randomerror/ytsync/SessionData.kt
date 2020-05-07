package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.time.Instant

val rooms: MutableMap<RoomId, MutableList<SessionData>> = HashMap()
val sessions: MutableMap<Session, RoomId> = HashMap()

inline class RoomId(val roomId: String)

class SessionData(
    val session: Session,
    var state: SyncState,
    var ignorePauseTill: Instant? = null
)

sealed class SyncState {
    object Unstarted : SyncState()
    class Paused(val timestamp: TimeStamp = TimeStamp(0.0)) : SyncState()
    class AwaitReady(val timestamp: TimeStamp) : SyncState()
    class Ready(val timestamp: TimeStamp) : SyncState()
    class Playing(
        private val realStartTime: Instant,
        private val originalTimestamp: TimeStamp
    ) : SyncState() {
        val timestamp: TimeStamp
            get() {
                // todo: calculate playback-speed
                val timePlaying = Instant.now().epochSecond - realStartTime.epochSecond
                val currentTime = originalTimestamp.second + timePlaying
                return TimeStamp(currentTime)
            }
    }
}

inline class TimeStamp(val second: Double)
