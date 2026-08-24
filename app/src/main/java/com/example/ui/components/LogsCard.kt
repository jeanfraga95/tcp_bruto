package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsCard(
    logs: List<LogEntry>,
    searchQuery: String,
    selectedLevel: LogLevel?,
    onSearchChange: (String) -> Unit,
    onLevelFilterChange: (LogLevel?) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var autoScroll by remember { mutableStateOf(true) }

    // Auto-scroll effect
    LaunchedEffect(logs.size, autoScroll) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row with Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Logs",
                        tint = HighDensityPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Logs do Sistema (${logs.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Auto-scroll toggle
                    IconButton(
                        onClick = { autoScroll = !autoScroll },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (autoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.Pause,
                            contentDescription = "Auto scroll",
                            tint = if (autoScroll) HighDensityPrimary else HighDensityTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Copy logs button
                    IconButton(
                        onClick = {
                            if (logs.isNotEmpty()) {
                                val text = logs.joinToString("\n") {
                                    "[${timeFormat.format(Date(it.timestamp))}] [${it.level}] ${it.message}"
                                }
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Logs copiados!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(30.dp).testTag("copy_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy logs",
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Clear logs button
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(30.dp).testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear logs",
                            tint = HighDensityCrimson,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filtrar logs...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = HighDensityTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = HighDensityTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("log_search_input")
            )

            // Level Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedLevel == null,
                    onClick = { onLevelFilterChange(null) },
                    label = { Text("Todos", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(6.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HighDensityPrimaryContainer,
                        selectedLabelColor = HighDensityOnPrimaryContainer,
                        containerColor = HighDensityBg,
                        labelColor = HighDensityTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedLevel == null,
                        borderColor = HighDensityCardBorder,
                        selectedBorderColor = HighDensityPrimary
                    ),
                    modifier = Modifier.height(26.dp)
                )

                listOf(
                    LogLevel.SUCCESS to "Sucesso",
                    LogLevel.INFO to "Info",
                    LogLevel.WARN to "Aviso",
                    LogLevel.ERROR to "Erro"
                ).forEach { (lvl, label) ->
                    val isSelected = selectedLevel == lvl
                    val chipColor = when (lvl) {
                        LogLevel.SUCCESS -> HighDensityEmerald
                        LogLevel.INFO -> HighDensityPrimary
                        LogLevel.WARN -> HighDensityAmber
                        LogLevel.ERROR -> HighDensityCrimson
                        LogLevel.DEBUG -> HighDensityTextMuted
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLevelFilterChange(if (isSelected) null else lvl) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(6.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.15f),
                            selectedLabelColor = chipColor,
                            containerColor = HighDensityBg,
                            labelColor = HighDensityTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = HighDensityCardBorder,
                            selectedBorderColor = chipColor
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            // High Density Terminal Console Box (#1B1B1F)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HighDensityTerminalBg)
                    .border(1.dp, HighDensityTerminalBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[INFO] DragonTCP pronto.\nInicie a conexão para ver o tráfego TCP em tempo real.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = HighDensityTerminalText.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs, key = { it.id }) { item ->
                            LogItemRow(entry = item, timeFormat = timeFormat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(
    entry: LogEntry,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val (levelColor, levelBadge) = when (entry.level) {
        LogLevel.SUCCESS -> Pair(HighDensityGreenBright, "OK")
        LogLevel.INFO -> Pair(HighDensityPrimaryContainer, "INF")
        LogLevel.WARN -> Pair(HighDensityAmber, "WRN")
        LogLevel.ERROR -> Pair(HighDensityErrorLight, "ERR")
        LogLevel.DEBUG -> Pair(HighDensityTerminalText, "DBG")
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = timeFormat.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.labelSmall.copy(
                color = HighDensityTerminalText.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        )

        // Level Badge
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = levelColor.copy(alpha = 0.18f),
            modifier = Modifier.padding(top = 1.dp)
        ) {
            Text(
                text = levelBadge,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = levelColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        // Message
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = when (entry.level) {
                    LogLevel.ERROR -> HighDensityErrorLight
                    LogLevel.SUCCESS -> HighDensityGreenBright
                    LogLevel.WARN -> HighDensityAmber
                    else -> HighDensityTerminalText
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

