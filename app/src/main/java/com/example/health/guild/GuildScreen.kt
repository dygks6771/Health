package com.example.health.guild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.health.game.GuildBoard
import com.example.health.game.HeroInfo
import com.example.health.game.HuntInfo
import com.example.health.net.ApiClient
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ResourceChip
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.battle.BattleOverlay
import com.example.health.ui.battle.BattleSpec
import com.example.health.ui.battle.ChestInfo
import com.example.health.ui.battle.Fighter
import com.example.health.ui.battle.simpleBattle
import com.example.health.ui.sprites.HeroSprites
import com.example.health.ui.sprites.MonsterAvatar
import com.example.health.ui.sprites.MonsterSprites
import com.example.health.ui.sprites.NpcBanner
import com.example.health.ui.sprites.NpcSprites
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Emerald
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.Mana
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

/** 모험자 길드: 매일 바뀌는 토벌 의뢰 5개 (쉬움 2 · 적정 2 · 도전 1) */
@Composable
fun GuildScreen(api: ApiClient, hero: HeroInfo, onHeroJson: (JSONObject) -> Unit, onBack: () -> Unit) {
    var board by remember { mutableStateOf<GuildBoard?>(null) }
    var battle by remember { mutableStateOf<BattleSpec?>(null) }
    val actions = rememberActions()

    suspend fun load() {
        board = GuildBoard.from(api.guild())
    }

    LaunchedEffect(Unit) { actions.run { load(); null } }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("모험자 길드", onBack)
        NpcBanner(NpcSprites.receptionist, "${hero.name} 님, 오늘의 토벌 의뢰예요! 의뢰는 매일 자정에 새로 붙는답니다.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResourceChip("⚔", "전투력 ${board?.power ?: hero.power}")
            ResourceChip("⚡", "${hero.stamina}")
            ResourceChip("💰", "%,d".format(hero.gold))
        }
        ActionFeedback(actions)
        board?.hunts?.forEach { hunt ->
            HuntCard(hunt, enabled = !actions.busy) {
                actions.run {
                    val json = api.hunt(hunt.slot)
                    onHeroJson(json.getJSONObject("hero"))
                    val victory = json.getBoolean("victory")
                    val chest = json.optJSONObject("chest")
                    battle = simpleBattle(
                        title = "토벌: ${hunt.monster}",
                        hero = Fighter(hero.name, HeroSprites.hero(hero.job, hero.appearance)),
                        enemy = Fighter("Lv.${hunt.targetLevel} ${hunt.monster}", MonsterSprites.monster(hunt.monster)),
                        enemyHp = hunt.requiredPower * 3L,
                        hits = if (hunt.difficulty == "HARD") 5 else 4,
                        resultTitle = if (victory) "🏆 토벌 성공!" else "토벌 실패",
                        resultLines = if (victory) {
                            listOf("${hunt.monster} 토벌 완료")
                        } else {
                            listOf("승률 ${json.getInt("winChance")}% 에서 패배했습니다", "행동력만 소모 · 의뢰는 다시 도전할 수 있어요")
                        },
                        victory = victory,
                        chest = chest?.let {
                            ChestInfo(
                                it.getString("tier"),
                                it.getString("name"),
                                listOfNotNull(
                                    "골드 +${json.getInt("rewardGold")}",
                                    json.getInt("rewardStones").takeIf { s -> s > 0 }?.let { s -> "강화석 +$s" },
                                ),
                            )
                        },
                        heroMaxHp = hero.power * 2L,
                        // 권장 전투력이 내 전투력에 가까울수록 많이 맞음 (도전 의뢰는 아슬아슬하게)
                        heroLoss = (hunt.requiredPower.toFloat() / hero.power.coerceAtLeast(1) * 0.7f).coerceIn(0.1f, 0.9f),
                        seed = hunt.slot,
                    )
                    load()
                    null
                }
            }
        }
    }
    battle?.let { BattleOverlay(it) { battle = null } }
    }
}

@Composable
private fun HuntCard(hunt: HuntInfo, enabled: Boolean, onHunt: () -> Unit) {
    val color = when (hunt.difficulty) {
        "EASY" -> Emerald
        "HARD" -> Crimson
        else -> Mana
    }
    Panel(highlight = hunt.difficulty == "HARD" && !hunt.cleared) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonsterAvatar(hunt.monster, 48.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text("[${hunt.difficultyName}]", color = color, fontWeight = FontWeight.Bold)
                Text("${hunt.monster} 토벌", style = MaterialTheme.typography.titleMedium, color = GoldBright)
            }
            if (hunt.cleared) Text("완료 ✓", color = Emerald, fontWeight = FontWeight.Bold)
        }
        Text("출몰 지역: ${hunt.regionName} · 적 레벨 ${hunt.targetLevel}", color = Parchment, style = MaterialTheme.typography.bodySmall)
        Text("권장 전투력 ${hunt.requiredPower} · 기본 보상 골드 ${hunt.rewardGold}", color = Gold, style = MaterialTheme.typography.bodySmall)
        if (!hunt.cleared) {
            Text(
                "승률 ${hunt.winChance}%",
                color = when {
                    hunt.winChance >= 90 -> Emerald
                    hunt.winChance >= 60 -> Gold
                    else -> Crimson
                },
                fontWeight = FontWeight.Bold,
            )
        }
        if (!hunt.available && !hunt.cleared) {
            Text("부족: " + hunt.reasons.joinToString(", "), color = Crimson, style = MaterialTheme.typography.bodySmall)
        }
        if (!hunt.cleared) {
            GoldButton("토벌 출발 ⚡${hunt.staminaCost}", onClick = onHunt, enabled = enabled && hunt.available)
        }
    }
}
