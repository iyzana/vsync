package de.randomerror.ytsync

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.Javalin
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

val gson = Gson()
private val logger = KotlinLogging.logger {}

private const val LISTEN_PORT = 4567
private const val WEBSOCKET_KEEPALIVE = 20L
private const val WEBSOCKET_IDLE_TIMEOUT = 30L

fun main() {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.INFO
    // (LoggerFactory.getLogger("org.eclipse.jetty") as Logger).level = Level.INFO

    thread(isDaemon = true) {
        while (true) {
            updateYoutubeDl()
            Thread.sleep(TimeUnit.DAYS.toMillis(1))
        }
    }

    Javalin.create()
        .ws("/api/room") { ws -> webSocket(ws) }
        .start("::", LISTEN_PORT)
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
    val stderr = process.errorStream.bufferedReader().readLines().joinToString(separator = "\n") { "yt-dlp: $it" }
    if (stderr != "") {
        logger.warn { stderr }
    }
}

private val local = ThreadLocal<String?>()

private fun webSocket(ws: WsConfig) {

    ws.onConnect { ctx ->
        local.set(null)
        log(ctx, "<connect>")
        ctx.enableAutomaticPings(WEBSOCKET_KEEPALIVE, SECONDS)
        ctx.session.idleTimeout = Duration.ofSeconds(WEBSOCKET_IDLE_TIMEOUT)
    }

    ws.onClose { ctx ->
        log(ctx, "<${ctx.closeStatus()} disconnect> ${ctx.reason()}")
        close(ctx)
    }

    ws.onError { ctx ->
        log(ctx, "<error ${ctx.error()}>")
        close(ctx)
    }

    ws.onMessage { ctx ->
        val cmd = ctx.message().trim().split(' ')
        if (cmd.isEmpty()) {
            log(ctx, "empty command -> <err>")
            kill(ctx)
            return@onMessage
        }
        val cmdString = cmd.joinToString(" ")
        local.set(cmdString)
        log(ctx, "<-")
        try {
            val response = dispatchCommand(cmd, ctx)
            log(ctx, "-> $response")
        } catch (_: Disconnect) {
            log(ctx, "<err>")
            kill(ctx)
        }
        local.set(null)
    }
}

private fun dispatchCommand(cmd: List<String>, ws: WsContext) = when {
    matches(cmd, "create") -> createRoom(ws)
    matches(cmd, "join", 1) -> joinRoom(RoomId(cmd[1]), ws)
    matches(cmd, "play", 1) -> coordinatePlay(ws, cmd[1].asTimeStamp(), isPlaying = true)
    matches(cmd, "pause", 1) -> coordinateClientPause(ws, cmd[1].asTimeStamp())
    matches(cmd, "ready", 1) -> clientIsReady(ws, cmd[1].asTimeStamp())
    matches(cmd, "sync") -> sync(ws)
    matches(cmd, "end", 1) -> setEnded(ws, cmd[1])
    matches(cmd, "buffer", 1) -> handleBuffering(ws, cmd[1].asTimeStamp())
    matchesMinArgs(cmd, "queue", "add") -> enqueue(ws, cmd.subList(2, cmd.size).joinToString(" ").trim())
    matches(cmd, "queue", "rm", 1) -> dequeue(ws, cmd[2])
    matches(cmd, "queue", "order", 1) -> reorder(ws, cmd[2])
    // args == 2 && command == "speed" -> setSpeed(session, cmd[1].toDouble())
    matches(cmd, "skip") -> skip(ws)
    else -> throw Disconnect()
}

private fun matches(cmd: List<String>, command: String, args: Int = 0) = (cmd.size - 1) == args && cmd[0] == command

private fun matches(cmd: List<String>, command: String, subcommand: String, args: Int = 0) =
    (cmd.size - 2) == args && cmd[0] == command && cmd[1] == subcommand

private fun matchesMinArgs(cmd: List<String>, command: String, subcommand: String, minArgs: Int = 1) =
    (cmd.size - 2) >= minArgs && cmd[0] == command && cmd[1] == subcommand

fun log(ws: WsContext, message: String) {
    val roomId = websockets[ws]?.let { "@${it.roomId} " } ?: ""
    val context = local.get()?.let { "[$it] " } ?: ""
    logger.debug { "$roomId${ws.session.remoteAddress}: $context$message" }
}
