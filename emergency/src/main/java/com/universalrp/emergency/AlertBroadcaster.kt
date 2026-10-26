package com.universalrp.emergency

import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

/**
 * Sends an emergency to every other phone on the same Wi-Fi, without any server and without
 * internet.
 *
 * The mechanism is deliberately the oldest one there is: a UDP broadcast on the local subnet. No
 * cloud, no account, no pairing screen, and nothing stops working when the router loses its
 * upstream connection, which is exactly when a family needs it to work.
 *
 * The broadcast address is derived from the Wi-Fi interface rather than guessed as 255.255.255.255,
 * because many routers drop the global broadcast while passing the directed one. The socket is
 * created once and reused; a new socket per alert would race with the socket being closed while a
 * send is in flight.
 */
object AlertBroadcaster {

    private const val TAG = "AlertBroadcaster"

    @Volatile
    private var socket: DatagramSocket? = null

    /** Broadcast address of the Wi-Fi link, or null if there is not one. */
    fun broadcastAddress(): InetAddress? {
        return try {
            for (network in NetworkInterface.getNetworkInterfaces()) {
                if (network.isLoopback || !network.isUp) continue
                for (address in network.interfaceAddresses) {
                    val broadcast = address.broadcast ?: continue
                    val bytes = broadcast.address
                    // Skip link-local and the unspecified address; neither is routable on a home
                    // network and a send to either just fails.
                    if (bytes == null || bytes.size != 4) continue
                    if (bytes[0] == 127.toByte()) continue
                    if (bytes.all { it == 0.toByte() }) continue
                    return broadcast
                }
            }
            null
        } catch (error: Exception) {
            null
        }
    }

    private fun ensureSocket(): DatagramSocket {
        val existing = socket
        if (existing != null && !existing.isClosed) return existing
        synchronized(this) {
            val again = socket
            if (again != null && !again.isClosed) return again
            val created = DatagramSocket(null)
            created.reuseAddress = true
            created.broadcast = true
            // Binding to the fixed port is what lets a listener on another phone answer on the
            // same port, and what makes the "is anybody there" reply arrive at all.
            created.bind(InetSocketAddress(EmergencyAlert.PORT))
            socket = created
            return created
        }
    }

    /**
     * Sends one alert. Returns the number of packets actually written.
     *
     * Never throws. A failure to reach anybody is a fact the caller may show, not an exception
     * that kills the service.
     */
    fun send(alert: EmergencyAlert): Int {
        val target = broadcastAddress() ?: return 0
        val payload = alert.encode().toByteArray(Charsets.UTF_8)
        if (payload.size > MAX_PACKET) return 0
        var sent = 0
        return try {
            val socket = ensureSocket()
            // Three copies, because UDP on a shared Wi-Fi loses packets and this is not a stream
            // where a retransmit is expected. Three is enough to survive one loss without turning
            // the airwaves into a flood every time a doorbell rings.
            repeat(SEND_COPIES) {
                val packet = DatagramPacket(payload, payload.size, target, EmergencyAlert.PORT)
                socket.send(packet)
                sent++
            }
            sent
        } catch (error: IOException) {
            closeSocket()
            0
        } catch (error: Exception) {
            closeSocket()
            0
        }
    }

    /**
     * Asks the other phones to say who they are.
     *
     * The reply is what fills the "connected phones" list. Without it the app can only ever show
     * the phones that happened to have an emergency, which is a poor way to tell somebody whether
     * the feature is working at all.
     */
    fun ping(): Boolean = sendText(AlertListener.PING_REQUEST + android.os.Build.MODEL)

    /** Answers a ping, so the asking phone can list this one. */
    fun sendPong(name: String): Boolean = sendText(AlertListener.PING_REPLY + name)

    private fun sendText(text: String): Boolean {
        val target = broadcastAddress() ?: return false
        val payload = text.toByteArray(Charsets.UTF_8)
        if (payload.size > MAX_PACKET) return false
        return try {
            ensureSocket().send(DatagramPacket(payload, payload.size, target, EmergencyAlert.PORT))
            true
        } catch (error: Exception) {
            closeSocket()
            false
        }
    }

    fun closeSocket() {
        synchronized(this) {
            try {
                socket?.close()
            } catch (error: Exception) {
                // Nothing useful can be done about a socket that will not close.
            }
            socket = null
        }
    }

    /** UDP payloads are capped well below the theoretical maximum; alerts are short. */
    const val MAX_PACKET = 1_400

    const val SEND_COPIES = 3

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    init {
        // Keep a reference so the object is never collected while the service holds it.
        debugLog("broadcaster ready")
    }
}
