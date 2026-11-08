package com.example.health.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.example.health.game.RunHistory
import com.example.health.health.RunReader
import com.example.health.net.ApiClient
import com.example.health.net.Session
import com.example.health.sync.RunSyncer
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

/** 설정: 서버 주소, 연결 확인, 테스트 러닝, 로그아웃 */
@Composable
fun SettingsScreen(
    session: Session,
    api: ApiClient,
    onHeroJson: (JSONObject) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val reader = remember { RunReader(context.applicationContext) }
    val syncer = remember { RunSyncer(reader, session, api) }
    var serverUrl by remember { mutableStateOf(session.serverUrl) }
    var history by remember { mutableStateOf<List<RunHistory>>(emptyList()) }
    val actions = rememberActions()
    val writePermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("설정", onBack)
        ActionFeedback(actions)

        Panel(title = "서버") {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("서버 주소") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            ServerAddressHelp()
            GhostButton(
                "저장 후 연결 확인",
                onClick = {
                    actions.run {
                        session.serverUrl = serverUrl
                        serverUrl = session.serverUrl // 정리된 주소(http:// 추가 등)를 입력란에 반영
                        val started = System.currentTimeMillis()
                        val health = api.health()
                        val ms = System.currentTimeMillis() - started
                        "✔ 연결됨 (${ms}ms) · 서버 ${health.optString("status")} / DB ${health.optString("database")}\n" +
                            "이제 USB 없이 이 주소로 자동 동기화됩니다"
                    }
                },
                enabled = !actions.busy,
            )
        }

        Panel(title = "러닝 기록") {
            GhostButton(
                "지금 러닝·걸음 동기화",
                onClick = {
                    actions.run {
                        val r = syncer.sync()
                        session.cachedHeroJson?.let { onHeroJson(JSONObject(it)) }
                        "러닝 ${r.readRuns}건 읽음 · 새로 반영 ${r.reward.newRuns}건 · EXP +${r.reward.expGained}\n" +
                            "걸음 → 행동력 +${r.staminaGained}" +
                            r.reward.rejected.joinToString("") { "\n보상 제외: $it" }
                    }
                },
                enabled = !actions.busy,
            )
            Text("🛠 개발용: 삼성헬스 없이 보상 흐름 확인", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            GhostButton(
                "[테스트] 러닝 3km 기록",
                onClick = {
                    actions.run {
                        if (!reader.hasWritePermissions()) {
                            writePermissionLauncher.launch(reader.writePermissions)
                            "운동·거리 쓰기 권한을 허용한 뒤 다시 눌러 주세요"
                        } else {
                            reader.insertTestRun(3.0)
                            "테스트 러닝 3km 를 기록했습니다. '지금 러닝 동기화'를 누르세요"
                        }
                    }
                },
                enabled = !actions.busy,
            )
        }

        Panel(title = "🛠 테스트 걸음수") {
            Text("행동력 확인용: 방금 3,000보 걸은 것으로 기록 (행동력 +30)", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            GhostButton(
                "[테스트] 걸음 3,000보 기록",
                onClick = {
                    actions.run {
                        if (!reader.hasWritePermissions()) {
                            writePermissionLauncher.launch(reader.writePermissions)
                            "걸음수 쓰기 권한을 허용한 뒤 다시 눌러 주세요"
                        } else {
                            reader.insertTestSteps(3_000)
                            "3,000보를 기록했습니다. '지금 러닝 동기화'를 누르세요"
                        }
                    }
                },
                enabled = !actions.busy,
            )
        }

        Panel(title = "최근 러닝 기록과 보상") {
            GhostButton("불러오기", onClick = { actions.run { history = RunHistory.listFrom(api.runHistory()); null } }, enabled = !actions.busy)
            history.forEach { r ->
                Text(
                    "${r.runDate} ${localTime(r.startTime)} · %.2fkm → 보상 %.2fkm · EXP +${r.expAwarded}".format(r.distanceKm, r.countedKm),
                    color = if (r.rejectedReason == null) Parchment else ParchmentDim,
                    style = MaterialTheme.typography.bodySmall,
                )
                r.rejectedReason?.let { Text("   ⚠ $it", color = Crimson, style = MaterialTheme.typography.bodySmall) }
            }
            Text(
                "보상 규칙: 러닝 1km = EXP 50 · 골드 20 (상한 없음, 평균 시속 25km 초과 기록만 제외) · 걸음 100보 = 행동력 1 (하루 150)",
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Panel(title = "계정") {
            Text("로그인: ${session.loginId ?: "-"}", color = ParchmentDim)
            GoldButton(
                "로그아웃",
                onClick = {
                    actions.run {
                        runCatching { api.logout() }
                        onLogout()
                        null
                    }
                },
                enabled = !actions.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val hhmm = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

private fun localTime(isoInstant: String): String =
    runCatching { hhmm.format(java.time.Instant.parse(isoInstant).atZone(java.time.ZoneId.systemDefault())) }.getOrDefault("")
