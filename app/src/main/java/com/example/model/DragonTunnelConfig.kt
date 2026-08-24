package com.example.model

enum class TunnelMode(val displayName: String, val description: String) {
    DRAGON_TCP_DIRECT("DragonTCP Direct", "Direct TCP tunnel with Dragon handshake"),
    DRAGON_TCP_PAYLOAD("TCP Custom Payload", "HTTP / TCP Payload injection header"),
    DRAGON_TCP_SSL("Dragon SSL / TLS", "TLS SNI encrypted TCP tunnel"),
    DRAGON_TCP_SSH("Dragon SSH Proxy", "SSH over TCP encapsulation tunnel")
}

data class DragonTunnelConfig(
    val serverHost: String = "",
    val serverPort: Int = 443,
    val username: String = "",
    val password: String = "",
    val sni: String = "",
    val customPayload: String = DEFAULT_PAYLOAD,
    val dnsPrimary: String = "8.8.8.8",
    val dnsSecondary: String = "1.1.1.1",
    val tunnelMode: TunnelMode = TunnelMode.DRAGON_TCP_DIRECT,
    val enableBypassLan: Boolean = true,
    val udpForwarding: Boolean = true,
    val bufferSizeKb: Int = 32
) {
    companion object {
        const val DEFAULT_PAYLOAD = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf]Proxy-Authorization: Basic [auth_basic][crlf]Upgrade: DragonTCP[crlf]Connection: Upgrade[crlf][crlf]"
    }
}

enum class VpnState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    AUTHENTICATING("Authenticating..."),
    CONNECTED("Connected"),
    RECONNECTING("Reconnecting..."),
    DISCONNECTING("Disconnecting..."),
    ERROR("Connection Error")
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR,
    DEBUG
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String = "DragonTCP",
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

data class VpnStats(
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val speedInBps: Long = 0L,
    val speedOutBps: Long = 0L,
    val durationSeconds: Long = 0L,
    val pingMs: Long = -1L
)

data class ServerProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val config: DragonTunnelConfig,
    val createdAt: Long = System.currentTimeMillis()
)
