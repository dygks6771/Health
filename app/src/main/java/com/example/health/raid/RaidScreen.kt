package com.example.health.raid

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.game.HeroInfo
import com.example.health.game.Portraits
import com.example.health.game.RaidInfo
import com.example.health.game.RaidResult
import com.example.health.net.ApiClient
import com.example.health.ui.battle.BattleOverlay
import com.example.health.ui.battle.BattleRound
import com.example.health.ui.battle.BattleSpec
import com.example.health.ui.battle.Fighter
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.sprites.HeroSprites
import com.example.health.ui.fantasy.FantasyBar
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.sprites.DragonSprite
import com.example.health.ui.sprites.HeroAvatar
import com.example.health.ui.sprites.PixelSprite
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.GradeEpic
import com.example.health.ui.theme.GradeLegendary
import com.example.health.ui.theme.GradeRare
import com.example.health.ui.theme.GradeUncommon
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

private fun raidGradeColor(grade: String): Color = when (grade) {
    "S" -> GradeLegendary
    "A" -> GradeEpic
    "B" -> GradeRare
    "C" -> GradeUncommon
    "D", "E" -> Parchment
    else -> Crimson
}

/** 레이드: 본인 + 친구 4명으로 고대 마룡에 도전. 데미지가 쌓일수록 마룡이 진화(4단계) */
@Composable
fun RaidScreen(api: ApiClient, hero: HeroInfo, onHeroJson: (JSONObject) -> Unit, onBack: () -> Unit) {
    var info by remember { mutableStateOf<RaidInfo?>(null) }
    var result by remember { mutableStateOf<RaidResult?>(null) }
    val selected = remember { mutableStateListOf<Long>() }
    var battle by remember { mutableStateOf<BattleSpec?>(null) }
    val actions = rememberActions()

    suspend fun load() {
        info = RaidInfo.from(api.raid())
    }

    LaunchedEffect(Unit) { actions.run { load(); null } }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("레이드", onBack)
        ActionFeedback(actions)
        val raid = info ?: return@Column

        Panel(title = raid.bossName, highlight = true) {
            val stage = result?.finalStage ?: 1
            PixelSprite(DragonSprite.stage(stage), Modifier.align(Alignment.CenterHorizontally), size = (110 + stage * 20).dp)
            Text(
                if (result == null) "깊은 잠에서 깨어난 마룡이 모험가들을 기다린다..." else "${stage}단계 ${result?.finalStageName}",
                color = GoldBright,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text("파티 ${raid.partySize}명 · ${raid.turns}턴 · 행동력 ${raid.staminaCost} · 주 1회", color = Parchment)
            Text(
                if (raid.remainingThisWeek > 0) "이번 주 도전 가능" else "이번 주 도전 완료 — 월요일 0시에 초기화",
                color = if (raid.remainingThisWeek > 0) Gold else Crimson,
                fontWeight = FontWeight.Bold,
            )
            raid.stages.forEach { s ->
                Text(
                    "${s.stage}단계 ${s.name} — 누적 %,d 부터 · 받는 피해 %d%%".format(s.threshold, (s.damageRate * 100).toInt()),
                    color = ParchmentDim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (raid.bestGrade != null) {
                Text("최고 기록: ${raid.bestGrade} 등급 · %,d".format(raid.bestDamage ?: 0L), color = raidGradeColor(raid.bestGrade), fontWeight = FontWeight.Bold)
            }
        }

        Panel(title = "보상 등급") {
            raid.grades.forEach { g ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(g.grade, color = raidGradeColor(g.grade), fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f))
                    Text("%,d+".format(g.minDamage), color = Parchment, modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.bodySmall)
                    Text(
                        "골드 ${g.gold} · 강화석 ${g.stones}" + (g.itemGrade?.let { " · $it 목걸이" } ?: ""),
                        color = Gold,
                        modifier = Modifier.weight(0.55f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        result?.let { RaidResultPanel(it) }

        Panel(title = "파티 편성 (${selected.size + 1}/${raid.partySize})") {
            Text("👑 ${hero.name} · Lv.${hero.level} ${hero.jobName} · 전투력 ${hero.power}", color = GoldBright)
            if (raid.friends.size < raid.partySize - 1) {
                Text(
                    "친구가 ${raid.partySize - 1}명 이상 필요합니다 (현재 ${raid.friends.size}명). 친구 메뉴에서 동료를 모으세요.",
                    color = Crimson,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            raid.friends.forEach { f ->
                val checked = f.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (checked) Gold else GoldDim, RoundedCornerShape(8.dp))
                        .clickable {
                            if (checked) selected.remove(f.id) else if (selected.size < raid.partySize - 1) selected.add(f.id)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(if (checked) "☑" else "☐", color = Gold)
                    HeroAvatar(f.job, f.appearance, 36.dp)
                    Text("${f.name} · Lv.${f.level} ${f.jobName}", color = Parchment, modifier = Modifier.weight(1f))
                    Text("${f.power}", color = Gold)
                }
            }
            GoldButton(
                "마룡 토벌 ⚡${raid.staminaCost}",
                onClick = {
                    actions.run {
                        val json = api.attackRaid(selected.toList())
                        val r = RaidResult.from(json)
                        onHeroJson(json.getJSONObject("hero"))
                        result = r
                        battle = raidBattle(r, raid)
                        load()
                        null
                    }
                },
                enabled = !actions.busy && selected.size == raid.partySize - 1 &&
                    hero.stamina >= raid.staminaCost && raid.remainingThisWeek > 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Panel(title = "🛠 개발용") {
            Text("테스트를 위해 이번 주 레이드 제한을 초기화합니다 (서버 개발 모드에서만)", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            GhostButton("주간 제한 초기화", onClick = { actions.run { api.devRaidReset(); load(); "초기화했습니다" } }, enabled = !actions.busy)
        }
    }
    battle?.let { BattleOverlay(it) { battle = null } }
    }
}

/** 서버가 계산한 10턴 결과를 전투 장면으로: 턴마다 한 명씩 돌진, 누적 데미지가 기준을 넘으면 마룡 진화 */
private fun raidBattle(r: RaidResult, info: RaidInfo): BattleSpec {
    val party = r.members.map { Fighter(it.name, HeroSprites.hero(it.job, it.appearance)) }
    fun hpAfter(total: Long, stage: Int): Float {
        val current = info.stages.firstOrNull { it.stage == stage }?.threshold ?: 0L
        val next = info.stages.firstOrNull { it.stage == stage + 1 }?.threshold ?: (current * 2).coerceAtLeast(1L)
        return 1f - ((total - current).toFloat() / (next - current).coerceAtLeast(1L)).coerceIn(0f, 1f)
    }
    var previousStage = 1
    // 파티 체력은 연출용: 진화 단계가 높을수록 마룡의 브레스가 아프다 (전멸하지는 않음)
    val partyMaxHp = 10_000L
    var partyHp = partyMaxHp
    val breathRate = listOf(0.03, 0.05, 0.07, 0.09)
    val rounds = r.turns.flatMap { t ->
        val evolved = t.stage != previousStage
        previousStage = t.stage
        val hpNow = hpAfter(t.totalDamage, t.stage)
        val attack = BattleRound(
            attacker = (t.turn - 1) % party.size.coerceAtLeast(1),
            damage = t.damage,
            enemyHpAfter = hpNow,
            enemySprite = if (evolved) DragonSprite.stage(t.stage) else null,
            caption = if (evolved) "⚠ 마룡이 진화했다! ${t.stage}단계 ${t.stageName}" else "${t.turn}턴 · 누적 %,d".format(t.totalDamage),
        )
        if (t.turn % 2 == 0) {
            val breath = (partyMaxHp * breathRate[(t.stage - 1).coerceIn(0, 3)]).toLong().coerceAtMost(partyHp - 1)
            partyHp -= breath
            listOf(
                attack,
                BattleRound(
                    attacker = 0,
                    damage = breath,
                    enemyHpAfter = hpNow,
                    enemyAttack = true,
                    heroHpAfter = partyHp.toFloat() / partyMaxHp,
                    caption = "🔥 ${t.stageName}의 브레스!",
                ),
            )
        } else {
            listOf(attack)
        }
    }
    return BattleSpec(
        title = "레이드: ${info.bossName}",
        heroes = party,
        enemy = Fighter(info.bossName, DragonSprite.stage(1)),
        enemySize = 180.dp,
        rounds = rounds,
        resultTitle = "레이드 결과: ${r.grade} 등급",
        resultLines = listOfNotNull(
            "총 데미지 %,d · 최종 ${r.finalStage}단계 ${r.finalStageName}".format(r.totalDamage),
            "골드 +${r.rewardGold} · 강화석 +${r.rewardStones}",
            r.item?.let { "획득: [${it.gradeName}] ${it.displayName} (${it.statText})" },
        ),
    )
}

@Composable
private fun RaidResultPanel(r: RaidResult) {
    Panel(title = "전투 결과", highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(r.grade, fontSize = 56.sp, color = raidGradeColor(r.grade), fontWeight = FontWeight.Black)
            PixelSprite(DragonSprite.stage(r.finalStage), size = 56.dp)
            Column {
                Text("총 데미지 %,d".format(r.totalDamage), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text("${Portraits.raidStage(r.finalStage)} 최종 형태: ${r.finalStage}단계 ${r.finalStageName}", color = Parchment)
            }
        }
        val maxTurn = r.turns.maxOfOrNull { it.damage } ?: 1L
        r.turns.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${t.turn}턴", color = ParchmentDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.15f))
                FantasyBar(t.damage.toFloat() / maxTurn, Crimson, Modifier.weight(0.45f))
                Text(
                    "%,d".format(t.damage) + if (t.evolved) " ⚠ ${t.stageName}로 진화!" else "",
                    color = if (t.evolved) Crimson else Parchment,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(0.4f),
                )
            }
        }
        Text("파티 기여", color = Gold, fontWeight = FontWeight.Bold)
        r.members.forEach { m ->
            Text("${m.name} (${m.jobName}) — %,d".format(m.damage), color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
        Text("보상: 골드 +${r.rewardGold} · 강화석 +${r.rewardStones}", color = Gold)
        r.item?.let { item ->
            Text("획득: [${item.gradeName}] ${item.displayName} (${item.statText})", color = gradeColor(item.gradeName), fontWeight = FontWeight.Bold)
        }
    }
}
