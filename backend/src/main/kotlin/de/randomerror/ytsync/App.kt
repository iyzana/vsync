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
import spark.Spark.*
import java.io.IOException

private val logger = KotlinLogging.logger {}

inline class RoomId(val roomId: String)

fun main() {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO

    webSocket("/roo", SyncWebSocket::class.java)
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
        val response = when {
            cmd.size == 1 && cmd[0] == "create" -> createRoom(session)
            cmd.size == 2 && cmd[0] == "join" -> joinRoom(RoomId(cmd[1]), session)
            cmd.size == 1 && cmd[0] == "play" -> messageRoom(session, "play")
            cmd.size == 1 && cmd[0] == "pause" -> messageRoom(session, "pause")
            else -> {
                kill(session)
                null
            }
        }
        if (response != null) {
            log(session, cmd.joinToString(" ") + " -> " + response)
            session.remote.sendString(response)
        } else {
            log(session, cmd.joinToString(" ") + " -> <err>")
            kill(session)
        }
    }
}

fun log(session: Session, message: String) {
    logger.info(session.remoteAddress.toString() + ": " + message)
}
