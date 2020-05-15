package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session
import java.time.Instant
import kotlin.math.abs

inline class TimeStamp(val second: Double)

fun String.asTimeStamp(): TimeStamp {
    return TimeStamp(toDouble())
}

sealed class SyncState {
    object Unstarted : SyncState()
    class Paused(val timestamp: TimeStamp = TimeStamp(0.0)) : SyncState()
    class AwaitReady(val timestamp: TimeStamp) : SyncState()
    class Ready(val timestamp: TimeStamp) : SyncState()
    class Playing(
        private val realStartTime: Instant,
        private val originalTimestamp: TimeStamp
    ) : SyncState() {
        val timestamp: TimeStamp
            get() {
                // todo: calculate playback-speed
                val timePlaying = Instant.now().epochSecond - realStartTime.epochSecond
                val currentTime = originalTimestamp.second + timePlaying
                return TimeStamp(currentTime)
            }
    }
}

fun coordinatePlay(session: Session, timestamp: TimeStamp, isPlaying: Boolean = false): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    if (isPlaying && user.syncState !is SyncState.AwaitReady) {
        user.syncState = SyncState.Playing(Instant.now(), timestamp)
    }
    if (room.participants.all { isReady(it.syncState, timestamp) }) {
        startPlay(room, timestamp)
    } else {
        coordinateServerPause(room, timestamp)
        room.setSyncState(SyncState.AwaitReady(timestamp))
        room.broadcastActive("ready? ${timestamp.second}")
    }
    return "play"
}

fun setReady(session: Session, timestamp: TimeStamp): String {
    val room = getRoom(session)
    room.getUser(session).syncState = SyncState.Ready(timestamp)
    if (room.participants.all { isReady(it.syncState, timestamp) }) {
        startPlay(room, timestamp)
    }
    return "ready"
}

private fun startPlay(room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Playing(Instant.now(), timestamp))
    room.participants.forEach { it.ignorePauseTill = null }
    room.broadcastActive("play")
}

private fun isReady(state: SyncState, timestamp: TimeStamp): Boolean {
    return when (state) {
        is SyncState.Ready -> {
            val diff = abs(state.timestamp.second - timestamp.second)
            diff <= 1.5
        }
        is SyncState.Playing -> {
            val diff = abs(state.timestamp.second - timestamp.second)
            diff <= 1.5
        }
        is SyncState.Unstarted -> {
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
    val ignorePauseTill = user.ignorePauseTill
    if (ignorePauseTill != null && ignorePauseTill.isAfter(Instant.now())) {
        return "pause ignore"
    }
    room.setSyncState(SyncState.Paused(timestamp))
    ignoreUpcomingPause(room)
    room.participants
        .filter { it.syncState != SyncState.Unstarted }
        .filter { it.session != session }
        .forEach { it.session.remote.sendStringByFuture("pause ${timestamp.second}") }
    return "pause client"
}

private fun coordinateServerPause(room: Room, timestamp: TimeStamp) {
    room.setSyncState(SyncState.Paused(timestamp))
    ignoreUpcomingPause(room)
    room.broadcastActive("pause ${timestamp.second}")
}

fun sync(session: Session): String {
    val room = getRoom(session)
    val user = room.getUser(session)
    if (user.syncState != SyncState.Unstarted) {
        return "sync deny"
    }
    user.syncState = SyncState.Playing(Instant.now(), TimeStamp(0.0))

    val activeMembers = room.participants.filter { it.syncState != SyncState.Unstarted }
    if (activeMembers.size == 1) {
        return "sync go"
    } else {
        return when (val state = activeMembers.find { it.session != session }!!.syncState) {
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

fun setEnded(session: Session, videoId: String): String {
    val room = getRoom(session)
    val user = room.getUser(session)

    val ignoreEndTill = user.ignoreEndTill
    if (ignoreEndTill != null && ignoreEndTill.isAfter(Instant.now())) {
        return "end ignore"
    }

    if (room.queue.isEmpty()) return "end empty"
    if (room.queue[0].videoId != videoId) return "end old"
    room.queue.removeAt(0)

    if (room.queue.isNotEmpty()) {
        room.participants.forEach { it.ignoreEndTill = Instant.now().plusSeconds(2) }
        val next = room.queue[0]
        room.broadcastAll("queue rm ${next.videoId}")
        room.broadcastAll("video ${next.videoId}")
    }
    return "end"
}

private fun ignoreUpcomingPause(room: Room) {
    room.participants.forEach { it.ignorePauseTill = Instant.now().plusSeconds(2) }
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
        .filter { it.syncState != SyncState.Unstarted }
        .forEach { it.syncState = state }
}

