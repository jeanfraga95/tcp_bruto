package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VpnRepository
import com.example.model.*
import com.example.vpn.DragonVpnService
import com.example.vpn.dragon.DragonTcpTunnelEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val currentConfig = VpnRepository.currentConfig
    val vpnState = VpnRepository.vpnState
    val vpnStats = VpnRepository.vpnStats
    val logs = VpnRepository.logs
    val profiles = VpnRepository.profiles

    private val _logFilter = MutableStateFlow("")
    val logFilter = _logFilter.asStateFlow()

    private val _logLevelFilter = MutableStateFlow<LogLevel?>(null)
    val logLevelFilter = _logLevelFilter.asStateFlow()

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing = _isTestingPing.asStateFlow()

    private val _pingResult = MutableStateFlow<Long?>(null)
    val pingResult = _pingResult.asStateFlow()

    val filteredLogs: StateFlow<List<LogEntry>> = combine(logs, logFilter, logLevelFilter) { allLogs, query, level ->
        allLogs.filter { entry ->
            val matchesQuery = if (query.isBlank()) true else {
                entry.message.contains(query, ignoreCase = true) || entry.tag.contains(query, ignoreCase = true)
            }
            val matchesLevel = if (level == null) true else entry.level == level
            matchesQuery && matchesLevel
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        VpnRepository.init(application.applicationContext)
    }

    fun updateServerHost(host: String) {
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(serverHost = host))
    }

    fun updateServerPort(portStr: String) {
        val port = portStr.toIntOrNull() ?: 443
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(serverPort = port))
    }

    fun updateUsername(user: String) {
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(username = user))
    }

    fun updatePassword(pass: String) {
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(password = pass))
    }

    fun updateTunnelMode(mode: TunnelMode) {
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(tunnelMode = mode))
        VpnRepository.addLog("Mode switched to: ${mode.displayName}", LogLevel.INFO)
    }

    fun updateSni(sni: String) {
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(sni = sni))
    }

    fun updatePayload(payload: String) {
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(customPayload = payload))
    }

    fun updateDns(primary: String, secondary: String) {
        val current = currentConfig.value
        VpnRepository.updateConfig(current.copy(dnsPrimary = primary, dnsSecondary = secondary))
    }

    fun setLogFilter(query: String) {
        _logFilter.value = query
    }

    fun setLogLevelFilter(level: LogLevel?) {
        _logLevelFilter.value = level
    }

    fun clearLogs() {
        VpnRepository.clearLogs()
    }

    fun testServerPing() {
        val config = currentConfig.value
        if (config.serverHost.isBlank()) {
            VpnRepository.addLog("Cannot test ping: Server IP / Host is blank.", LogLevel.WARN)
            return
        }

        viewModelScope.launch {
            _isTestingPing.value = true
            _pingResult.value = null
            VpnRepository.addLog("Probing TCP latency to ${config.serverHost}:${config.serverPort}...", LogLevel.INFO)

            val engine = DragonTcpTunnelEngine { true }
            val latency = engine.testPing(config.serverHost, config.serverPort, 4000)
            _isTestingPing.value = false
            _pingResult.value = latency

            if (latency >= 0) {
                VpnRepository.addLog("TCP Ping Response: ${latency}ms (Reachable)", LogLevel.SUCCESS)
            } else {
                VpnRepository.addLog("TCP Ping Failed / Timeout (Unreachable or port filtered)", LogLevel.ERROR)
            }
        }
    }

    fun checkVpnPermission(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun toggleVpnConnection(context: Context) {
        val state = vpnState.value
        if (state == VpnState.CONNECTED || state == VpnState.CONNECTING || state == VpnState.AUTHENTICATING) {
            VpnRepository.addLog("Disconnect button clicked.", LogLevel.INFO)
            DragonVpnService.stopService(context)
        } else {
            val config = currentConfig.value
            if (config.serverHost.isBlank()) {
                VpnRepository.addLog("Error: Please provide a Server IP / Host before connecting.", LogLevel.ERROR)
                return
            }
            VpnRepository.addLog("Starting DragonTCP connection sequence...", LogLevel.INFO)
            DragonVpnService.startService(context)
        }
    }

    fun saveCurrentAsProfile(profileName: String) {
        val name = profileName.ifBlank { "Server ${currentConfig.value.serverHost}:${currentConfig.value.serverPort}" }
        val profile = ServerProfile(
            name = name,
            config = currentConfig.value
        )
        VpnRepository.saveProfile(profile)
    }

    fun loadProfile(profile: ServerProfile) {
        VpnRepository.updateConfig(profile.config)
        VpnRepository.addLog("Loaded profile '${profile.name}'", LogLevel.SUCCESS)
    }

    fun deleteProfile(id: String) {
        VpnRepository.deleteProfile(id)
    }
}
