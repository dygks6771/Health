package com.example.health.raid

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
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.FantasyBar
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.fantasy.rememberActions
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
    val actions = rememberActions()

    suspend fun load() {
        info = RaidInfo.from(api.raid())
    }

    LaunchedEffect(Unit) { actions.run { load(); null } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("레이드", onBack)
        ActionFeedback(actions)
        val raid = info ?: return@Column

        Panel(title = "${Portraits.raidStage(1)} ${raid.bossName}", highlight = true) {
            Text("파티 ${raid.partySize}명 · ${raid.turns}턴 · 행동력 ${raid.staminaCost}", color = Parchment)
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
                    Text(Portraits.appearance(f.appearance), fontSize = 22.sp)
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
                        load()
                        null
                    }
                },
                enabled = !actions.busy && selected.size == raid.partySize - 1 && hero.stamina >= raid.staminaCost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RaidResultPanel(r: RaidResult) {
    Panel(title = "전투 결과", highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(r.grade, fontSize = 56.sp, color = raidGradeColor(r.grade), fontWeight = FontWeight.Black)
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
