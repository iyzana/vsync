package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.time.Instant
import kotlin.math.abs

inline class TimeStamp(val second: Double)

fun String.asTimeStamp(): TimeStamp {
    return TimeStamp(toDouble())
}

sealed class SyncState {
    object NotStarted : SyncState()
    class Paused(val timestamp: TimeStamp = TimeStamp(0.0)) : SyncState()
    class AwaitReady(val timestamp: TimeStamp) : SyncState()
    class Ready(val timestamp: TimeStamp) : SyncState()
    class Playing(
        private val startTime: Instant,
        private val videoTimestamp: TimeStamp
    ) : SyncState() {
        val timestamp: TimeStamp
            get() {
                // todo: calculate playback-speed
                val timePlaying = Instant.now().epochSecond - startTime.epochSecond
                val currentTime = videoTimestamp.second + timePlaying
                return TimeStamp(currentTime)
            }
    }
}

fun coordinatePlay(session: Session, timestamp: TimeStamp, isPlaying: Boolean = false): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    if (user.syncState is SyncState.NotStarted || user.syncState is SyncState.Paused) {
        room.timeoutSyncAt = Instant.now().plusSeconds(20)
    }
    val timeout = room.timeoutSyncAt
    if (timeout != null && Instant.now() > timeout) {
        kickSlowClients(room)
    }
    if (isPlaying && user.syncState !is SyncState.AwaitReady) {
        user.syncState = SyncState.Playing(Instant.now(), timestamp)
    }
    if (room.participants.all { isReadyOrPlaying(it.syncState, timestamp) }) {
        room.timeoutSyncAt = null
        if (!room.participants.all { isPlaying(it.syncState, timestamp) }) {
            startPlay(session, room, timestamp)
        }
    } else if (user.syncState !is SyncState.AwaitReady) {
        coordinateServerPause(session, room, timestamp)
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

fun setReady(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    user.syncState = SyncState.Ready(timestamp)
    if (room.participants.all { isReadyOrPlaying(it.syncState, timestamp) }) {
        startPlay(session, room, timestamp)
    }
    return "ready"
}

private fun startPlay(session: Session, room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Playing(Instant.now(), timestamp))
    room.ignorePauseTill = null
    room.broadcastActive(session, "play")
}

private fun isReadyOrPlaying(state: SyncState, timestamp: TimeStamp): Boolean {
    return when (state) {
        is SyncState.Ready -> {
            val diff = abs(state.timestamp.second - timestamp.second)
            diff <= 1.5
        }
        is SyncState.Playing -> {
            val diff = abs(state.timestamp.second - timestamp.second)
            diff <= 1.5
        }
        is SyncState.NotStarted -> {
            true // ignore unstarted clients
        }
        else -> false
    }
}

private fun isPlaying(state: SyncState, timestamp: TimeStamp): Boolean {
    return when (state) {
        is SyncState.Playing -> {
            val diff = abs(state.timestamp.second - timestamp.second)
            diff <= 1.5
        }
        is SyncState.NotStarted -> {
            true // ignore unstarted clients
        }
        else -> false
    }
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
    room.setSyncState(SyncState.Paused(timestamp))
    ignoreUpcomingPause(room)
    room.participants
        .filter { it.syncState != SyncState.NotStarted }
        .filter { it.session != session }
        .forEach { it.session.remote.sendStringByFuture("pause ${timestamp.second}") }
    return "pause client"
}

private fun coordinateServerPause(session: Session, room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Paused(timestamp))
    ignoreUpcomingPause(room)
    room.broadcastActive(session, "pause ${timestamp.second}")
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
                coordinateServerPause(session, room, state.timestamp)
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
                throw IllegalStateException("sync to unstarted")
            }
        }
    }
}

fun setEnded(session: Session, videoId: String): String {
    val room = getRoom(session)
    val user = room.getUser(session)

    synchronized(room.queue) {
        val ignoreEndTill = room.ignoreEndTill
        if (ignoreEndTill != null && ignoreEndTill.isAfter(Instant.now())) {
            return "end ignore"
        }

        if (room.queue.isEmpty()) return "end empty"
        if (room.queue[0].id != videoId) return "end old"

        room.ignoreEndTill = Instant.now().plusSeconds(2)
        playNext(session, room)
    }

    return "end"
}

fun playNext(session: Session, room: Room) {
    room.queue.removeAt(0)

    if (room.queue.isNotEmpty()) {
        val next = room.queue[0]
        room.broadcastAll(session, "queue rm ${next.id}")
        room.broadcastAll(session, "video ${next.id}")
    }
}

private fun ignoreUpcomingPause(room: Room) {
    room.ignorePauseTill = Instant.now().plusSeconds(2)
}

fun setSpeed(session: Session, speed: Double): String {
    TODO()
}

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

private fun Room.setSyncState(state: SyncState) {
    participants
        .filter { it.syncState != SyncState.NotStarted }
        .forEach { it.syncState = state }
}

