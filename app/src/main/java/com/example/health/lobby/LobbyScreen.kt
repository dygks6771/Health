package com.example.health.lobby

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.health.app.Screen
import com.example.health.game.HeroInfo
import com.example.health.game.ItemInfo
import com.example.health.game.Portraits
import com.example.health.game.RunReward
import com.example.health.game.WeeklyInfo
import com.example.health.game.formatPace
import com.example.health.health.RunReader
import com.example.health.net.ApiClient
import com.example.health.net.ApiException
import com.example.health.net.ServerUnreachableException
import com.example.health.net.Session
import com.example.health.notify.Notifier
import com.example.health.sync.RunSyncer
import com.example.health.sync.SyncWorker
import com.example.health.ui.battle.ChestInfo
import com.example.health.ui.battle.ChestReveal
import com.example.health.ui.fantasy.FantasyBar
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ResourceChip
import com.example.health.ui.sprites.HeroAvatar
import com.example.health.ui.sprites.ItemIcon
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Emerald
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.Mana
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import com.example.health.ui.theme.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class MenuItem(val icon: String, val title: String, val subtitle: String, val screen: Screen)

private val menu = listOf(
    MenuItem("🗺️", "모험", "10개 지역 · 던전", Screen.WORLD),
    MenuItem("📜", "모험자 길드", "오늘의 토벌 의뢰", Screen.GUILD),
    MenuItem("🐉", "레이드", "주 1회 · 파티 러닝", Screen.RAID),
    MenuItem("🏪", "상점", "장비 뽑기", Screen.SHOP),
    MenuItem("⚖️", "경매장", "유저 간 거래", Screen.AUCTION),
    MenuItem("⚒️", "대장간", "장비 강화", Screen.SMITH),
    MenuItem("🎒", "영웅 · 가방", "능력치 · 장비", Screen.BAG),
    MenuItem("🏆", "랭킹", "전체 모험가 순위", Screen.RANKING),
    MenuItem("👥", "친구", "동료 모집", Screen.FRIENDS),
    MenuItem("⚙️", "설정", "서버 · 기록 · 로그아웃", Screen.SETTINGS),
)

private val SLOTS = listOf("WEAPON" to "무기", "HELMET" to "투구", "ARMOR" to "갑옷", "BOOTS" to "장화", "ACCESSORY" to "목걸이")
private val WEEKDAYS = listOf("월", "화", "수", "목", "금", "토", "일")

/**
 * 로비. 위에서부터 영웅 → 이번 주 원정(목표·연속 기록) → 원정 지도 → 출석·보상 → 장비 순.
 * 화면에 돌아올 때마다, 그리고 켜 둔 동안 5분마다 러닝·걸음 기록을 자동 동기화한다.
 */
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
    val api = remember { ApiClient(session) }
    val scope = rememberCoroutineScope()

    var dailyClaimed by remember { mutableStateOf(true) }
    var weekly by remember { mutableStateOf<WeeklyInfo?>(null) }
    var claimingWeekly by remember { mutableStateOf(false) }
    /** 열고 있는 보물 상자 (출석 · 주간 원정 보상) */
    var openingChest by remember { mutableStateOf<ChestInfo?>(null) }

    var reward by remember { mutableStateOf<RunReward?>(null) }
    var stepStamina by remember { mutableStateOf(0 to 0) }
    var needPermission by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var lastSyncAt by remember { mutableLongStateOf(session.lastSyncAt) }
    var menuOpen by remember { mutableStateOf(false) }
    /** 처음 들어온 계정이면 사용 안내부터 */
    var showTutorial by remember { mutableStateOf(!session.tutorialDone) }

    suspend fun refreshWeekly() {
        runCatching { weekly = WeeklyInfo.from(api.weekly()) }
    }

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
            stepStamina = result.staminaGained to result.staminaOverflow
            lastSyncAt = session.lastSyncAt
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            syncError = e.message
        } catch (e: ServerUnreachableException) {
            syncError = e.message + "\n(연결되면 자동으로 다시 동기화합니다)"
            SyncWorker.syncSoon(context.applicationContext)
        } catch (e: Exception) {
            syncError = "동기화하지 못했습니다 (${e.message ?: e.javaClass.simpleName})"
        } finally {
            syncing = false
        }
        refreshWeekly()
    }

    fun claimDaily() = scope.launch {
        try {
            val json = api.claimDaily()
            onHeroJson(json.getJSONObject("hero"))
            val chest = json.getJSONObject("chest")
            val item = json.optJSONObject("item")?.let { ItemInfo.from(it) }
            dailyClaimed = true
            openingChest = ChestInfo(
                chest.getString("tier"),
                chest.getString("name"),
                listOfNotNull(
                    "골드 +${json.getLong("gold")}",
                    "강화석 +${json.getInt("stones")}",
                    item?.let { "획득: [${it.gradeName}] ${it.displayName}" },
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            syncError = e.message
        }
    }

    fun claimWeekly() = scope.launch {
        if (claimingWeekly) return@launch
        claimingWeekly = true
        try {
            val json = api.claimWeekly()
            onHeroJson(json.getJSONObject("hero"))
            val chest = json.getJSONObject("chest")
            val item = json.optJSONObject("item")?.let { ItemInfo.from(it) }
            openingChest = ChestInfo(
                chest.getString("tier"),
                chest.getString("name"),
                listOfNotNull(
                    "🔥 ${json.getInt("streak")}주 연속 원정 달성!",
                    "골드 +${json.getLong("gold")}",
                    "강화석 +${json.getInt("stones")}",
                    item?.let { "획득: [${it.gradeName}] ${it.displayName}" },
                ),
            )
            refreshWeekly()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            syncError = e.message
        } finally {
            claimingWeekly = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { scope.launch { syncNow() } }

    // 알림 권한 (Android 13+): 처음 한 번만 묻는다. 거절하면 설정 → 알림에서 다시 켤 수 있음
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        session.notificationsEnabled = granted
    }
    // 튜토리얼과 Health Connect 권한 요청이 끝난 뒤에 묻는다 (권한 창이 겹치지 않도록)
    LaunchedEffect(showTutorial, needPermission) {
        if (showTutorial || needPermission) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 33 && !session.askedNotificationPermission && !Notifier.canNotify(context)) {
            session.askedNotificationPermission = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(resumeKey) {
        runCatching { dailyClaimed = api.dailyStatus().getBoolean("claimedToday") }
        refreshWeekly()
        syncNow()
        // 로비를 켜 둔 동안에도 주기적으로 동기화 (화면을 벗어나거나 앱을 내리면 자동으로 멈춤)
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        while (true) {
            delay(FOREGROUND_SYNC_INTERVAL_MS)
            // 앱이 뒤로 내려가 있으면 건너뜀 (그동안은 SyncWorker 가 담당)
            if (lifecycle == null || lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) syncNow()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBar(hero, onMenu = { menuOpen = true })

            if (needPermission) {
                Panel(title = "운동 기록 권한이 필요해요", highlight = true) {
                    Text(
                        "러닝(경험치·원정)과 걸음수(행동력)를 읽으려면 Health Connect 의 운동·거리·걸음수 읽기를 허용해 주세요.",
                        color = Parchment,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    GoldButton("권한 허용하기", onClick = { permissionLauncher.launch(reader.requestPermissions) }, modifier = Modifier.fillMaxWidth())
                }
            }

            HeroCard(hero)

            // 처음 시작한 모험가에게 게임 방법 안내 (첫 러닝 전까지)
            if (weekly?.totalKm == 0.0) StarterGuide()

            weekly?.let { WeeklyCard(it, claiming = claimingWeekly, onClaim = { claimWeekly() }) }

            if (reward != null || stepStamina.first > 0 || stepStamina.second > 0) {
                RewardPanel(reward, stepStamina.first, stepStamina.second)
            }

            if (!dailyClaimed) DailyChestRow(onClaim = { claimDaily() })

            weekly?.let { ExpeditionCard(it, onOpen = { onNavigate(Screen.WORLD) }) }

            EquipmentStrip(hero, onOpen = { onNavigate(Screen.BAG) })

            SyncStatus(syncing, syncError, lastSyncAt, onSync = { scope.launch { syncNow() } })

            // 아래 바로가기 바에 가리지 않도록 여백
            Spacer(modifier = Modifier.height(84.dp))
        }

        QuickBar(modifier = Modifier.align(Alignment.BottomCenter), onNavigate = onNavigate)

        openingChest?.let { chest ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(onClick = {})
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                ChestReveal(chest, onDone = { openingChest = null })
            }
        }

        if (showTutorial) {
            TutorialOverlay(
                onFinish = {
                    session.tutorialDone = true
                    showTutorial = false
                    // 안내 마지막 장에서 말한 대로 바로 운동 기록 권한 요청
                    scope.launch {
                        val available = reader.sdkStatus() == HealthConnectClient.SDK_AVAILABLE
                        if (available && !reader.hasAllPermissions()) permissionLauncher.launch(reader.requestPermissions)
                    }
                },
            )
        }

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

/** 첫 러닝 전 안내: 무엇을 하면 무엇이 오르는지 */
@Composable
private fun StarterGuide() {
    Panel(title = "📖 모험가 안내서", highlight = true) {
        listOf(
            "🏃 삼성헬스 등으로 러닝을 기록하면 경험치·골드를 받고, 누적 거리만큼 새 지역이 열려요.",
            "👣 걸음 수는 행동력이 됩니다. 행동력으로 길드 토벌·던전·레이드에 도전하세요.",
            "🎯 매주 나에게 맞춘 원정 목표가 생겨요. 달성하면 보물 상자, 3주 연속이면 황금 상자!",
            "🐉 레이드는 친구 4명과 함께. 모두가 이번 주에 많이 뛸수록 강해져요.",
        ).forEach { Text(it, color = Parchment, style = MaterialTheme.typography.bodySmall) }
    }
}

/** 재화 + 우측 상단 메뉴 */
@Composable
private fun TopBar(hero: HeroInfo, onMenu: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ResourceChip("💰", "%,d".format(hero.gold))
        ResourceChip("⚡", "${hero.stamina}")
        ResourceChip("💎", "${hero.enhanceStones}")
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "☰",
            color = Gold,
            fontSize = 22.sp,
            modifier = Modifier
                .border(1.dp, GoldDim, RoundedCornerShape(8.dp))
                .clickable(onClick = onMenu)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

/** 영웅: 초상 · 이름 · 레벨 · 전투력 + 경험치/행동력 바 */
@Composable
private fun HeroCard(hero: HeroInfo) {
    Panel(highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HeroAvatar(hero.job, hero.appearance, 92.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(hero.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                Text(
                    "Lv.${hero.level}  ${Portraits.job(hero.job)} ${hero.jobName}",
                    color = Parchment,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("⚔ 전투력 %,d".format(hero.power), color = Gold, style = MaterialTheme.typography.titleMedium)
            }
        }
        LabeledBar("경험치", "${hero.expInLevel} / ${hero.expToNextLevel}", hero.expProgress, Mana)
        LabeledBar(
            "행동력",
            "${hero.stamina} / ${hero.maxStamina} · 오늘 걸음 +${hero.todayStepStamina}/${hero.dailyStepStaminaCap}",
            hero.staminaProgress,
            Emerald,
        )
        if (hero.statPoints > 0) {
            Text(
                "✦ 능력치 포인트 ${hero.statPoints} 개를 찍을 수 있어요 (영웅 · 가방)",
                color = GoldBright,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LabeledBar(label: String, value: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            Text(value, color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        }
        FantasyBar(progress, color)
    }
}

/** 이번 주 원정: 개인 맞춤 목표 거리 · 진행 · 요일별 거리 · 연속 달성 · 보상 */
@Composable
private fun WeeklyCard(w: WeeklyInfo, claiming: Boolean, onClaim: () -> Unit) {
    Panel(highlight = w.achieved && !w.claimed) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("🏃 이번 주 원정", style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.weight(1f))
            val streak = if (w.achieved) w.streakIfAchieved else w.streakBefore
            if (streak > 0) Text("🔥 ${streak}주 연속", color = GoldBright, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("%.1f".format(w.progressKm), color = GoldBright, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(" / %.1f km".format(w.goalKm), color = Parchment, fontSize = 18.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        FantasyBar(w.progress, if (w.achieved) GoldBright else Emerald)
        Text(
            when {
                w.claimed -> "✔ 이번 주 원정 완료! 다음 주 월요일에 새 목표가 열려요"
                w.achieved -> "목표 달성! 보상 상자를 열어 보세요"
                else -> "%.1fkm 남음 · ${w.daysLeft}일 남음 · 이번 주 ${w.runDays}일 뜀".format(w.remainingKm)
            },
            color = if (w.achieved) GoldBright else ParchmentDim,
            style = MaterialTheme.typography.bodySmall,
        )
        WeekChart(w.dayKm)
        if (!w.claimed) {
            Text(
                "달성 보상: ${w.rewardChestName} · 골드 %,d · 강화석 ${w.rewardStones}".format(w.rewardGold) +
                    if (w.streakIfAchieved < 3) "  (3주 연속이면 황금 상자)" else "",
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (w.achieved && !w.claimed) {
            GoldButton(if (claiming) "..." else "원정 보상 받기", onClick = onClaim, enabled = !claiming, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 월~일 막대 그래프 (오늘 강조) */
@Composable
private fun WeekChart(dayKm: List<Double>) {
    val today = LocalDate.now().dayOfWeek.value - 1
    val max = (dayKm.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        dayKm.forEachIndexed { i, km ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (km > 0) "%.1f".format(km) else "", color = ParchmentDim, fontSize = 10.sp)
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(44.dp)
                        .background(Track, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (km > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((km / max * 44).coerceAtLeast(4.0).dp)
                                .background(if (i == today) GoldBright else Emerald, RoundedCornerShape(4.dp)),
                        )
                    }
                }
                Text(
                    WEEKDAYS.getOrElse(i) { "" },
                    color = if (i == today) GoldBright else ParchmentDim,
                    fontSize = 11.sp,
                    fontWeight = if (i == today) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/** 원정 지도: 누적 거리로 열리는 10개 지역 + 개인 기록 */
@Composable
private fun ExpeditionCard(w: WeeklyInfo, onOpen: () -> Unit) {
    Panel(modifier = Modifier.clickable(onClick = onOpen)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("🗺️ 원정 지도", style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.weight(1f))
            Text("누적 %.1f km".format(w.totalKm), color = Parchment, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            (1..10).forEach { tier ->
                val unlocked = tier <= w.unlockedTier
                val next = tier == w.unlockedTier + 1
                Box(
                    modifier = Modifier
                        .size(if (next) 18.dp else 14.dp)
                        .background(if (unlocked) Gold else Track, CircleShape)
                        .border(1.dp, if (next) GoldBright else Color.Transparent, CircleShape),
                )
            }
        }
        if (w.nextRegionName != null && w.nextRegionKm != null) {
            FantasyBar(w.progressToNext.toFloat(), Gold)
            Text(
                "다음 지역 「${w.nextRegionName}」까지 %.1fkm".format((w.nextRegionKm - w.totalKm).coerceAtLeast(0.0)),
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text("모든 지역을 개척했습니다! 🏰", color = GoldBright, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RecordCell("최장 거리", "%.1f km".format(w.longestRunKm))
            RecordCell("최고 페이스", formatPace(w.bestPaceSecPerKm) + " /km")
            RecordCell("이번 주", "${w.runDays}일")
        }
    }
}

@Composable
private fun RecordCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Parchment, fontWeight = FontWeight.Bold)
        Text(label, color = ParchmentDim, fontSize = 11.sp)
    }
}

@Composable
private fun DailyChestRow(onClaim: () -> Unit) {
    Panel(highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎁", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("오늘의 출석 상자", color = Gold, fontWeight = FontWeight.Bold)
                Text("하루 한 번 · 황금 상자엔 장비가!", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            }
            GoldButton("열기", onClick = onClaim)
        }
    }
}

/** 장착 장비 5칸을 한 줄로 (누르면 영웅 · 가방) */
@Composable
private fun EquipmentStrip(hero: HeroInfo, onOpen: () -> Unit) {
    Panel(modifier = Modifier.clickable(onClick = onOpen)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("🛡 장착 장비", style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.weight(1f))
            Text("관리 ▶", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SLOTS.forEach { (slot, label) ->
                val item = hero.equipment.firstOrNull { it.slot == slot }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ItemIcon(item, 46.dp, emptySlot = slot)
                    Text(
                        item?.let { if (it.enhance > 0) "+${it.enhance}" else label } ?: label,
                        color = if (item != null) Parchment else ParchmentDim,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hero.stats.forEach { s ->
                Text("${s.name} ${s.total}", color = Parchment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RewardPanel(r: RunReward?, stamina: Int, overflow: Int) {
    Panel(title = "✨ 새 활동 보상", highlight = true) {
        if (r != null) {
            if (r.leveledUp) {
                Text("🎉 레벨 업! Lv.${r.levelBefore} → Lv.${r.levelAfter}  (포인트 +${r.statPointsGained})", color = GoldBright, fontWeight = FontWeight.Bold)
            }
            Text("🏃 러닝 %.2fkm → EXP +${r.expGained} · 골드 +${r.goldGained}".format(r.countedKm), color = Gold)
            if (r.tiredKm > 0.01) {
                Text(
                    "하루 10km 를 넘은 %.1fkm 는 보상 절반이에요. 무리하지 말고 내일 또 뛰어요!".format(r.tiredKm),
                    color = ParchmentDim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            r.rejected.forEach { Text("⚠ 보상 제외: $it", color = Crimson, style = MaterialTheme.typography.bodySmall) }
        }
        if (stamina > 0) Text("👣 걸음 → 행동력 +$stamina", color = Gold)
        if (overflow > 0) {
            Text("행동력이 가득 차서 $overflow 은(는) 받지 못했어요. 컨텐츠에 써 주세요!", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SyncStatus(syncing: Boolean, error: String?, lastSyncAt: Long, onSync: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        error?.let { Text("⚠ $it", color = Crimson, style = MaterialTheme.typography.bodySmall) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (syncing) "기록 동기화 중..." else "마지막 동기화 ${formatTime(lastSyncAt)} · 자동 동기화 켜짐",
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            GhostButton("지금 동기화", onClick = onSync, enabled = !syncing)
        }
    }
}

/** 하단 바로가기: 모험 · 모험자 길드 · 레이드 · 영웅 가방 (나머지는 우측 상단 ☰) */
@Composable
private fun QuickBar(modifier: Modifier, onNavigate: (Screen) -> Unit) {
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
            Triple("🎒", "영웅·가방", Screen.BAG),
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
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            GhostButton("닫기", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MenuTile(item: MenuItem, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .border(1.dp, GoldDim, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(item.icon, fontSize = 26.sp)
        Text(item.title, color = Gold, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Text(item.subtitle, color = ParchmentDim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("M/d HH:mm")

private fun formatTime(epochMillis: Long): String =
    if (epochMillis <= 0) "없음" else timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** 로비를 켜 둔 동안 다시 동기화하는 간격 (5분) */
private const val FOREGROUND_SYNC_INTERVAL_MS = 5 * 60 * 1000L
