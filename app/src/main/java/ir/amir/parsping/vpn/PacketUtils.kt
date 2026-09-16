package ir.amir.parsping.vpn

import java.net.InetAddress

/**
 * Minimal IPv4 + UDP packet parsing/building helpers.
 *
 * We only need to handle DNS traffic (UDP port 53) here: the VpnService is
 * configured so that only packets addressed to the chosen DNS server IPs
 * are routed into our tun interface in the first place, so we don't need a
 * full generic IP router - just enough parsing to pull out a DNS query and
 * enough building to send back a DNS response wrapped in IPv4 + UDP.
 */
object PacketUtils {

    const val PROTOCOL_UDP = 17

    fun isIPv4(buffer: ByteArray, length: Int): Boolean {
        if (length < 20) return false
        val version = (buffer[0].toInt() shr 4) and 0xF
        return version == 4
    }

    fun ipHeaderLength(buffer: ByteArray): Int = (buffer[0].toInt() and 0xF) * 4

    fun protocol(buffer: ByteArray): Int = buffer[9].toInt() and 0xFF

    fun srcIp(buffer: ByteArray): ByteArray = buffer.copyOfRange(12, 16)

    fun dstIp(buffer: ByteArray): ByteArray = buffer.copyOfRange(16, 20)

    fun udpSrcPort(buffer: ByteArray, ipHeaderLen: Int): Int =
        ((buffer[ipHeaderLen].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLen + 1].toInt() and 0xFF)

    fun udpDstPort(buffer: ByteArray, ipHeaderLen: Int): Int =
        ((buffer[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLen + 3].toInt() and 0xFF)

    fun udpPayload(buffer: ByteArray, ipHeaderLen: Int, totalLength: Int): ByteArray {
        val udpHeaderLen = 8
        val start = ipHeaderLen + udpHeaderLen
        if (start >= totalLength) return ByteArray(0)
        return buffer.copyOfRange(start, totalLength)
    }

    fun ipToBytes(address: String): ByteArray = InetAddress.getByName(address).address

    /**
     * Builds a raw IPv4 + UDP packet: source = (srcIp:srcPort), destination = (dstIp:dstPort),
     * carrying [payload]. Used to hand a DNS response back into the tun interface, addressed
     * as if it came directly from the DNS server the client thinks it queried.
     */
    fun buildUdpPacket(
        srcIp: ByteArray,
        srcPort: Int,
        dstIp: ByteArray,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)

        // --- IPv4 header ---
        packet[0] = 0x45 // version 4, header length 5 (no options)
        packet[1] = 0x00
        packet[2] = ((totalLength shr 8) and 0xFF).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = 0x00 // identification
        packet[5] = 0x00
        packet[6] = 0x40 // flags: don't fragment
        packet[7] = 0x00
        packet[8] = 64 // TTL
        packet[9] = PROTOCOL_UDP.toByte()
        packet[10] = 0x00 // checksum placeholder
        packet[11] = 0x00
        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)

        val checksum = ipChecksum(packet, 0, 20)
        packet[10] = ((checksum shr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        // --- UDP header ---
        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()
        packet[24] = ((udpLength shr 8) and 0xFF).toByte()
        packet[25] = (udpLength and 0xFF).toByte()
        packet[26] = 0x00 // UDP checksum = 0 (optional for IPv4, valid per RFC 768)
        packet[27] = 0x00

        // --- payload ---
        System.arraycopy(payload, 0, packet, 28, payload.size)

        return packet
    }

    private fun ipChecksum(buffer: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val word = ((buffer[i].toInt() and 0xFF) shl 8) or (buffer[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        sum = (sum and 0xFFFF) + (sum ushr 16)
        sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv() and 0xFFFF
    }
}
