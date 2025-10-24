package de.randomerror.ytsync

import io.javalin.websocket.WsContext
import java.time.Instant

private const val SYNC_TIMEOUT = 10L
private const val IGNORE_DURATION = 2L

fun coordinatePlay(ws: WsContext, timestamp: TimeStamp, isPlaying: Boolean = false): String {
    val room = getRoom(ws)
    val user = room.getUser(ws)
    if (room.timeoutSyncAt == null) {
        room.timeoutSyncAt = Instant.now().plusSeconds(SYNC_TIMEOUT)
    }
    val timeout = room.timeoutSyncAt
    if (timeout != null && Instant.now() > timeout) {
        kickSlowClients(room)
    }
    if (isPlaying && user.syncState !is SyncState.AwaitReady) {
        user.syncState = SyncState.Playing(Instant.now(), timestamp)
    }
    synchronized(room.participants) {
        if (room.participants.all { it.syncState.isReadyOrPlayingAt(timestamp) }) {
            room.timeoutSyncAt = null
            if (!room.participants.all { it.syncState.isPlayingAt(timestamp) }) {
                setPlaying(ws, room, timestamp)
            }
        } else {
            setPaused(ws, room, timestamp)
            room.setSyncState(SyncState.AwaitReady(timestamp))
            room.broadcastActive(ws, "ready? ${timestamp.second}")
        }
    }
    return "play"
}

fun kickSlowClients(room: Room) {
    room.participants
        .filter { it.syncState is SyncState.AwaitReady }
        .forEach { kill(it.ws, "sync timed out") }
}

fun clientIsReady(ws: WsContext, timestamp: TimeStamp): String {
    val room = getRoom(ws)
    val user = room.getUser(ws)
    user.syncState = SyncState.Ready(timestamp)
    if (room.participants.all { it.syncState.isReadyOrPlayingAt(timestamp) }) {
        setPlaying(ws, room, timestamp)
    }
    return "ready"
}

private fun setPlaying(ws: WsContext, room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Playing(Instant.now(), timestamp))
    room.ignorePauseTill = null
    room.broadcastActive(ws, "play")
}

private fun setPaused(ws: WsContext, room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Paused(timestamp))
    room.ignorePauseTill = Instant.now().plusSeconds(IGNORE_DURATION)
    room.broadcastActive(ws, "pause ${timestamp.second}")
}

fun coordinateClientPause(ws: WsContext, timestamp: TimeStamp): String {
    val room = getRoom(ws)
    val user = room.getUser(ws)
    if (user.syncState is SyncState.AwaitReady) {
        return "pause ignore ready"
    }
    val ignorePauseTill = room.ignorePauseTill
    if (ignorePauseTill != null && ignorePauseTill.isAfter(Instant.now())) {
        return "pause ignore"
    }
    room.timeoutSyncAt = null
    // this originally did not send a pause event to the client itself
    setPaused(ws,room, timestamp)
    return "pause client"
}

fun sync(ws: WsContext): String {
    val room = getRoom(ws)
    val user = room.getUser(ws)
    if (user.syncState != SyncState.NotStarted) {
        return "sync deny"
    }

    val timestamp = if (room.queue.isEmpty()) {
        TimeStamp.ZERO
    } else {
        val video = room.queue[0]
        video.startTimeSeconds?.let(::TimeStamp) ?: TimeStamp.ZERO
    }
    user.syncState = SyncState.Playing(Instant.now(), timestamp)
    val activeMembers = room.participants.filter { it.syncState != SyncState.NotStarted }
    if (activeMembers.size == 1) {
        return "sync go"
    } else {
        return when (val state = activeMembers.find { it.ws != ws }!!.syncState) {
            is SyncState.Paused -> {
                setPaused(ws, room, state.timestamp)
                "sync pause"
            }

            is SyncState.Ready -> {
                coordinatePlay(ws, state.timestamp)
                "sync ready"
            }

            is SyncState.AwaitReady -> {
                coordinatePlay(ws, state.timestamp)
                "sync awaitready"
            }

            is SyncState.Playing -> {
                coordinatePlay(ws, state.timestamp)
                "sync playing"
            }

            is SyncState.NotStarted -> {
                error("sync to unstarted")
            }
        }
    }
}

fun setEnded(ws: WsContext, videoUrl: String): String {
    val room = getRoom(ws)

    synchronized(room.queue) {
        if (room.queue.isEmpty()) return "end empty"
        if (room.queue[0].source?.url != videoUrl) return "end old"

        room.ignoreSkipTill = Instant.now().plusSeconds(IGNORE_DURATION)
        playNext(ws, room)
    }

    return "end"
}

fun playNext(ws: WsContext, room: Room) {
    room.queue.removeAt(0)
    room.timeoutSyncAt = null

    if (room.queue.isNotEmpty()) {
        val next = room.queue[0]
        room.broadcastAll(ws, "queue rm ${next.id}")
        room.broadcastAll(ws, "video ${gson.toJson(next.toVideoCommand())}")
        room.setSyncState(SyncState.Playing(Instant.now(), TimeStamp(next.startTimeSeconds?.toDouble() ?: 0.0)))
    } else {
        room.broadcastAll(ws, "video")
    }
}

//fun setSpeed(ws: WsContext, speed: Double): String {
//    TODO()
//}

fun handleBuffering(ws: WsContext, timestamp: TimeStamp): String {
    val room = getRoom(ws)
    val user = room.getUser(ws)
    if (user.syncState is SyncState.Paused) {
        return "buffer deny"
    }
    if (user.syncState is SyncState.AwaitReady) {
        return "buffer await ready"
    }
    user.syncState = SyncState.Paused(timestamp)
    coordinatePlay(ws, timestamp)
    return "buffer"
}

