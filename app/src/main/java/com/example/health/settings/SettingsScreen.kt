package com.example.health.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.health.BuildConfig
import com.example.health.game.RunHistory
import com.example.health.health.RunReader
import com.example.health.net.ApiClient
import com.example.health.net.Session
import com.example.health.notify.Notifier
import com.example.health.sync.RunSyncer
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.Ink
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

/** 설정: 기록 동기화 · 알림 · 개인정보처리방침 · 계정(로그아웃·탈퇴) · (개발 빌드) 서버 주소 */
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
    var notifications by remember { mutableStateOf(session.notificationsEnabled) }
    var showPolicy by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    val actions = rememberActions()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifications = granted
        session.notificationsEnabled = granted
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("설정", onBack)
        ActionFeedback(actions)

        Panel(title = "🏃 운동 기록") {
            Text(
                "삼성헬스 등에 기록된 러닝·걸음 수를 Health Connect 로 읽어 자동으로 동기화합니다 " +
                    "(앱을 열 때, 그리고 꺼져 있을 때도 15분마다).",
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
            )
            GhostButton(
                "지금 동기화",
                onClick = {
                    actions.run {
                        val r = syncer.sync()
                        session.cachedHeroJson?.let { onHeroJson(JSONObject(it)) }
                        "러닝 ${r.readRuns}건 확인 · 새로 반영 ${r.reward.newRuns}건 · EXP +${r.reward.expGained}\n" +
                            "걸음 → 행동력 +${r.staminaGained}" +
                            r.reward.rejected.joinToString("") { "\n보상 제외: $it" }
                    }
                },
                enabled = !actions.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(
                "최근 러닝 기록 보기",
                onClick = { actions.run { history = RunHistory.listFrom(api.runHistory()); null } },
                enabled = !actions.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            history.forEach { r ->
                Text(
                    "${r.runDate} ${localTime(r.startTime)} · %.2fkm → EXP +${r.expAwarded}".format(r.distanceKm),
                    color = if (r.rejectedReason == null) Parchment else ParchmentDim,
                    style = MaterialTheme.typography.bodySmall,
                )
                r.rejectedReason?.let { Text("   ⚠ $it", color = Crimson, style = MaterialTheme.typography.bodySmall) }
            }
            Text(
                "러닝 1km = EXP 50 · 골드 20 (하루 10km 초과분은 절반) · 걸음 100보 = 행동력 1 (하루 최대 150)",
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Panel(title = "🔔 알림") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("원정 소식 알림", color = Parchment)
                    Text("레벨 업 · 주간 목표 달성 · 주말 저녁 남은 거리", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = notifications,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notifications = on
                            session.notificationsEnabled = on
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Gold),
                )
            }
        }

        Panel(title = "📜 개인정보처리방침") {
            GhostButton(if (showPolicy) "접기" else "보기", onClick = { showPolicy = !showPolicy }, modifier = Modifier.fillMaxWidth())
            if (showPolicy) PrivacyPolicyContent()
        }

        // 서버 주소는 개발 빌드에서만 바꿀 수 있음 (출시 빌드는 정해진 https 서버로 접속)
        if (BuildConfig.DEBUG) {
            Panel(title = "🛠 서버 (개발 빌드)") {
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
                            "✔ 연결됨 (${ms}ms) · 서버 ${health.optString("status")} / DB ${health.optString("database")}"
                        }
                    },
                    enabled = !actions.busy,
                )
            }
        }

        Panel(title = "👤 계정") {
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
            if (!deleting) {
                GhostButton("회원 탈퇴", onClick = { deleting = true }, enabled = !actions.busy, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "탈퇴하면 영웅, 장비, 러닝 기록, 친구가 모두 즉시 삭제되며 되돌릴 수 없습니다. 계속하려면 비밀번호를 입력하세요.",
                    color = Crimson,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = deletePassword,
                    onValueChange = { deletePassword = it },
                    label = { Text("비밀번호") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        "취소",
                        onClick = {
                            deleting = false
                            deletePassword = ""
                        },
                        modifier = Modifier.weight(1f),
                    )
                    GoldButton(
                        "탈퇴하기",
                        onClick = {
                            actions.run {
                                api.deleteAccount(deletePassword)
                                onLogout()
                                null
                            }
                        },
                        enabled = !actions.busy && deletePassword.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            "러너스 크로니클 v${BuildConfig.VERSION_NAME}",
            color = ParchmentDim,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

private val hhmm = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

private fun localTime(isoInstant: String): String =
    runCatching { hhmm.format(java.time.Instant.parse(isoInstant).atZone(java.time.ZoneId.systemDefault())) }.getOrDefault("")
