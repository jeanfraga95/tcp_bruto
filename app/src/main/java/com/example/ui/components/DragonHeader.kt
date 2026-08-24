package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VpnState
import com.example.ui.theme.*

@Composable
fun DragonHeader(
    vpnState: VpnState,
    pingMs: Long,
    onOpenProfiles: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = HighDensitySurface,
        border = BorderStroke(1.dp, HighDensityCardBorder.copy(alpha = 0.5f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Dragon Logo & Title (High Density)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HighDensityPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DragonVPN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HighDensityPrimaryContainer
                        ) {
                            Text(
                                text = "TCP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = HighDensityOnPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "DRAGON TCP CORE V1.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HighDensityTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                    )
                }
            }

            // Status Badge & Profile Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadge(vpnState = vpnState, pingMs = pingMs)

                IconButton(
                    onClick = onOpenProfiles,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .testTag("profiles_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = "Profiles",
                        tint = HighDensityTextSecondary
                    )
                }

                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .testTag("info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = HighDensityTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    vpnState: VpnState,
    pingMs: Long,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (vpnState) {
        VpnState.CONNECTED -> Triple(HighDensityEmerald.copy(alpha = 0.12f), HighDensityEmerald, HighDensityEmerald)
        VpnState.CONNECTING, VpnState.AUTHENTICATING -> Triple(HighDensityPrimaryContainer, HighDensityPrimary, HighDensityPrimary)
        VpnState.ERROR -> Triple(HighDensityCrimson.copy(alpha = 0.12f), HighDensityCrimson, HighDensityCrimson)
        VpnState.DISCONNECTING -> Triple(HighDensityAmber.copy(alpha = 0.12f), HighDensityAmber, HighDensityAmber)
        VpnState.DISCONNECTED, VpnState.RECONNECTING -> Triple(HighDensitySecondaryContainer, HighDensityTextSecondary, HighDensityTextMuted)
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseDot"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(if (vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Text(
                text = if (vpnState == VpnState.CONNECTED && pingMs >= 0) "${pingMs}ms" else vpnState.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 11.sp
                )
            )
        }
    }
}

