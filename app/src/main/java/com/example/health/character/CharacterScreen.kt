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
import com.example.health.game.QuestStatus
import com.example.health.game.SyncReward
import com.example.health.health.StepsReader
import com.example.health.sync.StepsSyncer
import com.example.health.sync.SyncSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 메인 화면: 캐릭터, 레벨, 경험치, 골드, 퀘스트.
 * 화면이 열릴 때마다([resumeKey] 가 바뀔 때마다) 자동으로 서버와 동기화한다.
 */
@Composable
fun CharacterScreen(resumeKey: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reader = remember { StepsReader(context.applicationContext) }
    val settings = remember { SyncSettings(context.applicationContext) }
    val syncer = remember { StepsSyncer(reader, settings) }
    val scope = rememberCoroutineScope()

    var character by remember { mutableStateOf(syncer.cachedCharacter()) }
    var reward by remember { mutableStateOf<SyncReward?>(null) }
    var needPermission by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastSyncAt by remember { mutableLongStateOf(settings.lastSyncAt) }

    suspend fun syncNow() {
        if (syncing) return
        if (reader.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
            error = "이 기기에서는 Health Connect 를 사용할 수 없습니다."
            return
        }
        syncing = true
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
            syncing = false
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
                    Text("걸음수 권한이 필요합니다", style = MaterialTheme.typography.titleMedium)
                    Text("걸음수를 경험치로 바꾸려면 Health Connect 걸음수 읽기를 허용해 주세요.")
                    Button(onClick = { permissionLauncher.launch(reader.requestPermissions) }) { Text("권한 허용하기") }
                }
            }
        }

        reward?.let { RewardBanner(it) }

        val c = character
        if (c != null) {
            CharacterCard(c)
            Text("퀘스트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            c.quests.forEach { QuestCard(it) }
        } else if (!needPermission && error == null) {
            Text("캐릭터를 불러오는 중...")
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (syncing) "동기화 중..." else "마지막 동기화: ${formatTime(lastSyncAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(enabled = !syncing, onClick = { scope.launch { syncNow() } }) { Text("지금 동기화") }
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
            LinearProgressIndicator(
                progress = { c.levelProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Text("EXP ${c.expInLevel} / ${c.expToNextLevel}", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("💰 %,d G".format(c.gold), fontWeight = FontWeight.SemiBold)
                Text("👣 오늘 %,d보".format(c.todaySteps), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RewardBanner(r: SyncReward) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (r.leveledUp) {
                Text(
                    "🎉 레벨 업! Lv.${r.levelBefore} → Lv.${r.levelAfter}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("EXP +${r.expGained}" + if (r.goldGained > 0) " · 골드 +${r.goldGained}" else "")
            r.completedQuests.forEach { Text("✅ 퀘스트 완료: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun QuestCard(q: QuestStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text((if (q.weekly) "[주간] " else "[일일] ") + q.title, fontWeight = FontWeight.SemiBold)
                Text(
                    if (q.completed) "완료 ✓" else "%,d / %,d".format(q.progress, q.target),
                    color = if (q.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            LinearProgressIndicator(
                progress = { if (q.completed) 1f else q.fraction },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("보상: EXP +${q.rewardExp} · ${q.rewardGold} G", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("M/d HH:mm")

private fun formatTime(epochMillis: Long): String =
    if (epochMillis <= 0) "없음"
    else timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
