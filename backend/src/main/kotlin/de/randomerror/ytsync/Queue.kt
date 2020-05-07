package de.randomerror.ytsync

import org.eclipse.jetty.websocket.api.Session

fun enqueue(session: Session, videoId: String): String {
    val room = getRoom(session)
    room.queue.add(videoId)
    return if (room.queue.size == 1) {
        room.broadcastAll("video $videoId")
        room.queue.remove(videoId)
        "queue play"
    } else {
        room.broadcastAll("queue add $videoId")
        "queue"
    }
}