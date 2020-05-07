package de.randomerror.ytsync

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import mu.KotlinLogging
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.slf4j.LoggerFactory
import spark.Spark.init
import spark.Spark.webSocket
import java.io.IOException

private val logger = KotlinLogging.logger {}

fun main() {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO

    webSocket("/room", SyncWebSocket::class.java)
    init()
}

@WebSocket
class SyncWebSocket {
    @OnWebSocketConnect
    fun connected(session: Session) {
        log(session, "<connect>")
    }

    @OnWebSocketClose
    fun closed(session: Session, statusCode: Int, reason: String?) {
        log(session, "<disconnect>")
        close(session)
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
                cmd.size == 2 && cmd[0] == "play" -> {
                    coordinatePlay(session, TimeStamp(cmd[1].toDouble()), isPlaying = true)
                }
                cmd.size == 2 && cmd[0] == "pause" -> {
                    coordinateClientPause(session, TimeStamp(cmd[1].toDouble()))
                }
                cmd.size == 2 && cmd[0] == "ready" -> {
                    setReady(session, TimeStamp(cmd[1].toDouble()))
                }
                cmd.size == 1 && cmd[0] == "sync" -> {
                    sync(session)
                }
                cmd.size == 2 && cmd[0] == "buffer" -> {
                    handleBuffering(session, TimeStamp(cmd[1].toDouble()))
                }
                else -> throw Disconnect()
            }
            log(session, "/ $cmdString -> $response")
        } catch (e: Disconnect) {
            log(session, "/ $cmdString -> <err>")
            kill(session)
        }
    }
}

fun log(message: String) {
    logger.info(message)
}

fun log(session: Session, message: String) {
    logger.info(session.remoteAddress.toString() + ": " + message)
}
