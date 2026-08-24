package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VpnRepository
import com.example.model.DragonTunnelConfig
import com.example.model.LogLevel
import com.example.model.TunnelMode
import com.example.model.VpnState
import com.example.vpn.dragon.DragonProtocolHandler
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    VpnRepository.init(context)
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("DragonTCP VPN", appName)
  }

  @Test
  fun `config default values check`() {
    val config = DragonTunnelConfig(
      serverHost = "192.168.1.100",
      serverPort = 8080,
      username = "test_user",
      password = "secret_password"
    )
    assertEquals("192.168.1.100", config.serverHost)
    assertEquals(8080, config.serverPort)
    assertEquals("test_user", config.username)
    assertEquals("secret_password", config.password)
    assertEquals(TunnelMode.DRAGON_TCP_DIRECT, config.tunnelMode)
  }

  @Test
  fun `payload formatter replaces tags correctly`() {
    val config = DragonTunnelConfig(
      serverHost = "vpn.dragon.site",
      serverPort = 443,
      username = "admin",
      password = "password123",
      customPayload = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf]Authorization: [auth_basic][crlf][crlf]"
    )
    val formatted = DragonProtocolHandler.buildPayload(config)
    assertTrue(formatted.contains("CONNECT vpn.dragon.site:443 HTTP/1.1\r\n"))
    assertTrue(formatted.contains("Host: vpn.dragon.site\r\n"))
    assertTrue(formatted.contains("Authorization: Basic "))
  }

  @Test
  fun `repository logs management`() {
    VpnRepository.clearLogs()
    VpnRepository.addLog("Test message 1", LogLevel.INFO)
    VpnRepository.addLog("Test message 2", LogLevel.SUCCESS)

    val currentLogs = VpnRepository.logs.value
    assertTrue(currentLogs.any { it.message.contains("Test message 1") })
    assertTrue(currentLogs.any { it.message.contains("Test message 2") })
  }
}

