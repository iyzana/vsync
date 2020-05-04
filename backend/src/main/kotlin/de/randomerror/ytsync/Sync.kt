package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.lang.IllegalStateException
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

private val random = Random()

inline class TimeStamp(val timestamp: Double)

sealed class SyncState {
    object Unstarted : SyncState()
    class Paused(val timestamp: TimeStamp = TimeStamp(0.0)) : SyncState()
    class Ready(val timestamp: TimeStamp) : SyncState()
    class Playing(val start: Instant, val timestamp: TimeStamp) : SyncState()
}

class SessionState(
        val session: Session,
        var state: SyncState,
        var ignorePauseTill: Instant? = null
)

private val rooms: MutableMap<RoomId, MutableList<SessionState>> = HashMap()
private val sessions: MutableMap<Session, RoomId> = HashMap()

fun createRoom(session: Session): String? {
    if (sessions[session] != null) return null
    val roomId = generateRoomId()
    rooms[roomId] = mutableListOf(SessionState(session, SyncState.Unstarted))
    sessions[session] = roomId
    return "create ${roomId.roomId}"
}

private fun generateRoomId(): RoomId {
    val bytes = ByteArray(3)
    random.nextBytes(bytes)
    return RoomId(String(Base64.getUrlEncoder().encode(bytes)))
}

fun joinRoom(roomId: RoomId, session: Session): String? {
    if (sessions[session] != null) return null
    val room = rooms[roomId] ?: return null
    room.add(SessionState(session, SyncState.Unstarted))
    sessions[session] = roomId
    return "join ok"
}

fun setReady(session: Session, timestamp: TimeStamp): String? {
    val roomId = sessions[session] ?: return null
    val room = rooms[roomId]!!
    room.find { it.session == session }!!.state = SyncState.Ready(timestamp)
    if (room.all { isReady(it.state, timestamp) }) {
        room.forEach {
            it.state = SyncState.Playing(
                    Instant.now(),
                    timestamp
            )
        }
        broadcastRoom(room, "play") ?: return null
    }
    return "ok ready"
}

fun coordinatePlay(session: Session, timestamp: TimeStamp): String? {
    val roomId = sessions[session] ?: return null
    val room = rooms[roomId]!!
    if (room.all { isReady(it.state, timestamp) }) {
        room.filter { it.state != SyncState.Unstarted }.forEach {
            it.state = SyncState.Playing(
                    Instant.now(),
                    timestamp
            )
        }
        broadcastRoom(room, "play") ?: return null
    } else {
        coordinateServerPause(room, timestamp) ?: return null
        broadcastRoom(room, "ready? ${timestamp.timestamp}") ?: return null
    }
    return "ok play"
}

fun isReady(state: SyncState, timestamp: TimeStamp): Boolean {
    return when (state) {
        is SyncState.Ready -> {
            val diff = abs(state.timestamp.timestamp - timestamp.timestamp)
            diff <= 2
        }
        is SyncState.Playing -> {
            val timePlaying = state.start.epochSecond - Instant.now().epochSecond
            val playTimestamp = state.timestamp.timestamp + timePlaying
            val diff = abs(playTimestamp - timestamp.timestamp)
            diff <= 2
        }
        is SyncState.Unstarted -> {
            true
        }
        else -> false
    }
}

fun coordinateClientPause(session: Session, timestamp: TimeStamp): String? {
    val roomId = sessions[session] ?: return null
    val room = rooms[roomId]!!
    val ignorePauseTill = room.find { it.session == session }!!.ignorePauseTill
    if (ignorePauseTill != null && ignorePauseTill.isAfter(Instant.now())) {
        return "ok ignore pause"
    }
    room.forEach { it.state = SyncState.Paused(timestamp) }
    return broadcastRoom(room, "pause ${timestamp.timestamp}")
}

fun coordinateServerPause(room: MutableList<SessionState>, timestamp: TimeStamp): String? {
    room.forEach {
        it.state = SyncState.Paused(timestamp)
        it.ignorePauseTill = Instant.now().plusSeconds(1)
    }
    return broadcastRoom(room, "pause ${timestamp.timestamp}")
}

fun sync(session: Session): String? {
    val roomId = sessions[session] ?: return null
    val room = rooms[roomId]!!
    val sessionState = room.find { it.session == session }!!
    sessionState.state = SyncState.Playing(Instant.now(), TimeStamp(0.0))

    val activeMembers = room.filter { it.state != SyncState.Unstarted }
    if (activeMembers.size == 1) {
        return "ok sync go"
    } else {
        return when (val state = activeMembers.find { it.session != session }!!.state) {
            is SyncState.Paused -> {
                coordinateServerPause(room, state.timestamp)
                "ok sync pause"
            }
            is SyncState.Ready -> {
                coordinatePlay(session, state.timestamp)
                "ok sync ready"
            }
            is SyncState.Playing -> {
                val timePlaying = Instant.now().epochSecond - state.start.epochSecond
                val playTimestamp = state.timestamp.timestamp + timePlaying
                coordinatePlay(session, TimeStamp(playTimestamp))
                "ok sync play"
            }
            is SyncState.Unstarted -> {
                throw IllegalStateException("sync to unstarted")
            }
        }
    }
}

fun handleBuffering(session: Session, timestamp: TimeStamp): String? {
    val roomId = sessions[session] ?: return null
    val room = rooms[roomId]!!
    val sessionState = room.find { it.session == session }!!
    sessionState.state = SyncState.Paused(timestamp)
    return coordinatePlay(session, timestamp)
}

fun broadcastRoom(session: Session, message: String): String? {
    val roomId = sessions[session] ?: return null
    return broadcastRoom(rooms[roomId]!!, message)
}

fun broadcastRoom(sessions: MutableList<SessionState>, message: String): String? {
    log("broadcast $message")
    sessions.filter { it.state != SyncState.Unstarted }.forEach { member ->
        member.session.remote.sendStringByFuture(message)
    }
    return "ok $message"
}

fun close(session: Session) {
    val roomId = sessions.remove(session) ?: return
    val room = rooms[roomId]!!

    room.removeAll { it.session == session }
    if (room.isEmpty()) {
        rooms.remove(roomId)
        log(session, "<close ${roomId.roomId}>")
    }
}

fun kill(session: Session) {
    session.remote.sendString("invalid command")
    session.close(400, "invalid command")
}
