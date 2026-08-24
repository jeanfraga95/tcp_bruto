package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DragonTunnelConfig
import com.example.model.VpnState
import com.example.model.VpnStats
import com.example.ui.theme.*

@Composable
fun ConnectButton(
    vpnState: VpnState,
    vpnStats: VpnStats,
    config: DragonTunnelConfig,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = vpnState == VpnState.CONNECTED
    val isConnecting = vpnState == VpnState.CONNECTING || vpnState == VpnState.AUTHENTICATING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High Density Hero Status Container
        val heroBg = when (vpnState) {
            VpnState.CONNECTED -> Color(0xFFD8F3E5)
            VpnState.CONNECTING, VpnState.AUTHENTICATING -> HighDensityPrimaryContainer
            VpnState.ERROR -> Color(0xFFFFDAD6)
            VpnState.DISCONNECTING -> Color(0xFFFFECC4)
            VpnState.DISCONNECTED, VpnState.RECONNECTING -> HighDensityPrimaryContainer
        }

        val heroTextColor = when (vpnState) {
            VpnState.CONNECTED -> Color(0xFF00391F)
            VpnState.CONNECTING, VpnState.AUTHENTICATING -> HighDensityOnPrimaryContainer
            VpnState.ERROR -> Color(0xFF410002)
            VpnState.DISCONNECTING -> Color(0xFF332000)
            VpnState.DISCONNECTED, VpnState.RECONNECTING -> HighDensityOnPrimaryContainer
        }

        val iconBgColor = when (vpnState) {
            VpnState.CONNECTED -> HighDensityEmerald
            VpnState.CONNECTING, VpnState.AUTHENTICATING -> HighDensityPrimary
            VpnState.ERROR -> HighDensityCrimson
            VpnState.DISCONNECTING -> HighDensityAmber
            VpnState.DISCONNECTED, VpnState.RECONNECTING -> HighDensityPrimary
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onToggle() },
            color = heroBg,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Centered Badge / Pulse Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (isConnected) scalePulse else 1f)
                        .clip(CircleShape)
                        .background(iconBgColor)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(34.dp)
                                .rotate(rotation),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = when (vpnState) {
                                VpnState.CONNECTED -> Icons.Default.Shield
                                VpnState.ERROR -> Icons.Default.PowerSettingsNew
                                else -> Icons.Default.PowerSettingsNew
                            },
                            contentDescription = "VPN Status",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "Túnel Conectado"
                        VpnState.CONNECTING -> "Estabelecendo Conexão..."
                        VpnState.AUTHENTICATING -> "Autenticando Credenciais..."
                        VpnState.DISCONNECTING -> "Encerrando Conexão..."
                        VpnState.ERROR -> "Falha na Conexão"
                        VpnState.DISCONNECTED, VpnState.RECONNECTING -> "Pronto para Conectar"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = heroTextColor,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> {
                            val target = if (config.serverHost.isNotBlank()) "${config.serverHost}:${config.serverPort}" else "Host Ativo"
                            "Status: Seguro via DragonTCP ($target)"
                        }
                        VpnState.CONNECTING -> "Status: TCP Handshake em andamento"
                        VpnState.AUTHENTICATING -> "Status: Validando usuário & senha"
                        VpnState.DISCONNECTING -> "Status: Liberando interface TUN"
                        VpnState.ERROR -> "Status: Erro de socket / Autenticação"
                        VpnState.DISCONNECTED, VpnState.RECONNECTING -> "Status: TCP Handshake Idle"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = heroTextColor.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                if (isConnected) {
                    val hours = vpnStats.durationSeconds / 3600
                    val minutes = (vpnStats.durationSeconds % 3600) / 60
                    val seconds = vpnStats.durationSeconds % 60
                    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Tempo Ativo: $timeString",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = heroTextColor,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High Density Action Button
        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("connect_button"),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isConnected -> HighDensityCrimson
                    isConnecting -> HighDensityAmber
                    else -> HighDensityPrimary
                },
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "DESCONECTAR TÚNEL"
                        VpnState.CONNECTING, VpnState.AUTHENTICATING -> "CANCELAR CONEXÃO"
                        VpnState.DISCONNECTING -> "DESCONECTANDO..."
                        VpnState.ERROR -> "TENTAR NOVAMENTE"
                        VpnState.DISCONNECTED, VpnState.RECONNECTING -> "INICIAR CONEXÃO"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}


