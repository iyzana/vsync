package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

val sessions: MutableMap<Session, RoomId> = HashMap()
val rooms: MutableMap<RoomId, Room> = HashMap()
private val random = Random()

inline class RoomId(val roomId: String)

data class Room(
    val participants: MutableList<User>,
    val queue: MutableList<QueueItem> = mutableListOf(),
    var shutdownThread: Thread? = null,
    var timeoutSyncAt: Instant? = null,
    var ignorePauseTill: Instant? = null,
    var ignoreEndTill: Instant? = null
)

data class QueueItem(
    val id: String,
    var title: String,
    var thumbnail: String?
)

data class User(
    val session: Session,
    var syncState: SyncState = SyncState.NotStarted
)

fun getRoom(session: Session): Room {
    val roomId = sessions[session] ?: throw Disconnect()
    return rooms[roomId]!!
}

fun Room.getUser(
    session: Session
) = participants.find { it.session == session }!!

fun Room.broadcastActive(session: Session, message: String) {
    log(session, "broadcast: $message")
    participants
        .filter { it.syncState != SyncState.NotStarted }
        .forEach { member -> member.session.remote.sendStringByFuture(message) }
}

fun Room.broadcastAll(session: Session, message: String) {
    log(session, "broadcast all: $message")
    participants
        .forEach { member -> member.session.remote.sendStringByFuture(message) }
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
    val bytes = ByteArray(3)
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
        val playingId = room.queue[0].id
        log(session, "video $playingId")
        session.remote.sendStringByFuture("video $playingId")
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
            Thread.sleep(15 * 1000)
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
    session.close(400, reason)
}
