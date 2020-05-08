package de.randomerror.ytsync

import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

val sessions: MutableMap<Session, RoomId> = HashMap()
val rooms: MutableMap<RoomId, Room> = HashMap()
private val random = Random()
private val logger = KotlinLogging.logger {}

inline class RoomId(val roomId: String)

data class Room(
    val participants: MutableList<User>,
    val queue: MutableList<QueueItem> = mutableListOf(),
    var shutdownThread: Thread? = null
)

data class QueueItem(
    val videoId: String,
    var title: String? = null
)

data class User(
    val session: Session,
    var syncState: SyncState = SyncState.Unstarted,
    var ignorePauseTill: Instant? = null
)

fun getRoom(session: Session): Room {
    val roomId = sessions[session] ?: throw Disconnect()
    return rooms[roomId]!!
}

fun Room.getUser(
    session: Session
) = participants.find { it.session == session }!!

fun Room.broadcastActive(message: String) {
    logger.info("broadcast $message")
    participants
        .filter { it.syncState != SyncState.Unstarted }
        .forEach { member -> member.session.remote.sendStringByFuture(message) }
}

fun Room.broadcastAll(message: String) {
    logger.info("broadcast all $message")
    participants
        .forEach { member -> member.session.remote.sendStringByFuture(message) }
}

fun createRoom(session: Session): String {
    if (sessions[session] != null) throw Disconnect()
    val roomId = generateRoomId()
    if (rooms.containsKey(roomId)) throw Disconnect("server full")
    val room = Room(mutableListOf(User(session)))
    rooms[roomId] = room
    sessions[session] = roomId
    session.remote.sendString("create ${roomId.roomId}")
    return "create ${roomId.roomId}"
}

private fun generateRoomId(): RoomId {
    val bytes = ByteArray(3)
    random.nextBytes(bytes)
    return RoomId(String(Base64.getUrlEncoder().encode(bytes)))
}

fun joinRoom(roomId: RoomId, session: Session): String {
    if (sessions[session] != null) throw Disconnect()
    val room = rooms[roomId] ?: throw Disconnect()
    room.participants.add(User(session))
    room.shutdownThread?.interrupt()
    room.shutdownThread = null
    sessions[session] = roomId
    if (room.queue.isNotEmpty()) {
        session.remote.sendString("video ${room.queue[0].videoId}")
        for (item in room.queue.drop(1)) {
            session.remote.sendString("queue add ${item.videoId} ${item.title}")
        }
    }
    return "join ok"
}

fun close(session: Session) {
    val roomId = sessions.remove(session) ?: return
    val room = rooms[roomId]!!

    room.participants.removeAll { it.session == session }
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

fun kill(session: Session) {
    session.remote.sendString("invalid command")
    session.close(400, "invalid command")
}
