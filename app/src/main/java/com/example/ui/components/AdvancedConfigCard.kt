package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DragonTunnelConfig
import com.example.ui.theme.*

@Composable
fun AdvancedConfigCard(
    config: DragonTunnelConfig,
    onSniChange: (String) -> Unit,
    onPayloadChange: (String) -> Unit,
    onDnsChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
        border = BorderStroke(1.dp, HighDensityCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Clickable Title to Expand / Collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Advanced Settings",
                        tint = HighDensityPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Configurações Avançadas & Payload",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = HighDensityTextSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SNI / Bug Host Input
                    OutlinedTextField(
                        value = config.sni,
                        onValueChange = onSniChange,
                        label = {
                            Text(
                                "SNI / BUG HOST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                        },
                        placeholder = { Text("ex: cloudflare.com ou sni.operadora.com.br", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityCardBorder,
                            focusedLabelColor = HighDensityPrimary,
                            unfocusedLabelColor = HighDensitySecondary,
                            focusedContainerColor = HighDensitySurface,
                            unfocusedContainerColor = HighDensitySurface,
                            focusedTextColor = HighDensityTextPrimary,
                            unfocusedTextColor = HighDensityTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sni_input")
                    )

                    // Custom Payload Editor
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PAYLOAD CUSTOMIZADO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HighDensityPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            TextButton(
                                onClick = { onPayloadChange(DragonTunnelConfig.DEFAULT_PAYLOAD) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Restaurar Padrão", fontSize = 11.sp, color = HighDensityPrimary)
                            }
                        }

                        OutlinedTextField(
                            value = config.customPayload,
                            onValueChange = onPayloadChange,
                            placeholder = { Text("CONNECT [host_port] HTTP/1.1[crlf]...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityCardBorder,
                                focusedContainerColor = HighDensitySurface,
                                unfocusedContainerColor = HighDensitySurface,
                                focusedTextColor = HighDensityTextPrimary,
                                unfocusedTextColor = HighDensityTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("payload_input")
                        )

                        // Macro tags hints
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("[host_port]", "[host]", "[auth_basic]", "[crlf]").forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = HighDensityPrimaryContainer,
                                    modifier = Modifier.clickable {
                                        onPayloadChange(config.customPayload + tag)
                                    }
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = HighDensityOnPrimaryContainer,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // DNS Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = config.dnsPrimary,
                            onValueChange = { onDnsChange(it, config.dnsSecondary) },
                            label = {
                                Text(
                                    "DNS PRIMÁRIO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityPrimary
                                )
                            },
                            placeholder = { Text("8.8.8.8", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityCardBorder,
                                focusedLabelColor = HighDensityPrimary,
                                unfocusedLabelColor = HighDensitySecondary,
                                focusedContainerColor = HighDensitySurface,
                                unfocusedContainerColor = HighDensitySurface,
                                focusedTextColor = HighDensityTextPrimary,
                                unfocusedTextColor = HighDensityTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = config.dnsSecondary,
                            onValueChange = { onDnsChange(config.dnsPrimary, it) },
                            label = {
                                Text(
                                    "DNS SECUNDÁRIO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityPrimary
                                )
                            },
                            placeholder = { Text("1.1.1.1", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityCardBorder,
                                focusedLabelColor = HighDensityPrimary,
                                unfocusedLabelColor = HighDensitySecondary,
                                focusedContainerColor = HighDensitySurface,
                                unfocusedContainerColor = HighDensitySurface,
                                focusedTextColor = HighDensityTextPrimary,
                                unfocusedTextColor = HighDensityTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

