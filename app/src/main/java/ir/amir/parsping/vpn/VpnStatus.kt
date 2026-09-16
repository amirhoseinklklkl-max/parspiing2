package ir.amir.parsping.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple app-wide observable connection state, so Compose UI can react to the
 * VpnService starting/stopping without needing a bound service or broadcasts.
 */
object VpnStatus {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectedDnsName = MutableStateFlow<String?>(null)
    val connectedDnsName: StateFlow<String?> = _connectedDnsName

    // True while the ad-watch / VPN-permission flow triggered by "connect" is in progress,
    // so the UI can show a busy state on the connect button.
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    fun setConnected(name: String?) {
        _connectedDnsName.value = name
        _isConnected.value = name != null
    }

    fun setBusy(busy: Boolean) {
        _isBusy.value = busy
    }
}
