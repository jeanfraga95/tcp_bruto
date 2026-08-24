package com.example.vpn.dragon

import android.util.Base64
import com.example.model.DragonTunnelConfig
import com.example.model.TunnelMode
import java.nio.charset.StandardCharsets

object DragonProtocolHandler {

    /**
     * Builds the TCP handshake payload according to configured tunnel mode and credentials.
     */
    fun buildHandshake(config: DragonTunnelConfig): ByteArray {
        val host = config.serverHost.trim()
        val port = config.serverPort
        val hostPort = "$host:$port"
        val user = config.username.trim()
        val pass = config.password.trim()
        val basicAuth = if (user.isNotEmpty() || pass.isNotEmpty()) {
            val raw = "$user:$pass"
            Base64.encodeToString(raw.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        } else {
            ""
        }

        return when (config.tunnelMode) {
            TunnelMode.DRAGON_TCP_DIRECT -> {
                val sb = StringBuilder()
                sb.append("DRAGON-TCP/1.0\r\n")
                sb.append("Host: ").append(hostPort).append("\r\n")
                if (user.isNotEmpty()) sb.append("X-Dragon-User: ").append(user).append("\r\n")
                if (pass.isNotEmpty()) sb.append("X-Dragon-Pass: ").append(pass).append("\r\n")
                if (config.sni.isNotEmpty()) sb.append("X-Dragon-SNI: ").append(config.sni.trim()).append("\r\n")
                sb.append("X-Dragon-Client: Android-DragonTCP/1.0\r\n")
                sb.append("Connection: Upgrade\r\n")
                sb.append("Upgrade: DragonTCP\r\n\r\n")
                sb.toString().toByteArray(StandardCharsets.UTF_8)
            }

            TunnelMode.DRAGON_TCP_PAYLOAD -> {
                var payload = config.customPayload
                if (payload.isBlank()) {
                    payload = DragonTunnelConfig.DEFAULT_PAYLOAD
                }

                val replaced = payload
                    .replace("[host_port]", hostPort)
                    .replace("[host]", host)
                    .replace("[port]", port.toString())
                    .replace("[user]", user)
                    .replace("[pass]", pass)
                    .replace("[auth_basic]", basicAuth)
                    .replace("[crlf]", "\r\n")
                    .replace("[lf]", "\n")
                    .replace("[cr]", "\r")
                    .replace("[protocol]", "HTTP/1.1")
                    .replace("[raw]", "")
                    .replace("[sni]", if (config.sni.isNotEmpty()) config.sni.trim() else host)

                replaced.toByteArray(StandardCharsets.UTF_8)
            }

            TunnelMode.DRAGON_TCP_SSL -> {
                // SSL tunnel handshake encapsulation
                val sb = StringBuilder()
                sb.append("CONNECT ").append(hostPort).append(" HTTP/1.1\r\n")
                sb.append("Host: ").append(if (config.sni.isNotBlank()) config.sni.trim() else host).append("\r\n")
                if (basicAuth.isNotEmpty()) {
                    sb.append("Proxy-Authorization: Basic ").append(basicAuth).append("\r\n")
                }
                sb.append("User-Agent: DragonTCP/1.0\r\n")
                sb.append("Proxy-Connection: Keep-Alive\r\n\r\n")
                sb.toString().toByteArray(StandardCharsets.UTF_8)
            }

            TunnelMode.DRAGON_TCP_SSH -> {
                // SSH Header framing
                val sb = StringBuilder()
                sb.append("SSH-2.0-DragonTCP_1.0\r\n")
                sb.append("Auth: ").append(user).append(":").append(pass).append("\r\n\r\n")
                sb.toString().toByteArray(StandardCharsets.UTF_8)
            }
        }
    }

    /**
     * Inspects server response to verify handshake completion.
     */
    fun parseHandshakeResponse(response: String): HandshakeResult {
        val trimmed = response.trim()
        val firstLine = trimmed.lines().firstOrNull() ?: ""

        return when {
            firstLine.contains("200") || firstLine.contains("101") || firstLine.contains("DRAGON_OK") || firstLine.contains("OK") -> {
                HandshakeResult.Success("Handshake accepted ($firstLine)")
            }
            firstLine.contains("407") || firstLine.contains("Auth Required") -> {
                HandshakeResult.Error("Proxy Authentication Required (407) - Check username/password")
            }
            firstLine.contains("403") || firstLine.contains("Forbidden") -> {
                HandshakeResult.Error("Access Forbidden (403) - Account expired or unauthorized")
            }
            firstLine.contains("502") || firstLine.contains("503") || firstLine.contains("Bad Gateway") -> {
                HandshakeResult.Error("Server Gateway Error ($firstLine)")
            }
            trimmed.startsWith("SSH-2.0") -> {
                HandshakeResult.Success("SSH Dragon banner received ($firstLine)")
            }
            else -> {
                // Many TCP tunnel servers start piping raw traffic immediately or reply with raw bytes
                HandshakeResult.Success("Connected ($firstLine)")
            }
        }
    }
}

sealed class HandshakeResult {
    data class Success(val message: String) : HandshakeResult()
    data class Error(val message: String) : HandshakeResult()
}
