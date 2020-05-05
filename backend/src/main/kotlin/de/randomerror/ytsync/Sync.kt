package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.lang.IllegalStateException
import java.time.Instant
import java.util.*
import kotlin.math.abs

private fun getRoom(session: Session): MutableList<SessionData> {
    val roomId = sessions[session] ?: throw Disconnect()
    return rooms[roomId]!!
}

private fun getSessionData(
    session: Session,
    room: MutableList<SessionData>
) = room.find { it.session == session }!!

fun createRoom(session: Session): String {
    if (sessions[session] != null) throw Disconnect()
    val roomId = generateRoomId()
    val room = mutableListOf(SessionData(session, SyncState.Unstarted))
    rooms[roomId] = room
    sessions[session] = roomId
    session.remote.sendString("create ${roomId.roomId}")
    return "create ${roomId.roomId}"
}

private val random = Random()

private fun generateRoomId(): RoomId {
    val bytes = ByteArray(3)
    random.nextBytes(bytes)
    return RoomId(String(Base64.getUrlEncoder().encode(bytes)))
}

fun joinRoom(roomId: RoomId, session: Session): String {
    if (sessions[session] != null) throw Disconnect()
    val room = rooms[roomId] ?: throw Disconnect()
    room.add(SessionData(session, SyncState.Unstarted))
    sessions[session] = roomId
    return "join ok"
}

fun coordinatePlay(session: Session, timestamp: TimeStamp, isPlaying: Boolean = false): String {
    val room = getRoom(session)
    val sessionData = getSessionData(session, room)
    if (isPlaying && sessionData.state !is SyncState.AwaitReady) {
        sessionData.state = SyncState.Playing(Instant.now(), timestamp)
    }
    if (room.all { isReady(it.state, timestamp) }) {
        setSyncState(room, SyncState.Playing(Instant.now(), timestamp))
        room.forEach { it.ignorePauseTill = null }
        broadcast(room, "play")
    } else {
        coordinateServerPause(room, timestamp)
        setSyncState(room, SyncState.AwaitReady(timestamp))
        broadcast(room, "ready? ${timestamp.second}")
    }
    return "play"
}

fun setReady(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    getSessionData(session, room).state = SyncState.Ready(timestamp)
    if (room.all { isReady(it.state, timestamp) }) {
        setSyncState(room, SyncState.Playing(Instant.now(), timestamp))
        room.forEach { it.ignorePauseTill = null }
        broadcast(room, "play")
    }
    return "ready"
}

private fun isReady(state: SyncState, timestamp: TimeStamp): Boolean {
    return when (state) {
        is SyncState.Ready -> {
            val diff = abs(state.timestamp.second - timestamp.second)
            diff <= 2
        }
        is SyncState.Playing -> {
            val diff = abs(state.timestamp.second - timestamp.second)
            diff <= 2
        }
        is SyncState.Unstarted -> {
            true // ignore unstarted clients
        }
        else -> false
    }
}

fun coordinateClientPause(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    val sessionData = getSessionData(session, room)
    if (sessionData.state is SyncState.AwaitReady) {
        return "pause ignore ready"
    }
    val ignorePauseTill = getSessionData(session, room).ignorePauseTill
    if (ignorePauseTill != null && ignorePauseTill.isAfter(Instant.now())) {
        return "pause ignore"
    }
    setSyncState(room, SyncState.Paused(timestamp))
    room.forEach { it.ignorePauseTill = Instant.now().plusSeconds(1) }
    room.filter { it.state != SyncState.Unstarted }
        .filter { it.session != session }
        .forEach { it.session.remote.sendStringByFuture("pause ${timestamp.second}") }
    return "pause client"
}

private fun coordinateServerPause(room: MutableList<SessionData>, timestamp: TimeStamp) {
    setSyncState(room, SyncState.Paused(timestamp))
    room.forEach { it.ignorePauseTill = Instant.now().plusSeconds(1) }
    broadcast(room, "pause ${timestamp.second}")
}

fun sync(session: Session): String {
    val room = getRoom(session)
    val sessionData = getSessionData(session, room)
    if (sessionData.state != SyncState.Unstarted) {
        return "sync deny"
    }
    sessionData.state = SyncState.Playing(Instant.now(), TimeStamp(0.0))

    val activeMembers = room.filter { it.state != SyncState.Unstarted }
    if (activeMembers.size == 1) {
        return "sync go"
    } else {
        return when (val state = activeMembers.find { it.session != session }!!.state) {
            is SyncState.Paused -> {
                coordinateServerPause(room, state.timestamp)
                "sync pause"
            }
            is SyncState.Ready -> {
                coordinatePlay(session, state.timestamp)
                "sync ready"
            }
            is SyncState.AwaitReady -> {
                coordinatePlay(session, state.timestamp)
                "sync awaitready"
            }
            is SyncState.Playing -> {
                coordinatePlay(session, state.timestamp)
                "sync play"
            }
            is SyncState.Unstarted -> {
                throw IllegalStateException("sync to unstarted")
            }
        }
    }
}

fun handleBuffering(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    val sessionData = getSessionData(session, room)
    if (sessionData.state is SyncState.Paused) {
        return "buffer deny"
    }
    sessionData.state = SyncState.Paused(timestamp)
    coordinatePlay(session, timestamp)
    return "buffer"
}

private fun setSyncState(room: MutableList<SessionData>, state: SyncState) {
    room
        .filter { it.state != SyncState.Unstarted }
        .forEach { it.state = state }
}

private fun broadcast(sessions: MutableList<SessionData>, message: String) {
    log("broadcast $message")
    sessions
        .filter { it.state != SyncState.Unstarted }
        .forEach { member -> member.session.remote.sendStringByFuture(message) }
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
