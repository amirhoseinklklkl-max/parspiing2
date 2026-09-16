package ir.amir.parsping.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Measures latency to a DNS server by timing a raw TCP connect to port 53.
 * This works without root and without ICMP permissions, and reflects real
 * reachability of the DNS server better than a ping to a random host would.
 */
object PingUtil {

    suspend fun measure(host: String, port: Int = 53, timeoutMs: Int = 1500): Long? =
        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                val start = System.nanoTime()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val elapsedMs = (System.nanoTime() - start) / 1_000_000
                elapsedMs
            } catch (e: Exception) {
                null
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }
        }
}
