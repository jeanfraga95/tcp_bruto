package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DragonTunnelConfig
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.ServerProfile
import com.example.model.TunnelMode
import com.example.model.VpnState
import com.example.model.VpnStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object VpnRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var prefs: SharedPreferences? = null

    private const val PREFS_NAME = "dragon_tcp_vpn_prefs"
    private const val KEY_HOST = "key_host"
    private const val KEY_PORT = "key_port"
    private const val KEY_USER = "key_user"
    private const val KEY_PASS = "key_pass"
    private const val KEY_SNI = "key_sni"
    private const val KEY_PAYLOAD = "key_payload"
    private const val KEY_MODE = "key_mode"
    private const val KEY_DNS_PRI = "key_dns_pri"
    private const val KEY_DNS_SEC = "key_dns_sec"
    private const val KEY_PROFILES = "key_profiles"

    private val _currentConfig = MutableStateFlow(DragonTunnelConfig())
    val currentConfig = _currentConfig.asStateFlow()

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState = _vpnState.asStateFlow()

    private val _vpnStats = MutableStateFlow(VpnStats())
    val vpnStats = _vpnStats.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _profiles = MutableStateFlow<List<ServerProfile>>(emptyList())
    val profiles = _profiles.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedConfig()
        loadProfiles()
        addLog("DragonTCP Client initialised. Ready to connect.", LogLevel.INFO)
    }

    private fun loadSavedConfig() {
        val p = prefs ?: return
        val host = p.getString(KEY_HOST, "") ?: ""
        val port = p.getInt(KEY_PORT, 443)
        val user = p.getString(KEY_USER, "") ?: ""
        val pass = p.getString(KEY_PASS, "") ?: ""
        val sni = p.getString(KEY_SNI, "") ?: ""
        val payload = p.getString(KEY_PAYLOAD, DragonTunnelConfig.DEFAULT_PAYLOAD) ?: DragonTunnelConfig.DEFAULT_PAYLOAD
        val modeStr = p.getString(KEY_MODE, TunnelMode.DRAGON_TCP_DIRECT.name) ?: TunnelMode.DRAGON_TCP_DIRECT.name
        val mode = try { TunnelMode.valueOf(modeStr) } catch (e: Exception) { TunnelMode.DRAGON_TCP_DIRECT }
        val dnsPri = p.getString(KEY_DNS_PRI, "8.8.8.8") ?: "8.8.8.8"
        val dnsSec = p.getString(KEY_DNS_SEC, "1.1.1.1") ?: "1.1.1.1"

        _currentConfig.value = DragonTunnelConfig(
            serverHost = host,
            serverPort = port,
            username = user,
            password = pass,
            sni = sni,
            customPayload = payload,
            tunnelMode = mode,
            dnsPrimary = dnsPri,
            dnsSecondary = dnsSec
        )
    }

    fun updateConfig(config: DragonTunnelConfig) {
        _currentConfig.value = config
        prefs?.edit()?.apply {
            putString(KEY_HOST, config.serverHost)
            putInt(KEY_PORT, config.serverPort)
            putString(KEY_USER, config.username)
            putString(KEY_PASS, config.password)
            putString(KEY_SNI, config.sni)
            putString(KEY_PAYLOAD, config.customPayload)
            putString(KEY_MODE, config.tunnelMode.name)
            putString(KEY_DNS_PRI, config.dnsPrimary)
            putString(KEY_DNS_SEC, config.dnsSecondary)
            apply()
        }
    }

    fun setVpnState(state: VpnState) {
        _vpnState.value = state
    }

    fun setVpnStats(stats: VpnStats) {
        _vpnStats.value = stats
    }

    fun addLog(message: String, level: LogLevel = LogLevel.INFO, tag: String = "DragonTCP") {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            level = level
        )
        val current = _logs.value.toMutableList()
        if (current.size > 200) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addLog("Logs cleared.", LogLevel.INFO)
    }

    private fun loadProfiles() {
        val p = prefs ?: return
        val rawJson = p.getString(KEY_PROFILES, "[]") ?: "[]"
        try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<ServerProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cfgObj = obj.getJSONObject("config")
                val mode = try { TunnelMode.valueOf(cfgObj.getString("tunnelMode")) } catch (e: Exception) { TunnelMode.DRAGON_TCP_DIRECT }
                val config = DragonTunnelConfig(
                    serverHost = cfgObj.optString("serverHost", ""),
                    serverPort = cfgObj.optInt("serverPort", 443),
                    username = cfgObj.optString("username", ""),
                    password = cfgObj.optString("password", ""),
                    sni = cfgObj.optString("sni", ""),
                    customPayload = cfgObj.optString("customPayload", DragonTunnelConfig.DEFAULT_PAYLOAD),
                    dnsPrimary = cfgObj.optString("dnsPrimary", "8.8.8.8"),
                    dnsSecondary = cfgObj.optString("dnsSecondary", "1.1.1.1"),
                    tunnelMode = mode
                )
                list.add(
                    ServerProfile(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Server Profile"),
                        config = config,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            _profiles.value = list
        } catch (_: Exception) {
            _profiles.value = emptyList()
        }
    }

    fun saveProfile(profile: ServerProfile) {
        val list = _profiles.value.filterNot { it.id == profile.id }.toMutableList()
        list.add(0, profile)
        _profiles.value = list
        persistProfiles(list)
        addLog("Saved profile: ${profile.name}", LogLevel.SUCCESS)
    }

    fun deleteProfile(id: String) {
        val list = _profiles.value.filterNot { it.id == id }
        _profiles.value = list
        persistProfiles(list)
        addLog("Deleted server profile.", LogLevel.INFO)
    }

    private fun persistProfiles(list: List<ServerProfile>) {
        val p = prefs ?: return
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("createdAt", item.createdAt)
            val cfg = JSONObject()
            cfg.put("serverHost", item.config.serverHost)
            cfg.put("serverPort", item.config.serverPort)
            cfg.put("username", item.config.username)
            cfg.put("password", item.config.password)
            cfg.put("sni", item.config.sni)
            cfg.put("customPayload", item.config.customPayload)
            cfg.put("tunnelMode", item.config.tunnelMode.name)
            cfg.put("dnsPrimary", item.config.dnsPrimary)
            cfg.put("dnsSecondary", item.config.dnsSecondary)
            obj.put("config", cfg)
            arr.put(obj)
        }
        p.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }
}
