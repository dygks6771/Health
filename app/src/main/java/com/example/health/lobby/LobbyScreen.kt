package com.example.health.lobby

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.health.app.Screen
import com.example.health.game.HeroInfo
import com.example.health.game.Portraits
import com.example.health.game.RunReward
import com.example.health.health.RunReader
import com.example.health.net.ApiException
import com.example.health.net.Session
import com.example.health.sync.RunSyncer
import com.example.health.ui.fantasy.FantasyBar
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ResourceChip
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Emerald
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.Mana
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class MenuItem(val icon: String, val title: String, val subtitle: String, val screen: Screen)

private val menu = listOf(
    MenuItem("🗺️", "모험", "10개 지역 · 던전", Screen.WORLD),
    MenuItem("📜", "모험자 길드", "오늘의 토벌 의뢰", Screen.GUILD),
    MenuItem("🐉", "레이드", "친구와 마룡 토벌", Screen.RAID),
    MenuItem("⚒️", "대장간", "장비 강화", Screen.SMITH),
    MenuItem("🎒", "영웅 · 가방", "능력치 · 장비", Screen.BAG),
    MenuItem("👥", "친구", "동료 모집", Screen.FRIENDS),
    MenuItem("⚙️", "설정", "서버 · 테스트 · 로그아웃", Screen.SETTINGS),
)

/** 로비: 영웅과 재화, 러닝 동기화, 메뉴. 화면에 돌아올 때마다 러닝 기록을 자동 동기화 */
@Composable
fun LobbyScreen(
    hero: HeroInfo,
    session: Session,
    resumeKey: Int,
    onHeroJson: (JSONObject) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val context = LocalContext.current
    val reader = remember { RunReader(context.applicationContext) }
    val syncer = remember { RunSyncer(reader, session) }
    val scope = rememberCoroutineScope()

    var reward by remember { mutableStateOf<RunReward?>(null) }
    var needPermission by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var lastSyncAt by remember { mutableLongStateOf(session.lastSyncAt) }

    suspend fun syncNow() {
        if (syncing) return
        if (reader.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
            syncError = "이 기기에서는 Health Connect 를 사용할 수 없습니다"
            return
        }
        syncing = true
        syncError = null
        try {
            if (!reader.hasAllPermissions()) {
                needPermission = true
                return
            }
            needPermission = false
            val result = syncer.sync()
            session.cachedHeroJson?.let { onHeroJson(JSONObject(it)) }
            reward = result.reward.takeIf { it.newRuns > 0 }
            lastSyncAt = session.lastSyncAt
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            syncError = e.message
        } catch (e: Exception) {
            syncError = "서버에 연결하지 못했습니다"
        } finally {
            syncing = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { scope.launch { syncNow() } }

    LaunchedEffect(resumeKey) { syncNow() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ResourceChip("💰", "%,d".format(hero.gold))
            ResourceChip("⚡", "${hero.stamina}/${hero.maxStamina}")
            ResourceChip("💎", "${hero.enhanceStones}")
        }

        HeroPanel(hero)

        if (needPermission) {
            Panel(title = "운동 기록 권한 필요", highlight = true) {
                Text("러닝 거리를 경험치로 바꾸려면 Health Connect 의 운동·거리 읽기를 허용해 주세요.", color = Parchment)
                GoldButton("권한 허용하기", onClick = { permissionLauncher.launch(reader.requestPermissions) })
            }
        }
        reward?.let { RewardPanel(it) }

        menu.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item -> MenuTile(item, Modifier.weight(1f)) { onNavigate(item.screen) } }
                if (row.size == 1) Column(modifier = Modifier.weight(1f)) {}
            }
        }

        syncError?.let { Text("⚠ $it", color = Crimson, style = MaterialTheme.typography.bodySmall) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (syncing) "러닝 기록 동기화 중..." else "마지막 동기화: ${formatTime(lastSyncAt)}",
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
            )
            GhostButton("동기화", onClick = { scope.launch { syncNow() } }, enabled = !syncing)
        }
    }
}

@Composable
private fun HeroPanel(hero: HeroInfo) {
    Panel(highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Portraits.appearance(hero.appearance), fontSize = 72.sp)
                Text("${Portraits.job(hero.job)} ${hero.jobName}", color = Gold, style = MaterialTheme.typography.labelLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(hero.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                Text("Lv.${hero.level} · ${hero.jobRole}", color = Parchment, fontWeight = FontWeight.SemiBold)
                Text("⚔ 전투력 %,d".format(hero.power), color = Gold, style = MaterialTheme.typography.titleMedium)
                Text("🏃 오늘 %.2fkm · 이번 주 %.1fkm".format(hero.todayKm, hero.weekKm), color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("EXP ${hero.expInLevel} / ${hero.expToNextLevel}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        FantasyBar(hero.expProgress, Mana)
        Text("행동력 ${hero.stamina} / ${hero.maxStamina}  (러닝 1km = 10)", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        FantasyBar(hero.staminaProgress, Emerald)
        if (hero.statPoints > 0) {
            Text("✦ 사용하지 않은 능력치 포인트 ${hero.statPoints} — 영웅 · 가방에서 찍으세요", color = GoldBright, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RewardPanel(r: RunReward) {
    Panel(title = "🏃 러닝 보상", highlight = true) {
        if (r.leveledUp) {
            Text("🎉 레벨 업! Lv.${r.levelBefore} → Lv.${r.levelAfter}  (포인트 +${r.statPointsGained})", color = GoldBright, fontWeight = FontWeight.Bold)
        }
        Text("새 러닝 ${r.newRuns}건 · %.2fkm 반영".format(r.countedKm), color = Parchment)
        Text("EXP +${r.expGained} · 골드 +${r.goldGained} · 행동력 +${r.staminaGained}", color = Gold)
        r.rejected.forEach { Text("⚠ 보상 제외: $it", color = Crimson, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun MenuTile(item: MenuItem, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .border(1.dp, GoldDim, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(item.icon, fontSize = 30.sp)
        Text(item.title, color = Gold, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(item.subtitle, color = ParchmentDim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("M/d HH:mm")

private fun formatTime(epochMillis: Long): String =
    if (epochMillis <= 0) "없음" else timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
