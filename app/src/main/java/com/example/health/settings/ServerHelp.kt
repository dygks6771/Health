package com.example.health.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.health.ui.theme.ParchmentDim

/** 서버 주소 입력란 아래 안내: 연결 방법 3가지 */
@Composable
fun ServerAddressHelp() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(
            "PC 서버를 켜면 콘솔에 폰에서 쓸 주소가 표시됩니다.",
            "• USB: adb reverse tcp:8080 tcp:8080 후 http://127.0.0.1:8080",
            "• 같은 와이파이: http://<PC 와이파이 IP>:8080 (예: 192.168.0.10:8080)",
            "• 밖에서: PC·폰에 Tailscale 을 켜고 http://<PC Tailscale IP>:8080 (예: 100.64.1.2:8080)",
            "http:// 를 빼고 입력해도 됩니다.",
        ).forEach { Text(it, color = ParchmentDim, style = MaterialTheme.typography.bodySmall) }
    }
}
