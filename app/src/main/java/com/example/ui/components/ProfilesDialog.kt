package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DragonTunnelConfig
import com.example.model.ServerProfile
import com.example.ui.theme.*

@Composable
fun ProfilesDialog(
    profiles: List<ServerProfile>,
    currentConfig: DragonTunnelConfig,
    onSelectProfile: (ServerProfile) -> Unit,
    onSaveCurrent: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isSavingMode by remember { mutableStateOf(false) }
    var profileNameInput by remember {
        mutableStateOf(
            if (currentConfig.serverHost.isNotBlank()) "VPS ${currentConfig.serverHost}:${currentConfig.serverPort}" else "Novo Servidor Dragon"
        )
    }

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
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Profiles",
                            tint = HighDensityPrimary
                        )
                        Text(
                            text = "Perfis de Servidores",
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

                // Save Current Profile Box
                if (!isSavingMode) {
                    Button(
                        onClick = { isSavingMode = true },
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighDensityPrimary,
                            contentColor = HighDensityOnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Configuração Atual", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = profileNameInput,
                            onValueChange = { profileNameInput = it },
                            label = { Text("Nome do Perfil", color = HighDensityPrimary, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityCardBorder,
                                focusedContainerColor = HighDensityBg,
                                unfocusedContainerColor = HighDensityBg,
                                focusedTextColor = HighDensityTextPrimary,
                                unfocusedTextColor = HighDensityTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isSavingMode = false }) {
                                Text("Cancelar", color = HighDensityTextSecondary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    onSaveCurrent(profileNameInput)
                                    isSavingMode = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HighDensityEmerald,
                                    contentColor = HighDensityOnPrimary
                                )
                            ) {
                                Text("Salvar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = HighDensityCardBorder.copy(alpha = 0.6f))

                // Profile List
                Text(
                    text = "Perfis Salvos (${profiles.size}):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = HighDensityTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (profiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum perfil salvo ainda.\nSalve sua configuração para alternar rapidamente.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = HighDensityTextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profiles, key = { it.id }) { item ->
                            ProfileItemRow(
                                profile = item,
                                onSelect = {
                                    onSelectProfile(item)
                                    onDismiss()
                                },
                                onDelete = { onDeleteProfile(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileItemRow(
    profile: ServerProfile,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = HighDensityBg,
        border = BorderStroke(1.dp, HighDensityCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                )
                Text(
                    text = "${profile.config.serverHost}:${profile.config.serverPort} • ${profile.config.tunnelMode.displayName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HighDensityPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (profile.config.username.isNotBlank()) {
                    Text(
                        text = "Usuário: ${profile.config.username}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HighDensityTextMuted,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSelect) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = "Select",
                        tint = HighDensityEmerald
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = HighDensityCrimson
                    )
                }
            }
        }
    }
}

