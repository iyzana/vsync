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
import spark.Spark.init
import spark.Spark.webSocket
import spark.Spark.webSocketIdleTimeoutMillis
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

val gson = Gson()
private val logger = KotlinLogging.logger {}

private const val WEBSOCKET_IDLE_TIMEOUT = 75L
private const val WEBSOCKET_KEEPALIVE = 30L

fun main() {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO

    thread(isDaemon = true) {
        while (true) {
            updateYoutubeDl()
            Thread.sleep(TimeUnit.DAYS.toMillis(1))
        }
    }

    webSocketIdleTimeoutMillis(SECONDS.toMillis(WEBSOCKET_IDLE_TIMEOUT).toInt())
    webSocket("/api/room", SyncWebSocket::class.java)
    init()
}

private fun updateYoutubeDl() {
    logger.info { "checking for yt-dlp updates" }
    val process = Runtime.getRuntime().exec(
        arrayOf("yt-dlp", "--update")
    )
    process.waitFor()
    logger.info {
        process.inputStream.bufferedReader().readLines().joinToString(separator = "\n") { "yt-dlp: $it" }
    }
    logger.warn {
        process.errorStream.bufferedReader().readLines().joinToString(separator = "\n") { "yt-dlp: $it" }
    }
}

private val local = ThreadLocal<String?>()

@WebSocket
class SyncWebSocket {
    private val keepAliveScheduler = Executors.newScheduledThreadPool(1)
    private val keepAliveTasks = mutableMapOf<Session, ScheduledFuture<*>>()

    @OnWebSocketConnect
    fun connected(session: Session) {
        local.set(null)
        log(session, "<connect>")
        val keepaliveTask = keepAliveScheduler.scheduleAtFixedRate({
            session.remote.sendPing(ByteBuffer.allocate(1))
        }, WEBSOCKET_KEEPALIVE, WEBSOCKET_KEEPALIVE, SECONDS)
        keepAliveTasks[session] = keepaliveTask
    }

    @OnWebSocketClose
    fun closed(session: Session, statusCode: Int, reason: String?) {
        log(session, "<$statusCode disconnect> $reason")
        close(session)
        keepAliveTasks.remove(session)!!.cancel(true)
    }

    @OnWebSocketMessage
    @Throws(IOException::class)
    fun message(session: Session, message: String) {
        val cmd = message.trim().split(' ')
        if (cmd.isEmpty()) {
            log(session, "empty command -> <err>")
            kill(session)
            return
        }
        val cmdString = cmd.joinToString(" ")
        local.set(cmdString)
        log(session, "<-")
        try {
            val response = dispatchCommand(cmd, session)
            log(session, "-> $response")
        } catch (_: Disconnect) {
            log(session, "<err>")
            kill(session)
        }
        local.set(null)
    }

    private fun dispatchCommand(cmd: List<String>, session: Session) = when {
        matches(cmd, "create") -> createRoom(session)
        matches(cmd, "join", 1) -> joinRoom(RoomId(cmd[1]), session)
        matches(cmd, "play", 1) -> coordinatePlay(session, cmd[1].asTimeStamp(), isPlaying = true)
        matches(cmd, "pause", 1) -> coordinateClientPause(session, cmd[1].asTimeStamp())
        matches(cmd, "ready", 1) -> clientIsReady(session, cmd[1].asTimeStamp())
        matches(cmd, "sync") -> sync(session)
        matches(cmd, "end", 1) -> setEnded(session, cmd[1])
        matches(cmd, "buffer", 1) -> handleBuffering(session, cmd[1].asTimeStamp())
        matchesMinArgs(cmd, "queue", "add") -> enqueue(session, cmd.subList(2, cmd.size).joinToString(" ").trim())
        matches(cmd, "queue", "rm", 1) -> dequeue(session, cmd[2])
        matches(cmd, "queue", "order", 1) -> reorder(session, cmd[2])
        // args == 2 && command == "speed" -> setSpeed(session, cmd[1].toDouble())
        matches(cmd, "skip") -> skip(session)
        else -> throw Disconnect()
    }

    private fun matches(cmd: List<String>, command: String, args: Int = 0) = (cmd.size - 1) == args && cmd[0] == command

    private fun matches(cmd: List<String>, command: String, subcommand: String, args: Int = 0) =
        (cmd.size - 2) == args && cmd[0] == command && cmd[1] == subcommand

    private fun matchesMinArgs(cmd: List<String>, command: String, subcommand: String, minArgs: Int = 1) =
        (cmd.size - 2) >= minArgs && cmd[0] == command && cmd[1] == subcommand
}

fun log(session: Session, message: String) {
    val roomId = sessions[session]?.let { "@${it.roomId} " } ?: ""
    val context = local.get()?.let { "[$it] " } ?: ""
    logger.info("$roomId${session.remoteAddress.port}: $context$message")
}
