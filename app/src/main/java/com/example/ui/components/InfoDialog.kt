package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = BorderStroke(1.dp, HighDensityCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About DragonTCP",
                            tint = HighDensityPrimary
                        )
                        Text(
                            text = "Sobre o DragonTCP VPN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HighDensityTextSecondary
                        )
                    }
                }

                Text(
                    text = "Cliente Android para conexões VPN TCP baseadas no script/repositório DragonTCP (penguinehis/DragonTCP).",
                    style = MaterialTheme.typography.bodySmall.copy(color = HighDensityTextSecondary)
                )

                // VPS Setup Guide Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HighDensityBg,
                    border = BorderStroke(1.dp, HighDensityCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = HighDensityEmerald, modifier = Modifier.size(16.dp))
                            Text("Instruções no Servidor VPS", fontWeight = FontWeight.Bold, color = HighDensityTextPrimary, fontSize = 12.sp)
                        }
                        Text(
                            text = "1. Execute o script do DragonTCP no seu VPS (ex: portas 443, 80, 8080).\n2. Insira o IP público da VPS no campo IP.\n3. Coloque a porta configurada e as credenciais de usuário/senha.\n4. Toque no botão Conectar e acompanhe a autenticação em tempo real no console de logs!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = HighDensityTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                // Protocol & Security Features
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HighDensityBg,
                    border = BorderStroke(1.dp, HighDensityCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(16.dp))
                            Text("Recursos Integrados", fontWeight = FontWeight.Bold, color = HighDensityTextPrimary, fontSize = 12.sp)
                        }
                        Text(
                            text = "• Handshake TCP customizado DragonTCP\n• Injeção de Payload HTTP/Proxy e SNI\n• Suporte a TLS / SSL SNI Tunneling\n• Testador de Ping e Latência integrado\n• Console de Logs ao vivo com busca e cópia",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = HighDensityTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary, contentColor = HighDensityOnPrimary)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

