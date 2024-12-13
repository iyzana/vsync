package de.randomerror.ytsync

import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.websocket.WsCloseStatus
import io.javalin.websocket.WsContext
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.websocket.api.CloseStatus
import java.util.*
import kotlin.concurrent.thread

private const val ROOM_CLOSE_TIMEOUT_MS = 15L * 1000
private const val ROOM_ID_BYTES = 3

private val logger = KotlinLogging.logger {}

val websockets: MutableMap<WsContext, RoomId> = HashMap()
val rooms: MutableMap<RoomId, Room> = HashMap()
private val random = Random()

fun getRoom(ws: WsContext): Room {
    val roomId = websockets[ws] ?: throw Disconnect()
    return rooms[roomId]!!
}

fun createRoom(ws: WsContext, roomId: RoomId = generateRoomId()): String {
    if (websockets[ws] != null) throw Disconnect()
    if (rooms.containsKey(roomId)) throw Disconnect("server full")
    val room = Room(mutableListOf(User(ws)))
    if (rooms.isEmpty()) {
        logger.info { "active rooms" }
    }
    rooms[roomId] = room
    websockets[ws] = roomId
    ws.send("create ${roomId.roomId}")
    return "create ${roomId.roomId}"
}

private fun generateRoomId(): RoomId {
    val bytes = ByteArray(ROOM_ID_BYTES)
    random.nextBytes(bytes)
    return RoomId(String(Base64.getUrlEncoder().encode(bytes)))
}

fun joinRoom(roomId: RoomId, ws: WsContext): String {
    if (websockets[ws] != null) throw Disconnect()
    var room = rooms[roomId]
    if (room == null) {
        createRoom(ws, roomId)
        log(ws, "create ${roomId.roomId}")
        room = rooms[roomId]!!
    } else {
        synchronized(room.participants) {
            room.participants.add(User(ws))
        }
        room.shutdownThread?.interrupt()
        room.shutdownThread = null
        websockets[ws] = roomId
        if (room.participants.size > room.maxConcurrentUsers) {
            room.maxConcurrentUsers = room.participants.size
        }
    }
    room.broadcastAll(ws, "users ${room.participants.size}")
    if (room.queue.isNotEmpty()) {
        val playingSource = gson.toJson(room.queue[0].source)
        log(ws, "video $playingSource")
        ws.send("video $playingSource")
        for (item in room.queue.drop(1)) {
            val videoJson = gson.toJson(item)
            log(ws, "queue add $videoJson")
            ws.send("queue add $videoJson")
        }
    }
    return "join ok"
}

fun close(ws: WsContext) {
    val roomId = websockets.remove(ws) ?: return
    val room = rooms[roomId]!!

    synchronized(room.participants) {
        room.participants.removeAll { it.ws == ws }
    }
    room.broadcastAll(ws, "users ${room.participants.size}")
    if (room.participants.isEmpty()) {
        scheduleRoomClose(room, roomId, ws)
    }
}

private fun scheduleRoomClose(
    room: Room,
    roomId: RoomId,
    ws: WsContext
) {
    room.shutdownThread = thread {
        try {
            Thread.sleep(ROOM_CLOSE_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            // interrupt the shutdown
            return@thread
        }
        rooms.remove(roomId)
        log(ws, "<close ${roomId.roomId}>")
        if (roomId.roomId != "test") {
            logger.info { "room statistic: ${room.maxConcurrentUsers} users, ${room.numQueuedVideos} videos" }
        }
        if (rooms.isEmpty()) {
            logger.info { "no more active rooms" }
        }
    }
}

fun kill(ws: WsContext, reason: String = "invalid command") {
    log(ws, "<killing: ${reason}>")
    ws.send(reason)
    ws.closeSession(WsCloseStatus.POLICY_VIOLATION, reason)
}
