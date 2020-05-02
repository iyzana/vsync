package de.randomerror.ytsync

import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.util.*
import kotlin.collections.HashMap

private val random = Random()

private val rooms: MutableMap<RoomId, MutableList<Session>> = HashMap()
private val sessions: MutableMap<Session, RoomId> = HashMap()

fun createRoom(session: Session): String? {
    if (sessions[session] != null) return null
    val roomId = generateRoomId()
    rooms[roomId] = mutableListOf(session)
    sessions[session] = roomId
    return roomId.roomId
}

private fun generateRoomId(): RoomId {
    val bytes = ByteArray(3)
    random.nextBytes(bytes)
    return RoomId(String(Base64.getEncoder().encode(bytes)))
}

fun joinRoom(roomId: RoomId, session: Session): String? {
    if (sessions[session] != null) return null
    val room = rooms[roomId] ?: return null
    room.add(session)
    sessions[session] = roomId
    return "ok"
}

fun messageRoom(session: Session, message: String): String? {
    val roomId = sessions[session] ?: return null
    rooms[roomId]!!.forEach { member ->
        if (member != session) {
            member.remote.sendString(message)
        }
    }
    return "ok"
}

fun close(session: Session) {
    val roomId = sessions.remove(session) ?: return
    val room = rooms[roomId]!!

    room.remove(session)
    if (room.isEmpty()) {
        rooms.remove(roomId)
        log(session, "<close ${roomId.roomId}>")
    }
}

fun kill(session: Session) {
    session.remote.sendString("invalid command")
    session.close(400, "invalid command")
}
