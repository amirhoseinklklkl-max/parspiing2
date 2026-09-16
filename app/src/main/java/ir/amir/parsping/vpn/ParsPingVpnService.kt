package ir.amir.parsping.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * A local, no-root VPN whose only purpose is to change the device's DNS servers.
 *
 * Trick: we only add host routes (/32) for the chosen DNS server IPs, not a
 * catch-all 0.0.0.0/0 route. Combined with Builder.addDnsServer(), Android will
 * make the OS resolver send its DNS queries straight to those IPs - and only
 * packets to those IPs are what get pulled into our tun interface. Everything
 * else (normal web/app traffic) keeps flowing over the real network untouched.
 *
 * So all we have to relay here is: read a UDP/53 packet from tun, forward its
 * payload to the real DNS server over a protected socket, and write the reply
 * back into tun wrapped as if it came from that DNS server.
 */
class ParsPingVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val tunWriteMutex = Mutex()
    private var readerJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> {
                val primary = intent?.getStringExtra(EXTRA_PRIMARY) ?: return START_NOT_STICKY
                val secondary = intent.getStringExtra(EXTRA_SECONDARY)
                val name = intent.getStringExtra(EXTRA_NAME) ?: primary
                startVpn(primary, secondary, name)
            }
        }
        return START_STICKY
    }

    private fun startVpn(primary: String, secondary: String?, name: String) {
        stopVpn() // clean any previous session first

        val builder = Builder()
            .setSession("ParsPing")
            .setMtu(1500)
            .addAddress(TUN_ADDRESS, 24)
            .addDnsServer(primary)

        builder.addRoute(primary, 32)
        if (!secondary.isNullOrBlank()) {
            builder.addDnsServer(secondary)
            builder.addRoute(secondary, 32)
        }

        val pfd = try {
            builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN", e)
            null
        }

        if (pfd == null) {
            VpnStatus.setConnected(null)
            return
        }

        vpnInterface = pfd
        VpnStatus.setConnected(name)

        readerJob = serviceScope.launch {
            runTunLoop(pfd, primary, secondary)
        }
    }

    private suspend fun runTunLoop(pfd: ParcelFileDescriptor, primary: String, secondary: String?) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32767)

        while (true) {
            val length = try {
                input.read(buffer)
            } catch (e: Exception) {
                break // interface was closed
            }
            if (length <= 0) continue
            if (!PacketUtils.isIPv4(buffer, length)) continue
            if (PacketUtils.protocol(buffer) != PacketUtils.PROTOCOL_UDP) continue

            val ipHeaderLen = PacketUtils.ipHeaderLength(buffer)
            if (ipHeaderLen + 8 > length) continue

            val dstPort = PacketUtils.udpDstPort(buffer, ipHeaderLen)
            if (dstPort != 53) continue

            val srcPort = PacketUtils.udpSrcPort(buffer, ipHeaderLen)
            val dstIpBytes = PacketUtils.dstIp(buffer)
            val clientIpBytes = PacketUtils.srcIp(buffer)
            val queryPayload = PacketUtils.udpPayload(buffer, ipHeaderLen, length)

            // Copy what we need before launching, since `buffer` is reused on next loop
            val payloadCopy = queryPayload.copyOf()
            val dstIpCopy = dstIpBytes.copyOf()
            val clientIpCopy = clientIpBytes.copyOf()

            serviceScope.launch {
                relayDnsQuery(dstIpCopy, srcPort, clientIpCopy, payloadCopy, output)
            }
        }
    }

    private suspend fun relayDnsQuery(
        dnsServerIp: ByteArray,
        clientPort: Int,
        clientIp: ByteArray,
        query: ByteArray,
        output: FileOutputStream
    ) {
        var socket: DatagramSocket? = null
        try {
            val dnsAddress = InetAddress.getByAddress(dnsServerIp)
            socket = DatagramSocket()
            protect(socket) // send this socket's traffic over the real network, not back into our own tun

            socket.soTimeout = 5000
            socket.send(DatagramPacket(query, query.size, InetSocketAddress(dnsAddress, 53)))

            val replyBuffer = ByteArray(4096)
            val replyPacket = DatagramPacket(replyBuffer, replyBuffer.size)
            socket.receive(replyPacket)

            val responsePayload = replyBuffer.copyOf(replyPacket.length)
            val ipPacket = PacketUtils.buildUdpPacket(
                srcIp = dnsServerIp,
                srcPort = 53,
                dstIp = clientIp,
                dstPort = clientPort,
                payload = responsePayload
            )

            tunWriteMutex.withLock {
                output.write(ipPacket)
            }
        } catch (e: Exception) {
            Log.w(TAG, "DNS relay failed: ${e.message}")
        } finally {
            socket?.close()
        }
    }

    private fun stopVpn() {
        readerJob?.cancel()
        readerJob = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        VpnStatus.setConnected(null)
    }

    override fun onRevoke() {
        // User disabled the VPN from system settings
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ParsPingVpnService"
        private const val TUN_ADDRESS = "10.111.222.1"

        const val ACTION_DISCONNECT = "ir.amir.parsping.DISCONNECT"
        const val EXTRA_PRIMARY = "extra_primary"
        const val EXTRA_SECONDARY = "extra_secondary"
        const val EXTRA_NAME = "extra_name"
    }
}
