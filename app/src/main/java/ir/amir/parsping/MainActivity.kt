package ir.amir.parsping

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener
import ir.amir.parsping.data.DefaultDnsList
import ir.amir.parsping.data.DnsRepository
import ir.amir.parsping.model.DnsCategory
import ir.amir.parsping.model.DnsServer
import ir.amir.parsping.ping.PingUtil
import ir.amir.parsping.ui.MainScreen
import ir.amir.parsping.ui.theme.ParsPingTheme
import ir.amir.parsping.vpn.ParsPingVpnService
import ir.amir.parsping.vpn.VpnStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var dnsRepository: DnsRepository
    private var pendingConnectDns: DnsServer? = null

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val dns = pendingConnectDns
            pendingConnectDns = null
            if (result.resultCode == RESULT_OK && dns != null) {
                showRewardedThenConnect(dns)
            } else {
                VpnStatus.setBusy(false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dnsRepository = DnsRepository(this)
        Adivery.prepareAppOpenAd(this, AdiveryIds.APP_OPEN_PLACEMENT_ID)
        showAppOpenAdOnLaunch()

        setContent {
            ParsPingTheme {
                ParsPingApp(
                    dnsRepository = dnsRepository,
                    onRequestConnect = { dns -> startConnectFlow(dns) },
                    onDisconnect = { disconnectVpn() }
                )
            }
        }
    }

    private fun showAppOpenAdOnLaunch() {
        val placementId = AdiveryIds.APP_OPEN_PLACEMENT_ID
        if (Adivery.isLoaded(placementId)) {
            Adivery.showAppOpenAd(this, placementId)
        } else {
            Adivery.addPlacementListener(placementId, object : AdiveryListener() {
                override fun onAppOpenAdLoaded(placementId: String) {
                    Adivery.showAppOpenAd(this@MainActivity, placementId)
                    Adivery.removePlacementListener(placementId)
                }

                override fun onError(placementId: String, reason: String) {
                    Adivery.removePlacementListener(placementId)
                }
            })
        }
    }

    private fun startConnectFlow(dns: DnsServer) {
        VpnStatus.setBusy(true)
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            pendingConnectDns = dns
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            showRewardedThenConnect(dns)
        }
    }

    private fun showRewardedThenConnect(dns: DnsServer) {
        val placementId = AdiveryIds.REWARDED_PLACEMENT_ID

        if (!Adivery.isLoaded(placementId)) {
            VpnStatus.setBusy(false)
            Toast.makeText(this, "تبلیغ آماده نیست، چند لحظه دیگر دوباره تلاش کنید", Toast.LENGTH_SHORT).show()
            return
        }

        Adivery.addPlacementListener(placementId, object : AdiveryListener() {
            override fun onRewardedAdClosed(placementId: String, isRewarded: Boolean) {
                Adivery.removePlacementListener(placementId)
                if (isRewarded) {
                    connectVpn(dns)
                } else {
                    VpnStatus.setBusy(false)
                    Toast.makeText(
                        this@MainActivity,
                        "برای اتصال باید تبلیغ را تا انتها ببینید",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onError(placementId: String, reason: String) {
                Adivery.removePlacementListener(placementId)
                VpnStatus.setBusy(false)
                Toast.makeText(this@MainActivity, "نمایش تبلیغ با خطا مواجه شد", Toast.LENGTH_SHORT).show()
            }
        })

        Adivery.showAd(placementId)
    }

    private fun connectVpn(dns: DnsServer) {
        dnsRepository.saveSelectedDnsId(dns.id)
        val intent = Intent(this, ParsPingVpnService::class.java).apply {
            putExtra(ParsPingVpnService.EXTRA_PRIMARY, dns.primary)
            putExtra(ParsPingVpnService.EXTRA_SECONDARY, dns.secondary)
            putExtra(ParsPingVpnService.EXTRA_NAME, dns.name)
        }
        startService(intent)
        VpnStatus.setBusy(false)
    }

    private fun disconnectVpn() {
        val intent = Intent(this, ParsPingVpnService::class.java).apply {
            action = ParsPingVpnService.ACTION_DISCONNECT
        }
        startService(intent)
    }
}

@Composable
private fun ParsPingApp(
    dnsRepository: DnsRepository,
    onRequestConnect: (DnsServer) -> Unit,
    onDisconnect: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var customDns by remember { mutableStateOf(dnsRepository.loadCustomDns()) }
    var selectedId by remember { mutableStateOf(dnsRepository.loadSelectedDnsId()) }
    val pings = remember { mutableStateMapOf<String, Long?>() }
    var pingLoadingIds by remember { mutableStateOf(setOf<String>()) }

    val isConnected by VpnStatus.isConnected.collectAsState()
    val connectedName by VpnStatus.connectedDnsName.collectAsState()
    val isBusy by VpnStatus.isBusy.collectAsState()

    val dnsGroups = remember(customDns) {
        linkedMapOf(
            DnsCategory.GENERAL to DefaultDnsList.general,
            DnsCategory.GAMING to DefaultDnsList.gaming,
            DnsCategory.ANTI_CENSORSHIP to DefaultDnsList.antiCensorship,
            DnsCategory.CUSTOM to customDns
        )
    }

    fun refreshPings() {
        val all = dnsGroups.values.flatten()
        all.forEach { dns ->
            pingLoadingIds = pingLoadingIds + dns.id
            scope.launch {
                val result = PingUtil.measure(dns.primary)
                pings[dns.id] = result
                pingLoadingIds = pingLoadingIds - dns.id
            }
        }
    }

    LaunchedEffect(Unit) { refreshPings() }

    MainScreen(
        dnsGroups = dnsGroups,
        selectedId = selectedId,
        pings = pings,
        pingLoadingIds = pingLoadingIds,
        isConnected = isConnected,
        connectedName = connectedName,
        connecting = isBusy,
        onSelect = { dns ->
            selectedId = dns.id
            dnsRepository.saveSelectedDnsId(dns.id)
        },
        onConnectClick = {
            dnsGroups.values.flatten().find { it.id == selectedId }?.let(onRequestConnect)
        },
        onDisconnectClick = onDisconnect,
        onAddCustom = { name, primary, secondary ->
            val newDns = DnsServer(
                id = "custom-${System.currentTimeMillis()}",
                name = name,
                primary = primary,
                secondary = secondary,
                category = DnsCategory.CUSTOM,
                isCustom = true
            )
            dnsRepository.addCustomDns(newDns)
            customDns = dnsRepository.loadCustomDns()
            pingLoadingIds = pingLoadingIds + newDns.id
            scope.launch {
                val result = PingUtil.measure(newDns.primary)
                pings[newDns.id] = result
                pingLoadingIds = pingLoadingIds - newDns.id
            }
        },
        onRemoveCustom = { id ->
            dnsRepository.removeCustomDns(id)
            customDns = dnsRepository.loadCustomDns()
            if (selectedId == id) {
                selectedId = null
                dnsRepository.saveSelectedDnsId(null)
            }
        },
        onRefreshPings = { refreshPings() }
    )
}
