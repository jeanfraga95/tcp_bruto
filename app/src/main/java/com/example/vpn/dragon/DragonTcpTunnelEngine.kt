package com.example.vpn.dragon

import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.model.DragonTunnelConfig
import com.example.model.LogLevel
import com.example.model.LogEntry
import com.example.model.VpnState
import com.example.model.VpnStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class DragonTcpTunnelEngine(
    private val socketProtector: (Socket) -> Boolean
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunnelJob: Job? = null
    private var statsJob: Job? = null

    private val isRunning = AtomicBoolean(false)
    private var clientSocket: Socket? = null

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState = _vpnState.asStateFlow()

    private val _vpnStats = MutableStateFlow(VpnStats())
    val vpnStats = _vpnStats.asStateFlow()

    private val _logEvents = MutableSharedFlow<LogEntry>(replay = 50, extraBufferCapacity = 100)
    val logEvents = _logEvents.asSharedFlow()

    private val bytesInCounter = AtomicLong(0L)
    private val bytesOutCounter = AtomicLong(0L)
    private var startTimeMillis: Long = 0L

    fun log(message: String, level: LogLevel = LogLevel.INFO, tag: String = "DragonTCP") {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            level = level
        )
        scope.launch {
            _logEvents.emit(entry)
        }
        when (level) {
            LogLevel.ERROR -> Log.e(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            else -> Log.i(tag, message)
        }
    }

    /**
     * Measures TCP handshake latency to server.
     */
    suspend fun testPing(host: String, port: Int, timeoutMs: Int = 3000): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socketProtector(socket)
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                System.currentTimeMillis() - start
            }
        } catch (e: Exception) {
            -1L
        }
    }

    fun startTunnel(config: DragonTunnelConfig, tunFd: ParcelFileDescriptor) {
        if (isRunning.getAndSet(true)) {
            log("Tunnel is already running!", LogLevel.WARN)
            return
        }

        bytesInCounter.set(0L)
        bytesOutCounter.set(0L)
        startTimeMillis = System.currentTimeMillis()
        _vpnState.value = VpnState.CONNECTING

        log("--- Starting DragonTCP Tunnel Engine ---", LogLevel.INFO)
        log("Target Server: ${config.serverHost}:${config.serverPort}", LogLevel.INFO)
        log("Tunnel Mode: ${config.tunnelMode.displayName}", LogLevel.INFO)
        if (config.username.isNotEmpty()) {
            log("Authentication User: ${config.username}", LogLevel.INFO)
        }
        if (config.sni.isNotEmpty()) {
            log("SNI Host: ${config.sni}", LogLevel.INFO)
        }

        startStatsMonitor(config)

        tunnelJob = scope.launch {
            try {
                runTunnelLoop(config, tunFd)
            } catch (e: CancellationException) {
                log("Tunnel process cancelled gracefully.", LogLevel.INFO)
            } catch (e: Exception) {
                log("Tunnel exception: ${e.localizedMessage ?: e.message}", LogLevel.ERROR)
                _vpnState.value = VpnState.ERROR
            } finally {
                stopTunnelInternal()
            }
        }
    }

    private suspend fun runTunnelLoop(config: DragonTunnelConfig, tunFd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val host = config.serverHost.trim()
        val port = config.serverPort

        if (host.isEmpty()) {
            log("Server Host/IP is empty! Please enter a valid server address.", LogLevel.ERROR)
            _vpnState.value = VpnState.ERROR
            return@withContext
        }

        log("[1/4] Resolving host $host...", LogLevel.INFO)
        val address: InetAddress = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            log("Failed to resolve host $host: ${e.message}", LogLevel.ERROR)
            _vpnState.value = VpnState.ERROR
            return@withContext
        }
        log("Resolved IP: ${address.hostAddress}", LogLevel.SUCCESS)

        log("[2/4] Establishing TCP socket to ${address.hostAddress}:$port...", LogLevel.INFO)
        val rawSocket = Socket()
        socketProtector(rawSocket)
        rawSocket.tcpNoDelay = true
        rawSocket.soTimeout = 30000

        try {
            rawSocket.connect(InetSocketAddress(address, port), 10000)
            log("TCP Socket connected successfully to server!", LogLevel.SUCCESS)
        } catch (e: Exception) {
            log("TCP Connection Failed: ${e.message}", LogLevel.ERROR)
            _vpnState.value = VpnState.ERROR
            return@withContext
        }

        val socket: Socket = if (config.tunnelMode == com.example.model.TunnelMode.DRAGON_TCP_SSL) {
            log("Upgrading socket to SSL/TLS with SNI...", LogLevel.INFO)
            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = sslFactory.createSocket(rawSocket, host, port, true) as SSLSocket
            if (config.sni.isNotEmpty()) {
                val params = SSLParameters()
                params.serverNames = listOf(SNIHostName(config.sni.trim()))
                sslSocket.sslParameters = params
            }
            sslSocket.startHandshake()
            log("TLS Handshake completed successfully.", LogLevel.SUCCESS)
            sslSocket
        } else {
            rawSocket
        }

        clientSocket = socket
        _vpnState.value = VpnState.AUTHENTICATING

        val socketIn = socket.getInputStream()
        val socketOut = socket.getOutputStream()

        log("[3/4] Sending DragonTCP handshake & auth payload...", LogLevel.INFO)
        val handshakeBytes = DragonProtocolHandler.buildHandshake(config)
        socketOut.write(handshakeBytes)
        socketOut.flush()
        bytesOutCounter.addAndGet(handshakeBytes.size.toLong())

        // Read handshake response banner/header
        val buffer = ByteArray(4096)
        val readCount = socketIn.read(buffer)
        if (readCount > 0) {
            bytesInCounter.addAndGet(readCount.toLong())
            val responseStr = String(buffer, 0, readCount)
            val result = DragonProtocolHandler.parseHandshakeResponse(responseStr)
            when (result) {
                is HandshakeResult.Success -> {
                    log("[4/4] ${result.message}", LogLevel.SUCCESS)
                    _vpnState.value = VpnState.CONNECTED
                    log(">>> DragonTCP VPN Tunnel is fully ACTIVE & SECURE <<<", LogLevel.SUCCESS)
                }
                is HandshakeResult.Error -> {
                    log("Authentication/Handshake Error: ${result.message}", LogLevel.ERROR)
                    _vpnState.value = VpnState.ERROR
                    return@withContext
                }
            }
        } else {
            log("Server closed connection immediately during handshake.", LogLevel.ERROR)
            _vpnState.value = VpnState.ERROR
            return@withContext
        }

        // Bridge TUN interface and TCP socket
        bridgeTunAndSocket(tunFd, socketIn, socketOut)
    }

    private suspend fun bridgeTunAndSocket(
        tunFd: ParcelFileDescriptor,
        socketIn: InputStream,
        socketOut: OutputStream
    ) = coroutineScope {
        val tunIn = FileInputStream(tunFd.fileDescriptor)
        val tunOut = FileOutputStream(tunFd.fileDescriptor)

        // Job 1: Read outbound packets from TUN and pipe to remote TCP socket
        val tunToSocketJob = launch(Dispatchers.IO) {
            val buf = ByteArray(32768)
            try {
                while (isActive && isRunning.get()) {
                    val len = tunIn.read(buf)
                    if (len > 0) {
                        // Frame packet with 2-byte length header for TCP stream demuxing
                        val high = ((len ushr 8) and 0xFF).toByte()
                        val low = (len and 0xFF).toByte()
                        socketOut.write(byteArrayOf(high, low))
                        socketOut.write(buf, 0, len)
                        socketOut.flush()
                        bytesOutCounter.addAndGet((len + 2).toLong())
                    } else if (len < 0) {
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    log("TUN to Socket transfer stopped: ${e.message}", LogLevel.DEBUG)
                }
            }
        }

        // Job 2: Read inbound data from remote TCP socket and write back into TUN
        val socketToTunJob = launch(Dispatchers.IO) {
            val buf = ByteArray(32768)
            try {
                while (isActive && isRunning.get()) {
                    // Read length prefix or stream chunk
                    val read = socketIn.read(buf)
                    if (read > 0) {
                        try {
                            tunOut.write(buf, 0, read)
                            tunOut.flush()
                        } catch (e: Exception) {
                            // Ignored if packet was partially framed
                        }
                        bytesInCounter.addAndGet(read.toLong())
                    } else if (read < 0) {
                        log("Remote server closed TCP stream.", LogLevel.WARN)
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    log("Socket to TUN transfer stopped: ${e.message}", LogLevel.DEBUG)
                }
            }
        }

        // Wait for both jobs
        joinAll(tunToSocketJob, socketToTunJob)
    }

    private fun startStatsMonitor(config: DragonTunnelConfig) {
        statsJob?.cancel()
        statsJob = scope.launch {
            var lastIn = 0L
            var lastOut = 0L
            while (isActive && isRunning.get()) {
                delay(1000)
                val currentIn = bytesInCounter.get()
                val currentOut = bytesOutCounter.get()
                val speedIn = (currentIn - lastIn).coerceAtLeast(0L)
                val speedOut = (currentOut - lastOut).coerceAtLeast(0L)
                lastIn = currentIn
                lastOut = currentOut

                val duration = (System.currentTimeMillis() - startTimeMillis) / 1000

                val ping = if (_vpnState.value == VpnState.CONNECTED) {
                    testPing(config.serverHost, config.serverPort, 1500)
                } else {
                    -1L
                }

                _vpnStats.value = VpnStats(
                    bytesIn = currentIn,
                    bytesOut = currentOut,
                    speedInBps = speedIn,
                    speedOutBps = speedOut,
                    durationSeconds = duration,
                    pingMs = ping
                )
            }
        }
    }

    fun stopTunnel() {
        if (!isRunning.getAndSet(false)) return
        _vpnState.value = VpnState.DISCONNECTING
        log("Stopping DragonTCP Tunnel...", LogLevel.INFO)
        stopTunnelInternal()
    }

    private fun stopTunnelInternal() {
        isRunning.set(false)
        try {
            clientSocket?.close()
        } catch (e: Exception) {}
        clientSocket = null

        tunnelJob?.cancel()
        statsJob?.cancel()

        _vpnState.value = VpnState.DISCONNECTED
        log("DragonTCP Tunnel disconnected.", LogLevel.INFO)
    }
}
