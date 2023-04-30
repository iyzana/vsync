package de.randomerror.ytsync

import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.websocket.api.Session
import java.util.*
import kotlin.concurrent.thread

private const val ROOM_CLOSE_TIMEOUT_MS = 15L * 1000
private const val ROOM_ID_BYTES = 3

val sessions: MutableMap<Session, RoomId> = HashMap()
val rooms: MutableMap<RoomId, Room> = HashMap()
private val random = Random()

fun getRoom(session: Session): Room {
    val roomId = sessions[session] ?: throw Disconnect()
    return rooms[roomId]!!
}

fun createRoom(session: Session, roomId: RoomId = generateRoomId()): String {
    if (sessions[session] != null) throw Disconnect()
    if (rooms.containsKey(roomId)) throw Disconnect("server full")
    val room = Room(mutableListOf(User(session)))
    rooms[roomId] = room
    sessions[session] = roomId
    session.remote.sendStringByFuture("create ${roomId.roomId}")
    return "create ${roomId.roomId}"
}

private fun generateRoomId(): RoomId {
    val bytes = ByteArray(ROOM_ID_BYTES)
    random.nextBytes(bytes)
    return RoomId(String(Base64.getUrlEncoder().encode(bytes)))
}

fun joinRoom(roomId: RoomId, session: Session): String {
    if (sessions[session] != null) throw Disconnect()
    var room = rooms[roomId]
    if (room == null) {
        createRoom(session, roomId)
        log(session, "create ${roomId.roomId}")
        room = rooms[roomId]!!
    } else {
        room.participants.add(User(session))
        room.shutdownThread?.interrupt()
        room.shutdownThread = null
        sessions[session] = roomId
    }
    room.broadcastAll(session, "users ${room.participants.size}")
    if (room.queue.isNotEmpty()) {
        val playingSource = gson.toJson(room.queue[0].source)
        log(session, "video $playingSource")
        session.remote.sendStringByFuture("video $playingSource")
        for (item in room.queue.drop(1)) {
            val videoJson = gson.toJson(item)
            log(session, "queue add $videoJson")
            session.remote.sendStringByFuture("queue add $videoJson")
        }
    }
    return "join ok"
}

fun close(session: Session) {
    val roomId = sessions.remove(session) ?: return
    val room = rooms[roomId]!!

    room.participants.removeAll { it.session == session }
    room.broadcastAll(session, "users ${room.participants.size}")
    if (room.participants.isEmpty()) {
        scheduleRoomClose(room, roomId, session)
    }
}

private fun scheduleRoomClose(
    room: Room,
    roomId: RoomId,
    session: Session
) {
    room.shutdownThread = thread {
        try {
            Thread.sleep(ROOM_CLOSE_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            // interrupt the shutdown
            return@thread
        }
        rooms.remove(roomId)
        log(session, "<close ${roomId.roomId}>")
    }
}

fun kill(session: Session, reason: String = "invalid command") {
    session.remote.sendStringByFuture(reason)
    session.close(HttpStatus.BAD_REQUEST_400, reason)
}
