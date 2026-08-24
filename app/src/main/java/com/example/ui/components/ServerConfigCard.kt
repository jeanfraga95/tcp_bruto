package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DragonTunnelConfig
import com.example.model.TunnelMode
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigCard(
    config: DragonTunnelConfig,
    isTestingPing: Boolean,
    pingResult: Long?,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onModeChange: (TunnelMode) -> Unit,
    onTestPing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
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
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Server Config",
                        tint = HighDensityPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Configuração do Servidor",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                }

                // Ping Button
                OutlinedButton(
                    onClick = onTestPing,
                    enabled = !isTestingPing && config.serverHost.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HighDensityPrimary
                    ),
                    border = BorderStroke(1.dp, HighDensityPrimary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("test_ping_button")
                ) {
                    if (isTestingPing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = HighDensityPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ping...", fontSize = 11.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Ping",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (pingResult != null) {
                                if (pingResult >= 0) "${pingResult}ms" else "Falha"
                            } else "Testar Ping",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pingResult != null && pingResult < 0) HighDensityCrimson else HighDensityPrimary
                        )
                    }
                }
            }

            // Server Mode Chips
            Text(
                text = "MODO DE CONEXÃO",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HighDensityPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TunnelMode.values().forEach { mode ->
                    val isSelected = config.tunnelMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeChange(mode) },
                        label = {
                            Text(
                                mode.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HighDensityPrimaryContainer,
                            selectedLabelColor = HighDensityOnPrimaryContainer,
                            containerColor = HighDensityBg,
                            labelColor = HighDensityTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = HighDensityCardBorder,
                            selectedBorderColor = HighDensityPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // IP / Host & Port
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = config.serverHost,
                    onValueChange = onHostChange,
                    label = {
                        Text(
                            "SERVER IP / HOST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                    },
                    placeholder = { Text("ex: 185.220.101.5", fontSize = 13.sp) },
                    trailingIcon = {
                        if (config.serverHost.isNotEmpty()) {
                            IconButton(onClick = { onHostChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = HighDensityTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
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
                    modifier = Modifier
                        .weight(1.8f)
                        .testTag("server_ip_input")
                )

                OutlinedTextField(
                    value = if (config.serverPort == 0) "" else config.serverPort.toString(),
                    onValueChange = onPortChange,
                    label = {
                        Text(
                            "PORTA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                    },
                    placeholder = { Text("443", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    modifier = Modifier
                        .weight(1.0f)
                        .testTag("server_port_input")
                )
            }

            // Quick Ports Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Portas:",
                    style = MaterialTheme.typography.labelSmall.copy(color = HighDensityTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                listOf(443, 80, 8080, 22, 3128, 8000).forEach { p ->
                    val isCurrent = config.serverPort == p
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCurrent) HighDensityPrimaryContainer else HighDensityBg,
                        border = BorderStroke(1.dp, if (isCurrent) HighDensityPrimary else HighDensityCardBorder),
                        modifier = Modifier.clickable { onPortChange(p.toString()) }
                    ) {
                        Text(
                            text = p.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isCurrent) HighDensityOnPrimaryContainer else HighDensityTextSecondary,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Username and Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = config.username,
                    onValueChange = onUserChange,
                    label = {
                        Text(
                            "USERNAME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                    },
                    placeholder = { Text("dragon_user", fontSize = 13.sp) },
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag("username_input")
                )

                OutlinedTextField(
                    value = config.password,
                    onValueChange = onPassChange,
                    label = {
                        Text(
                            "PASSWORD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                    },
                    placeholder = { Text("••••••••", fontSize = 13.sp) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                tint = HighDensityTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag("password_input")
                )
            }
        }
    }
}

