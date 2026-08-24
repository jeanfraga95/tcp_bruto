package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VpnStats
import com.example.ui.theme.*

@Composable
fun StatsCard(
    stats: VpnStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
        border = BorderStroke(1.dp, HighDensityCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Default.ArrowDownward,
                iconColor = HighDensityEmerald,
                label = "DOWNLOAD",
                value = formatSpeed(stats.speedInBps),
                total = formatBytes(stats.bytesIn)
            )

            HorizontalDivider(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp),
                color = HighDensityCardBorder.copy(alpha = 0.6f)
            )

            StatItem(
                icon = Icons.Default.ArrowUpward,
                iconColor = HighDensityPrimary,
                label = "UPLOAD",
                value = formatSpeed(stats.speedOutBps),
                total = formatBytes(stats.bytesOut)
            )

            HorizontalDivider(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp),
                color = HighDensityCardBorder.copy(alpha = 0.6f)
            )

            StatItem(
                icon = Icons.Default.Speed,
                iconColor = if (stats.pingMs in 0..100) HighDensityEmerald else if (stats.pingMs in 101..250) HighDensityAmber else HighDensityCrimson,
                label = "LATÊNCIA",
                value = if (stats.pingMs >= 0) "${stats.pingMs} ms" else "-- ms",
                total = "RTT Ping"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    total: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HighDensityTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = HighDensityTextPrimary,
                fontSize = 13.sp
            )
        )

        Text(
            text = total,
            style = MaterialTheme.typography.labelSmall.copy(
                color = HighDensityTextMuted,
                fontSize = 10.sp
            )
        )
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        else -> "$bytesPerSec B/s"
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

