package ir.amir.parsping.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.amir.parsping.model.DnsServer
import ir.amir.parsping.ui.theme.PingBad
import ir.amir.parsping.ui.theme.PingGood
import ir.amir.parsping.ui.theme.PingMedium

@Composable
fun DnsCard(
    dns: DnsServer,
    selected: Boolean,
    pingMs: Long?,
    pingLoading: Boolean,
    onSelect: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = dns.name, style = MaterialTheme.typography.titleMedium)
                val subtitle = if (dns.secondary != null) "${dns.primary} , ${dns.secondary}" else dns.primary
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PingBadge(pingMs = pingMs, loading = pingLoading)

            if (onRemove != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "حذف")
                }
            }
        }
    }
}

@Composable
private fun PingBadge(pingMs: Long?, loading: Boolean) {
    val (color, text) = when {
        loading -> MaterialTheme.colorScheme.onSurfaceVariant to "..."
        pingMs == null -> PingBad to "✕"
        pingMs < 200 -> PingGood to "${pingMs}ms"
        else -> PingMedium to "${pingMs}ms"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}
