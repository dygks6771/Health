package com.example.healthrpg

import com.example.healthrpg.common.ServerAddress.Kind
import com.example.healthrpg.common.ServerAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerAddressesTest {

    @Test
    fun `IP 대역으로 접속 방법을 구분한다`() {
        assertEquals(Kind.TAILSCALE, ServerAddresses.classify("100.101.5.7"))
        assertEquals(Kind.LAN, ServerAddresses.classify("192.168.0.10"))
        assertEquals(Kind.LAN, ServerAddresses.classify("10.0.0.3"))
        assertEquals(Kind.LAN, ServerAddresses.classify("172.20.1.1"))
        assertEquals(Kind.OTHER, ServerAddresses.classify("100.20.1.1"))
        assertEquals(Kind.OTHER, ServerAddresses.classify("8.8.8.8"))
    }

    @Test
    fun `WSL·Docker 가상 네트워크는 제외한다`() {
        assertTrue(ServerAddresses.isVirtual("vEthernet (WSL (Hyper-V firewall))"))
        assertTrue(ServerAddresses.isVirtual("docker0"))
        assertFalse(ServerAddresses.isVirtual("Wi-Fi"))
        assertFalse(ServerAddresses.isVirtual("Tailscale"))
    }

    @Test
    fun `이 PC 주소 목록을 만들 수 있다`() {
        ServerAddresses.find(8080).forEach { assertTrue(it.url.startsWith("http://") && it.url.endsWith(":8080")) }
    }
}
