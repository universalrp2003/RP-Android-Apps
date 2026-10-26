package com.universalrp.emergency

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * Listens for alerts from the other phones.
 *
 * A single blocking receive loop on a background thread. The socket is bound to the same fixed
 * port the broadcaster sends to, which is the only reason the replies to a ping come back to the
 * right place.
 *
 * A timeout is set so the loop can notice it has been asked to stop; a listener that only checks
 * a flag inside a blocking read keeps the thread, and the socket, alive for a minute after the
 * user pressed stop.
 *
 * The wire protocol is three plain-text packet kinds, chosen so that a JVM unit test can parse
 * them without `org.json` and so a log dump is readable:
 * - `RPEMERGENCY1|name|severity|tamil|message|confidence|millis` - a real alert
 * - `RPEMERGENCY-PING|model` - who is out there?
 * - `RPEMERGENCY-PONG|name` - I am here, and this is my name
 */
class AlertListener(private val onAlert: (EmergencyAlert) -> Unit) : Thread("alert-listener") {

    @Volatile
    private var running = true

    @Volatile
    private var socket: DatagramSocket? = null

    override fun run() {
        val buffer = ByteArray(AlertBroadcaster.MAX_PACKET)
        while (running) {
            val active = try {
                val created = DatagramSocket(null)
                created.reuseAddress = true
                created.broadcast = true
                created.soTimeout = SOCKET_TIMEOUT_MS
                created.bind(InetSocketAddress(EmergencyAlert.PORT))
                socket = created
                created
            } catch (error: Exception) {
                // Usually "address already in use" while the broadcaster still holds the port.
                // Wait and try again rather than dying, because the broadcaster may be closing.
                runCatching { Thread.sleep(RETRY_MS) }
                continue
            }
            try {
                while (running) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        active.receive(packet)
                    } catch (error: SocketTimeoutException) {
                        continue
                    } catch (error: IOException) {
                        break
                    }
                    handle(String(packet.data, packet.offset, packet.length, Charsets.UTF_8))
                }
            } catch (error: Exception) {
                // An unexpected failure on one socket must not end listening for good.
                runCatching { Thread.sleep(RETRY_MS) }
            } finally {
                runCatching { active.close() }
                socket = null
            }
        }
    }

    private fun handle(text: String) {
        when {
            text.startsWith(PING_REQUEST) -> {
                // Somebody is asking who is here. Answer so their screen can list this phone.
                AlertBroadcaster.sendPong(DeviceNames.thisPhone())
            }
            text.startsWith(PING_REPLY) -> {
                // A reply to our own ping. The name the other phone uses is after the marker.
                val name = text.substringAfter('|', "").trim()
                if (name.isNotEmpty()) EmergencyState.addSource(name)
            }
            else -> {
                val alert = EmergencyAlert.decode(text) ?: return
                // Ignore our own broadcast coming back off a hub or a repeater.
                if (alert.sourceName == DeviceNames.thisPhone()) return
                onAlert(alert)
            }
        }
    }

    fun shutdown() {
        running = false
        runCatching { socket?.close() }
        interrupt()
    }

    companion object {
        private const val SOCKET_TIMEOUT_MS = 1_000
        private const val RETRY_MS = 2_000L

        /** "Who is out there?" */
        const val PING_REQUEST = "RPEMERGENCY-PING|"

        /** "I am here, and this is my name." */
        const val PING_REPLY = "RPEMERGENCY-PONG|"
    }
}
