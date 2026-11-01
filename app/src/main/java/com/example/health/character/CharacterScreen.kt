package com.example.health.character

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.health.game.CharacterLook
import com.example.health.game.GameCharacter
import com.example.health.game.RunReward
import com.example.health.health.RunReader
import com.example.health.sync.RunSyncer
import com.example.health.sync.ServerApi
import com.example.health.sync.SyncSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 메인 화면: 캐릭터, 레벨, 행동력, 스탯, 장비.
 * 화면이 열릴 때마다([resumeKey] 가 바뀔 때마다) 러닝 기록을 자동으로 서버와 동기화한다.
 */
@Composable
fun CharacterScreen(resumeKey: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reader = remember { RunReader(context.applicationContext) }
    val settings = remember { SyncSettings(context.applicationContext) }
    val syncer = remember { RunSyncer(reader, settings) }
    val scope = rememberCoroutineScope()

    var character by remember { mutableStateOf(syncer.cachedCharacter()) }
    var reward by remember { mutableStateOf<RunReward?>(null) }
    var needPermission by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastSyncAt by remember { mutableLongStateOf(settings.lastSyncAt) }

    suspend fun syncNow() {
        if (busy) return
        if (reader.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
            error = "이 기기에서는 Health Connect 를 사용할 수 없습니다."
            return
        }
        busy = true
        error = null
        try {
            if (!reader.hasAllPermissions()) {
                needPermission = true
                return
            }
            needPermission = false
            val result = syncer.sync()
            character = result.character
            reward = result.reward.takeUnless { it.isEmpty }
            lastSyncAt = settings.lastSyncAt
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "서버에 연결하지 못했습니다: ${e.message ?: e.javaClass.simpleName}"
        } finally {
            busy = false
        }
    }

    /** 스탯 찍기·강화처럼 캐릭터를 바꾸는 요청. 응답의 캐릭터로 화면 갱신 */
    fun runAction(block: suspend (ServerApi, Long) -> JSONObject) {
        val userId = settings.userId ?: return
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val json = block(ServerApi(settings.serverUrl), userId)
                val characterJson = json.optJSONObject("character") ?: json
                syncer.save(characterJson)
                character = GameCharacter.from(characterJson)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { scope.launch { syncNow() } }

    LaunchedEffect(resumeKey) { syncNow() }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (needPermission) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("운동 기록 권한이 필요합니다", style = MaterialTheme.typography.titleMedium)
                    Text("러닝 거리를 경험치로 바꾸려면 Health Connect 의 운동·거리 읽기를 허용해 주세요.")
                    Button(onClick = { permissionLauncher.launch(reader.requestPermissions) }) { Text("권한 허용하기") }
                }
            }
        }

        reward?.let { RewardBanner(it) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val c = character
        if (c != null) {
            CharacterCard(c)
            StatsCard(c, enabled = !busy) { stat -> runAction { api, id -> api.allocateStat(id, stat) } }
            EquipmentCard(c, enabled = !busy) { runAction { api, id -> api.enhanceWeapon(id) } }
        } else if (!needPermission && error == null) {
            Text("캐릭터를 불러오는 중...")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (busy) "처리 중..." else "마지막 동기화: ${formatTime(lastSyncAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(enabled = !busy, onClick = { scope.launch { syncNow() } }) { Text("지금 동기화") }
        }
    }
}

@Composable
private fun CharacterCard(c: GameCharacter) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(CharacterLook.avatar(c.level), fontSize = 56.sp)
                Column {
                    Text(
                        CharacterLook.title(c.level),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${c.nickname}  Lv.${c.level}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            LinearProgressIndicator(progress = { c.levelProgress }, modifier = Modifier.fillMaxWidth().height(10.dp))
            Text("EXP ${c.expInLevel} / ${c.expToNextLevel}", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { c.staminaProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text("⚡ 행동력 ${c.stamina} / ${c.maxStamina}  (러닝 1km = 10)", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("💰 %,d G".format(c.gold), fontWeight = FontWeight.SemiBold)
                Text("🏃 오늘 %.2fkm · 이번 주 %.1fkm".format(c.todayKm, c.weekKm), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatsCard(c: GameCharacter, enabled: Boolean, onAdd: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("능력치", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "남은 포인트 ${c.statPoints}",
                    color = if (c.statPoints > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            c.stats.forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.name, modifier = Modifier.weight(1f))
                    Text(
                        if (s.bonus > 0) "${s.total}  (${s.base} + 장비 ${s.bonus})" else "${s.total}",
                        modifier = Modifier.weight(2f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedButton(enabled = enabled && c.statPoints > 0, onClick = { onAdd(s.code) }) { Text("+1") }
                }
            }
            Text("레벨업마다 포인트 3 지급", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EquipmentCard(c: GameCharacter, enabled: Boolean, onEnhance: () -> Unit) {
    val w = c.weapon
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("장비", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("⚔️ 무기 +${w.enhance} [${w.gradeName}]  공격력 +${w.attackBonus}", fontWeight = FontWeight.SemiBold)
            Text("💎 강화석 ${c.enhanceStones}개 (던전 보상)", style = MaterialTheme.typography.bodySmall)
            if (w.nextCostStones != null && w.nextCostGold != null) {
                val affordable = c.enhanceStones >= w.nextCostStones && c.gold >= w.nextCostGold
                Button(enabled = enabled && affordable, onClick = onEnhance) {
                    Text("강화 +${w.enhance + 1}  (강화석 ${w.nextCostStones} · %,d G)".format(w.nextCostGold))
                }
            } else {
                Text("최대 강화 달성", color = MaterialTheme.colorScheme.primary)
            }

            HorizontalDivider()
            Text("장신구 (레이드 보상, 종류별 가장 좋은 것 자동 장착)", style = MaterialTheme.typography.bodySmall)
            if (c.accessories.isEmpty()) {
                Text("아직 없음", style = MaterialTheme.typography.bodySmall)
            }
            c.accessories.sortedByDescending { it.equipped }.forEach { a ->
                Text(
                    (if (a.equipped) "✅ " else "▫️ ") + "${a.gradeName} ${a.typeName}  ${a.statName} +${a.bonus}",
                    color = if (a.equipped) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun RewardBanner(r: RunReward) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (r.leveledUp) {
                Text(
                    "🎉 레벨 업! Lv.${r.levelBefore} → Lv.${r.levelAfter} (스탯 포인트 +${r.statPointsGained})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("🏃 새 러닝 ${r.newRuns}건 · %.2fkm 반영".format(r.countedKm))
            Text("EXP +${r.expGained} · 골드 +${r.goldGained} · 행동력 +${r.staminaGained}")
            r.rejected.forEach { Text("⚠️ 보상 제외: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("M/d HH:mm")

private fun formatTime(epochMillis: Long): String =
    if (epochMillis <= 0) "없음"
    else timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
