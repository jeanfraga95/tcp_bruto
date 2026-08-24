package com.example.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.VpnState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DragonVpnScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val vpnStats by viewModel.vpnStats.collectAsStateWithLifecycle()
    val currentConfig by viewModel.currentConfig.collectAsStateWithLifecycle()
    val filteredLogs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    val logLevelFilter by viewModel.logLevelFilter.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val isTestingPing by viewModel.isTestingPing.collectAsStateWithLifecycle()
    val pingResult by viewModel.pingResult.collectAsStateWithLifecycle()

    var showProfilesDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Launcher for VPN Permission intent
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleVpnConnection(context)
        } else {
            Toast.makeText(context, "Permissão de VPN não concedida.", Toast.LENGTH_SHORT).show()
        }
    }

    val onToggleVpn = {
        if (vpnState == VpnState.DISCONNECTED || vpnState == VpnState.ERROR) {
            val prepareIntent = viewModel.checkVpnPermission(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                viewModel.toggleVpnConnection(context)
            }
        } else {
            viewModel.toggleVpnConnection(context)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HighDensityBg,
        topBar = {
            DragonHeader(
                vpnState = vpnState,
                pingMs = vpnStats.pingMs,
                onOpenProfiles = { showProfilesDialog = true },
                onOpenInfo = { showInfoDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Big Connect Power Button
            ConnectButton(
                vpnState = vpnState,
                vpnStats = vpnStats,
                config = currentConfig,
                onToggle = onToggleVpn,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Real-Time Stats (Download, Upload, Ping)
            StatsCard(stats = vpnStats)

            // Server, Port, User, Pass Config Card
            ServerConfigCard(
                config = currentConfig,
                isTestingPing = isTestingPing,
                pingResult = pingResult,
                onHostChange = { viewModel.updateServerHost(it) },
                onPortChange = { viewModel.updateServerPort(it) },
                onUserChange = { viewModel.updateUsername(it) },
                onPassChange = { viewModel.updatePassword(it) },
                onModeChange = { viewModel.updateTunnelMode(it) },
                onTestPing = { viewModel.testServerPing() }
            )

            // Advanced Settings (SNI, Custom Payload, DNS)
            AdvancedConfigCard(
                config = currentConfig,
                onSniChange = { viewModel.updateSni(it) },
                onPayloadChange = { viewModel.updatePayload(it) },
                onDnsChange = { pri, sec -> viewModel.updateDns(pri, sec) }
            )

            // Real-time Logs Console Card
            LogsCard(
                logs = filteredLogs,
                searchQuery = logFilter,
                selectedLevel = logLevelFilter,
                onSearchChange = { viewModel.setLogFilter(it) },
                onLevelFilterChange = { viewModel.setLogLevelFilter(it) },
                onClearLogs = { viewModel.clearLogs() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dialogs
        if (showProfilesDialog) {
            ProfilesDialog(
                profiles = profiles,
                currentConfig = currentConfig,
                onSelectProfile = { viewModel.loadProfile(it) },
                onSaveCurrent = { viewModel.saveCurrentAsProfile(it) },
                onDeleteProfile = { viewModel.deleteProfile(it) },
                onDismiss = { showProfilesDialog = false }
            )
        }

        if (showInfoDialog) {
            InfoDialog(onDismiss = { showInfoDialog = false })
        }
    }
}
