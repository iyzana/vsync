package de.randomerror.ytsync

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.gson.Gson
import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.slf4j.LoggerFactory
import spark.Spark.*
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

val gson = Gson()
private val logger = KotlinLogging.logger {}

fun main() {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO

    webSocket("/room", SyncWebSocket::class.java)
    init()
}

@WebSocket
class SyncWebSocket {
    private val keepaliveScheduler = Executors.newScheduledThreadPool(1)
    private val keepaliveTasks = mutableMapOf<Session, ScheduledFuture<*>>()

    @OnWebSocketConnect
    fun connected(session: Session) {
        log(session, "<connect>")
        val keepaliveTask = keepaliveScheduler.scheduleAtFixedRate({
            log(session, "<ping>")
            session.remote.sendPing(ByteBuffer.allocate(1))
        }, 30, 30, TimeUnit.SECONDS)
        keepaliveTasks[session] = keepaliveTask
    }

    @OnWebSocketClose
    fun closed(session: Session, statusCode: Int, reason: String?) {
        log(session, "<$statusCode disconnect> $reason")
        close(session)
        keepaliveTasks.remove(session)!!.cancel(true)
    }

    @OnWebSocketMessage
    @Throws(IOException::class)
    fun message(session: Session, message: String) {
        val cmd = message.trim().split(' ')
        val cmdString = cmd.joinToString(" ")
        log(session, "\\ $cmdString")
        try {
            val response = when {
                cmd.size == 1 && cmd[0] == "create" -> createRoom(session)
                cmd.size == 2 && cmd[0] == "join" -> joinRoom(RoomId(cmd[1]), session)
                cmd.size == 2 && cmd[0] == "play" -> coordinatePlay(session, cmd[1].asTimeStamp(), isPlaying = true)
                cmd.size == 2 && cmd[0] == "pause" -> coordinateClientPause(session, cmd[1].asTimeStamp())
                cmd.size == 2 && cmd[0] == "ready" -> setReady(session, cmd[1].asTimeStamp())
                cmd.size == 1 && cmd[0] == "sync" -> sync(session)
                cmd.size == 2 && cmd[0] == "end" -> setEnded(session, cmd[1])
                cmd.size == 2 && cmd[0] == "buffer" -> handleBuffering(session, cmd[1].asTimeStamp())
                cmd.size >= 3 && cmd[0] == "queue" && cmd[1] == "add" ->
                    enqueue(session, cmd.subList(2, cmd.size).joinToString(" "))
                cmd.size == 3 && cmd[0] == "queue" && cmd[1] == "rm" -> dequeue(session, cmd[2])
                cmd.size == 3 && cmd[0] == "queue" && cmd[1] == "order" -> reorder(session, cmd[2])
                cmd.size == 2 && cmd[0] == "speed" -> setSpeed(session, cmd[1].toDouble())
                cmd.size == 1 && cmd[0] == "skip" -> skip(session)
                else -> throw Disconnect()
            }
            log(session, "/ $cmdString -> $response")
        } catch (e: Disconnect) {
            log(session, "/ $cmdString -> <err>")
            kill(session)
        }
    }
}

class Disconnect(message: String = "invalid command") : RuntimeException(message)

fun log(session: Session, message: String) {
    logger.info(session.remoteAddress.toString() + ": " + message)
}
