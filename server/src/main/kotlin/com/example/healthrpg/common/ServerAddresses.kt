package com.example.healthrpg.common

import org.slf4j.LoggerFactory
import org.springframework.boot.web.server.context.WebServerInitializedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.Inet4Address
import java.net.NetworkInterface

/** 폰에서 이 PC 서버에 접속할 수 있는 주소 1개 */
data class ServerAddress(val kind: Kind, val url: String, val interfaceName: String) {
    enum class Kind(val label: String) {
        TAILSCALE("밖에서 (Tailscale)"),
        LAN("같은 와이파이"),
        OTHER("기타"),
    }
}

object ServerAddresses {

    /** 가상 네트워크(WSL·Docker·VM) 는 폰에서 접속할 수 없으므로 제외 */
    private val VIRTUAL_NAMES = listOf("vethernet", "docker", "virtualbox", "vmware", "hyper-v", "wsl", "br-", "veth")

    fun classify(ip: String): ServerAddress.Kind {
        val parts = ip.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return ServerAddress.Kind.OTHER
        val (a, b) = parts
        return when {
            a == 100 && b in 64..127 -> ServerAddress.Kind.TAILSCALE // CGNAT 대역 = Tailscale 주소
            a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168) -> ServerAddress.Kind.LAN
            else -> ServerAddress.Kind.OTHER
        }
    }

    fun isVirtual(interfaceName: String): Boolean {
        val name = interfaceName.lowercase()
        return VIRTUAL_NAMES.any { it in name }
    }

    /** 이 PC 의 IPv4 주소들을 폰에서 쓸 서버 주소로 (Tailscale → 와이파이 → 기타 순) */
    fun find(port: Int): List<ServerAddress> =
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .filterNot { isVirtual(it.name) || isVirtual(it.displayName ?: "") }
            .flatMap { nic ->
                nic.inetAddresses.toList().filterIsInstance<Inet4Address>().map { address ->
                    val ip = address.hostAddress
                    ServerAddress(classify(ip), "http://$ip:$port", nic.displayName ?: nic.name)
                }
            }
            .sortedBy { it.kind.ordinal }
}

/** 서버가 켜지면 폰 앱 설정에 입력할 주소를 콘솔에 출력 */
@Component
class ServerAddressPrinter {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onStarted(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        val addresses = runCatching { ServerAddresses.find(port) }.getOrDefault(emptyList())
        val lines = buildList {
            add("")
            add("==================== 폰 앱 > 설정 > 서버 주소 ====================")
            add("  USB 연결 (adb reverse tcp:$port tcp:$port) : http://127.0.0.1:$port")
            addresses.forEach { add("  ${it.kind.label.padEnd(16)} : ${it.url}   [${it.interfaceName}]") }
            if (addresses.none { it.kind == ServerAddress.Kind.TAILSCALE }) {
                add("  밖에서 접속하려면 PC·폰에 Tailscale 을 설치해 같은 계정으로 로그인하세요 (server/README.md)")
            }
            add("===================================================================")
        }
        log.info(lines.joinToString("\n"))
    }
}
