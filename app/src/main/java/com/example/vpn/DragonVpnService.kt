package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.VpnRepository
import com.example.model.LogLevel
import com.example.model.VpnState
import com.example.vpn.dragon.DragonTcpTunnelEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class DragonVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tunInterface: ParcelFileDescriptor? = null
    private var tunnelEngine: DragonTcpTunnelEngine? = null

    companion object {
        const val ACTION_START_VPN = "com.example.vpn.START"
        const val ACTION_STOP_VPN = "com.example.vpn.STOP"
        const val CHANNEL_ID = "dragon_vpn_service_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, DragonVpnService::class.java).apply {
                action = ACTION_START_VPN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DragonVpnService::class.java).apply {
                action = ACTION_STOP_VPN
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        VpnRepository.init(applicationContext)

        tunnelEngine = DragonTcpTunnelEngine { socket ->
            protect(socket)
        }

        // Collect engine state
        serviceScope.launch {
            tunnelEngine?.vpnState?.collectLatest { state ->
                VpnRepository.setVpnState(state)
                updateNotification(state)
                if (state == VpnState.DISCONNECTED || state == VpnState.ERROR) {
                    if (state == VpnState.DISCONNECTED) {
                        stopSelf()
                    }
                }
            }
        }

        // Collect engine logs
        serviceScope.launch {
            tunnelEngine?.logEvents?.collectLatest { entry ->
                VpnRepository.addLog(entry.message, entry.level, entry.tag)
            }
        }

        // Collect engine stats
        serviceScope.launch {
            tunnelEngine?.vpnStats?.collectLatest { stats ->
                VpnRepository.setVpnStats(stats)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_VPN) {
            disconnectVpn()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_VPN) {
            connectVpn()
        }

        return START_STICKY
    }

    private fun connectVpn() {
        val config = VpnRepository.currentConfig.value

        if (config.serverHost.isBlank()) {
            VpnRepository.addLog("Error: Server IP / Host is missing!", LogLevel.ERROR)
            VpnRepository.setVpnState(VpnState.ERROR)
            stopSelf()
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification(VpnState.CONNECTING))
        VpnRepository.addLog("Configuring Android VPN TUN Network Interface...", LogLevel.INFO)

        try {
            val builder = Builder()
                .setSession("DragonTCP VPN")
                .setMtu(1500)
                .addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0)

            // DNS
            try {
                if (config.dnsPrimary.isNotBlank()) {
                    builder.addDnsServer(config.dnsPrimary.trim())
                } else {
                    builder.addDnsServer("8.8.8.8")
                }
                if (config.dnsSecondary.isNotBlank()) {
                    builder.addDnsServer(config.dnsSecondary.trim())
                }
            } catch (e: Exception) {
                builder.addDnsServer("8.8.8.8")
            }

            tunInterface = builder.establish()

            val tunFd = tunInterface
            if (tunFd == null) {
                VpnRepository.addLog("Failed to establish TUN interface (Permission revoked?)", LogLevel.ERROR)
                VpnRepository.setVpnState(VpnState.ERROR)
                stopSelf()
                return
            }

            VpnRepository.addLog("TUN interface created (10.8.0.2/32, MTU 1500).", LogLevel.SUCCESS)
            tunnelEngine?.startTunnel(config, tunFd)

        } catch (e: Exception) {
            VpnRepository.addLog("VPN Init Exception: ${e.message}", LogLevel.ERROR)
            VpnRepository.setVpnState(VpnState.ERROR)
            stopSelf()
        }
    }

    private fun disconnectVpn() {
        VpnRepository.setVpnState(VpnState.DISCONNECTING)
        tunnelEngine?.stopTunnel()
        try {
            tunInterface?.close()
        } catch (e: Exception) {}
        tunInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        VpnRepository.addLog("VPN permission revoked by system.", LogLevel.WARN)
        disconnectVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        tunnelEngine?.stopTunnel()
        try {
            tunInterface?.close()
        } catch (e: Exception) {}
        tunInterface = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DragonTCP VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active DragonTCP VPN tunnel connection status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: VpnState): Notification {
        val config = VpnRepository.currentConfig.value
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DragonVpnService::class.java).apply { action = ACTION_STOP_VPN },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "DragonTCP VPN • ${state.label}"
        val content = if (config.serverHost.isNotBlank()) {
            "Server: ${config.serverHost}:${config.serverPort} (${config.tunnelMode.displayName})"
        } else {
            "DragonTCP Tunnel"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .setOngoing(state == VpnState.CONNECTED || state == VpnState.CONNECTING || state == VpnState.AUTHENTICATING)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (state == VpnState.CONNECTED || state == VpnState.CONNECTING) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stopIntent)
        }

        return builder.build()
    }

    private fun updateNotification(state: VpnState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(state))
    }
}
