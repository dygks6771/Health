package com.example.health.lobby

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.sprites.HeroAvatar
import com.example.health.ui.sprites.ItemIcon
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
    MenuItem("🐉", "레이드", "주 1회 · 마룡 토벌", Screen.RAID),
    MenuItem("🏪", "상점", "장비 뽑기", Screen.SHOP),
    MenuItem("⚖️", "경매장", "유저 간 거래", Screen.AUCTION),
    MenuItem("⚒️", "대장간", "장비 강화", Screen.SMITH),
    MenuItem("🎒", "영웅 · 가방", "능력치 · 장비", Screen.BAG),
    MenuItem("🏆", "랭킹", "전체 모험가 순위", Screen.RANKING),
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

    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
            EquipmentPanel(hero)

            if (needPermission) {
                Panel(title = "운동 기록 권한 필요", highlight = true) {
                    Text("러닝 거리를 경험치로 바꾸려면 Health Connect 의 운동·거리 읽기를 허용해 주세요.", color = Parchment)
                    GoldButton("권한 허용하기", onClick = { permissionLauncher.launch(reader.requestPermissions) })
                }
            }
            reward?.let { RewardPanel(it) }

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
            // 아래 바로가기 바에 가리지 않도록 여백
            Text("", modifier = Modifier.padding(bottom = 84.dp))
        }

        QuickBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onNavigate = onNavigate,
            onMenu = { menuOpen = true },
        )

        if (menuOpen) {
            MenuOverlay(
                onSelect = {
                    menuOpen = false
                    onNavigate(it)
                },
                onClose = { menuOpen = false },
            )
        }
    }
}

/** 하단 바로가기: 모험 · 모험자 길드 · 레이드 + 전체 메뉴 */
@Composable
private fun QuickBar(modifier: Modifier, onNavigate: (Screen) -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            Triple("🗺️", "모험", Screen.WORLD),
            Triple("📜", "길드", Screen.GUILD),
            Triple("🐉", "레이드", Screen.RAID),
        ).forEach { (icon, label, screen) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Gold, RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .clickable { onNavigate(screen) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(icon, fontSize = 22.sp)
                Text(label, color = GoldBright, style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, GoldBright, RoundedCornerShape(10.dp))
                .background(Gold, RoundedCornerShape(10.dp))
                .clickable(onClick = onMenu)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("☰", fontSize = 22.sp, color = Color.Black)
            Text("메뉴", color = Color.Black, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** 메뉴 창: 화면을 어둡게 덮고 컨텐츠 목록 표시 */
@Composable
private fun MenuOverlay(onSelect: (Screen) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Panel(
            title = "모험가의 수첩",
            highlight = true,
            // 패널 안을 눌러도 메뉴가 닫히지 않도록 클릭을 여기서 소비
            modifier = Modifier.padding(20.dp).clickable(onClick = {}),
        ) {
            menu.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { item -> MenuTile(item, Modifier.weight(1f)) { onSelect(item.screen) } }
                    if (row.size == 1) Column(modifier = Modifier.weight(1f)) {}
                }
            }
            GhostButton("닫기", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 장착 장비 4칸 */
@Composable
private fun EquipmentPanel(hero: HeroInfo) {
    Panel(title = "장착 장비") {
        listOf("WEAPON" to "무기", "HELMET" to "투구", "ARMOR" to "갑옷", "BOOTS" to "장화", "ACCESSORY" to "목걸이").forEach { (slot, label) ->
            val item = hero.equipment.firstOrNull { it.slot == slot }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ItemIcon(item, 40.dp, emptySlot = slot)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item?.let { "[${it.gradeName}] ${it.displayName}" } ?: "$label — 비어 있음",
                        color = item?.let { gradeColor(it.gradeName) } ?: ParchmentDim,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (item != null) Text(item.statText, color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            hero.stats.forEach { s ->
                Text("${s.name} ${s.total}", color = Parchment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HeroPanel(hero: HeroInfo) {
    Panel(highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HeroAvatar(hero.job, hero.appearance, 110.dp)
                Text("${Portraits.job(hero.job)} ${hero.jobName}", color = Gold, style = MaterialTheme.typography.labelLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(hero.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                Text("Lv.${hero.level} · ${hero.jobRole}", color = Parchment, fontWeight = FontWeight.SemiBold)
                Text("⚔ 전투력 %,d".format(hero.power), color = Gold, style = MaterialTheme.typography.titleMedium)
                Text("🏃 오늘 보상 %.2fkm · 이번 주 %.1fkm".format(hero.todayRewardKm, hero.weekKm), color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                if (hero.todayKm - hero.todayRewardKm > 0.01) {
                    Text(
                        "⚠ 오늘 %.2fkm 중 %.2fkm 는 보상 제외 (설정 → 최근 러닝 기록)".format(hero.todayKm, hero.todayKm - hero.todayRewardKm),
                        color = Crimson,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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
