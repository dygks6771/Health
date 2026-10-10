package com.example.health.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.health.health.StepsReader
import kotlinx.coroutines.launch

/** 서버 주소 입력 + 연결 확인 + 걸음수 동기화 */
@Composable
fun SyncSection(reader: StepsReader, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { SyncSettings(context.applicationContext) }
    val syncer = remember { StepsSyncer(reader, settings) }
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(settings.serverUrl) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun launchAction(block: suspend () -> String) {
        settings.serverUrl = serverUrl
        busy = true
        message = null
        scope.launch {
            try {
                message = block()
                isError = false
            } catch (e: Exception) {
                message = "실패: ${e.message ?: e.javaClass.simpleName}\n" +
                    "서버가 켜져 있는지, 주소가 맞는지 확인하세요."
                isError = true
            } finally {
                busy = false
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("서버 동기화", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("서버 주소") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "USB 연결: adb reverse tcp:8080 tcp:8080 실행 후 http://127.0.0.1:8080\n" +
                    "와이파이: http://<PC IP>:8080",
                style = MaterialTheme.typography.bodySmall,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        launchAction {
                            val health = ServerApi(serverUrl).health()
                            "서버 ${health.optString("status")} / DB ${health.optString("database")}"
                        }
                    },
                ) { Text("연결 확인") }

                Button(
                    enabled = !busy,
                    onClick = {
                        launchAction {
                            val r = syncer.sync()
                            val match = if (r.serverTotal == r.localTotal) "일치" else "불일치!"
                            "사용자 #${r.userId} · 최근 ${r.syncedDays}일 동기화 완료\n" +
                                "앱 %,d걸음 / 서버 저장 %,d걸음 ($match)".format(r.localTotal, r.serverTotal)
                        }
                    },
                ) { Text("서버로 동기화") }
            }

            if (busy) Text("처리 중...")
            message?.let {
                Text(
                    it,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
