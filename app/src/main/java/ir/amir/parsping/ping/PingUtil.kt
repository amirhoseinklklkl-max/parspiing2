package ir.amir.parsping.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

/**
 * Measures latency to a DNS server by sending a real DNS query over UDP
 * (the actual protocol/port DNS resolution uses) and timing the round trip
 * to the first response byte.
 *
 * We deliberately do NOT use a TCP connect() for this: many public DNS
 * providers route/prioritize UDP:53 differently (and faster) than TCP:53
 * on their anycast networks, which made the old TCP-based measurement read
 * ~100ms higher than real-world ping tools for the same server. A genuine
 * UDP DNS round trip reflects the actual path a device's DNS lookups take,
 * so the numbers line up with independent ping tests instead of diverging
 * from them.
 */
object PingUtil {

    suspend fun measure(host: String, timeoutMs: Int = 1500): Long? =
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = timeoutMs

                val address = InetAddress.getByName(host)
                val query = buildDnsQuery()

                val start = System.nanoTime()
                socket.send(DatagramPacket(query, query.size, address, 53))

                val replyBuffer = ByteArray(512)
                val replyPacket = DatagramPacket(replyBuffer, replyBuffer.size)
                socket.receive(replyPacket) // any reply (even SERVFAIL/NXDOMAIN) confirms round trip

                (System.nanoTime() - start) / 1_000_000
            } catch (e: Exception) {
                null
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }
        }

    /** Builds a minimal, valid DNS query (A record for example.com) with a random transaction ID. */
    private fun buildDnsQuery(): ByteArray {
        val id = Random.nextInt(0, 65536)
        val header = byteArrayOf(
            (id shr 8).toByte(), (id and 0xFF).toByte(), // transaction ID
            0x01, 0x00, // flags: standard query, recursion desired
            0x00, 0x01, // QDCOUNT = 1
            0x00, 0x00, // ANCOUNT
            0x00, 0x00, // NSCOUNT
            0x00, 0x00  // ARCOUNT
        )
        val question = encodeQName("example.com") + byteArrayOf(0x00, 0x01, 0x00, 0x01) // TYPE A, CLASS IN
        return header + question
    }

    private fun encodeQName(name: String): ByteArray {
        val out = mutableListOf<Byte>()
        name.split(".").forEach { label ->
            out.add(label.length.toByte())
            out.addAll(label.toByteArray(Charsets.US_ASCII).toList())
        }
        out.add(0) // root terminator
        return out.toByteArray()
    }
}
