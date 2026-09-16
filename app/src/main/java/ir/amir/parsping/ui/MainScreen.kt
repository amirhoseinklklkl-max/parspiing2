package ir.amir.parsping.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.parsping.model.DnsCategory
import ir.amir.parsping.model.DnsServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dnsGroups: Map<DnsCategory, List<DnsServer>>,
    selectedId: String?,
    pings: Map<String, Long?>,
    pingLoadingIds: Set<String>,
    isConnected: Boolean,
    connectedName: String?,
    connecting: Boolean,
    onSelect: (DnsServer) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onAddCustom: (name: String, primary: String, secondary: String?) -> Unit,
    onRemoveCustom: (String) -> Unit,
    onRefreshPings: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val selectedDns = dnsGroups.values.flatten().find { it.id == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پارس پینگ") },
                actions = {
                    IconButton(onClick = onRefreshPings) {
                        Icon(Icons.Filled.Refresh, contentDescription = "بروزرسانی پینگ‌ها")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن DNS")
            }
        },
        bottomBar = {
            ConnectBar(
                selectedDns = selectedDns,
                isConnected = isConnected,
                connectedName = connectedName,
                connecting = connecting,
                onConnectClick = onConnectClick,
                onDisconnectClick = onDisconnectClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            dnsGroups.forEach { (category, list) ->
                if (list.isEmpty()) return@forEach

                item {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                items(list, key = { it.id }) { dns ->
                    DnsCard(
                        dns = dns,
                        selected = dns.id == selectedId,
                        pingMs = pings[dns.id],
                        pingLoading = pingLoadingIds.contains(dns.id),
                        onSelect = { onSelect(dns) },
                        onRemove = if (dns.isCustom) {
                            { onRemoveCustom(dns.id) }
                        } else null,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddDnsDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, primary, secondary ->
                onAddCustom(name, primary, secondary)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ConnectBar(
    selectedDns: DnsServer?,
    isConnected: Boolean,
    connectedName: String?,
    connecting: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    isConnected -> "متصل به: ${connectedName ?: ""}"
                    selectedDns != null -> "انتخاب شده: ${selectedDns.name}"
                    else -> "یک DNS را انتخاب کنید"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = if (isConnected) onDisconnectClick else onConnectClick,
                enabled = (selectedDns != null || isConnected) && !connecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when {
                        connecting -> "در حال اتصال..."
                        isConnected -> "قطع اتصال"
                        else -> "اتصال"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
