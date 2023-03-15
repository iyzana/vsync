package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.time.Instant

private const val SYNC_TIMEOUT = 20L
private const val IGNORE_DURATION = 2L

fun coordinatePlay(session: Session, timestamp: TimeStamp, isPlaying: Boolean = false): String {
    val room = getRoom(session)
    val user = room.getUser(session)
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
    if (room.participants.all { it.syncState.isReadyOrPlayingAt(timestamp) }) {
        room.timeoutSyncAt = null
        if (!room.participants.all { it.syncState.isPlayingAt(timestamp) }) {
            setPlaying(session, room, timestamp)
        }
    } else {
        setPaused(session, room, timestamp)
        room.setSyncState(SyncState.AwaitReady(timestamp))
        room.broadcastActive(session, "ready? ${timestamp.second}")
    }
    return "play"
}

fun kickSlowClients(room: Room) {
    room.participants
        .filter { it.syncState is SyncState.AwaitReady }
        .forEach { kill(it.session, "sync timed out") }
}

fun clientIsReady(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    user.syncState = SyncState.Ready(timestamp)
    if (room.participants.all { it.syncState.isReadyOrPlayingAt(timestamp) }) {
        setPlaying(session, room, timestamp)
    }
    return "ready"
}

private fun setPlaying(session: Session, room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Playing(Instant.now(), timestamp))
    room.ignorePauseTill = null
    room.broadcastActive(session, "play")
}

private fun setPaused(session: Session, room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Paused(timestamp))
    room.ignorePauseTill = Instant.now().plusSeconds(IGNORE_DURATION)
    room.broadcastActive(session, "pause ${timestamp.second}")
}

fun coordinateClientPause(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    if (user.syncState is SyncState.AwaitReady) {
        return "pause ignore ready"
    }
    val ignorePauseTill = room.ignorePauseTill
    if (ignorePauseTill != null && ignorePauseTill.isAfter(Instant.now())) {
        return "pause ignore"
    }
    room.timeoutSyncAt = null
    // this originally did not send a pause event to the client itself
    setPaused(session,room, timestamp)
    return "pause client"
}

fun sync(session: Session): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    if (user.syncState != SyncState.NotStarted) {
        return "sync deny"
    }
    user.syncState = SyncState.Playing(Instant.now(), TimeStamp(0.0))

    val activeMembers = room.participants.filter { it.syncState != SyncState.NotStarted }
    if (activeMembers.size == 1) {
        return "sync go"
    } else {
        return when (val state = activeMembers.find { it.session != session }!!.syncState) {
            is SyncState.Paused -> {
                setPaused(session, room, state.timestamp)
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
                "sync playing"
            }

            is SyncState.NotStarted -> {
                error("sync to unstarted")
            }
        }
    }
}

fun setEnded(session: Session, queueId: String): String {
    val room = getRoom(session)

    synchronized(room.queue) {
        if (room.queue.isEmpty()) return "end empty"
        if (room.queue[0].url != queueId) return "end old"

        room.ignoreSkipTill = Instant.now().plusSeconds(IGNORE_DURATION)
        playNext(session, room)
    }

    return "end"
}

fun playNext(session: Session, room: Room) {
    room.queue.removeAt(0)
    room.timeoutSyncAt = null

    if (room.queue.isNotEmpty()) {
        val next = room.queue[0]
        room.broadcastAll(session, "queue rm ${next.id}")
        room.broadcastAll(session, "video ${next.url}")
        room.setSyncState(SyncState.Playing(Instant.now(), TimeStamp(0.0)))
    } else {
        room.broadcastAll(session, "video")
    }
}

//fun setSpeed(session: Session, speed: Double): String {
//    TODO()
//}

fun handleBuffering(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    if (user.syncState is SyncState.Paused) {
        return "buffer deny"
    }
    if (user.syncState is SyncState.AwaitReady) {
        return "buffer await ready"
    }
    user.syncState = SyncState.Paused(timestamp)
    coordinatePlay(session, timestamp)
    return "buffer"
}

