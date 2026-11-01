package com.example.health.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import com.example.health.game.HeroInfo
import com.example.health.game.ItemInfo
import com.example.health.game.Portraits
import com.example.health.game.RegionInfo
import com.example.health.net.ApiClient
import com.example.health.ui.battle.BattleOverlay
import com.example.health.ui.battle.BattleSpec
import com.example.health.ui.battle.ChestInfo
import com.example.health.ui.battle.Fighter
import com.example.health.ui.battle.simpleBattle
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.sprites.HeroSprites
import com.example.health.ui.sprites.MonsterSprites
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ResourceChip
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

private data class DungeonResult(val dungeon: String, val stones: Int, val gold: Long, val item: ItemInfo?)

/** 세계 지도: 초보자 마을 ~ 마왕성 외곽 10개 지역과 각 지역 던전 */
@Composable
fun WorldScreen(api: ApiClient, hero: HeroInfo, onHeroJson: (JSONObject) -> Unit, onBack: () -> Unit) {
    var regions by remember { mutableStateOf<List<RegionInfo>>(emptyList()) }
    var result by remember { mutableStateOf<DungeonResult?>(null) }
    var battle by remember { mutableStateOf<BattleSpec?>(null) }
    val actions = rememberActions()

    suspend fun load() {
        regions = RegionInfo.listFrom(api.world())
    }

    LaunchedEffect(Unit) { actions.run { load(); null } }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScreenHeader("세계 지도", onBack)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ResourceChip("Lv", "${hero.level}")
                    ResourceChip("능력치 합", "${hero.totalStats}")
                    ResourceChip("⚡", "${hero.stamina}")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                result?.let { r ->
                    Panel(title = "🏆 ${r.dungeon} 공략 성공!", highlight = true) {
                        Text("강화석 +${r.stones} · 골드 +${r.gold}", color = Gold)
                        val item = r.item
                        if (item != null) {
                            Text("획득: [${item.gradeName}] ${item.displayName}", color = gradeColor(item.gradeName), fontWeight = FontWeight.Bold)
                            Text("${item.slotName} · ${item.statText}" + if (item.equipped) " (빈 부위라 자동 장착)" else "", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("이번에는 장비가 나오지 않았습니다", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                ActionFeedback(actions)
            }
        }
        items(regions, key = { it.tier }) { region ->
            RegionCard(region, enabled = !actions.busy) {
                actions.run {
                    val json = api.dungeon(region.tier)
                    onHeroJson(json.getJSONObject("hero"))
                    val victory = json.getBoolean("victory")
                    val chestJson = json.optJSONObject("chest")
                    val r = DungeonResult(
                        dungeon = json.getString("dungeon"),
                        stones = json.getInt("rewardStones"),
                        gold = json.getLong("rewardGold"),
                        item = if (json.isNull("item")) null else ItemInfo.from(json.getJSONObject("item")),
                    )
                    result = r.takeIf { victory }
                    battle = simpleBattle(
                        title = "던전: ${region.dungeonName}",
                        hero = Fighter(hero.name, HeroSprites.hero(hero.job, hero.appearance)),
                        enemy = Fighter(region.boss, MonsterSprites.monster(region.boss)),
                        enemyHp = region.minTotalStats * 40L + 500,
                        hits = 6,
                        resultTitle = if (victory) "🏆 ${r.dungeon} 공략 성공!" else "${r.dungeon} 공략 실패",
                        resultLines = if (victory) {
                            listOf("${region.boss} 처치!")
                        } else {
                            listOf("승률 ${json.getInt("winChance")}% 에서 패배했습니다", "행동력만 소모 · 능력치를 올리면 승률이 오릅니다")
                        },
                        victory = victory,
                        chest = chestJson?.let {
                            ChestInfo(
                                it.getString("tier"),
                                it.getString("name"),
                                listOf(
                                    "강화석 +${r.stones}",
                                    "골드 +${r.gold}",
                                    r.item?.let { item -> "획득: [${item.gradeName}] ${item.displayName} (${item.statText})" }
                                        ?: "장비는 나오지 않았습니다",
                                ),
                            )
                        },
                        enemySize = 170.dp,
                        heroMaxHp = hero.power * 2L,
                        heroLoss = (region.minTotalStats.toFloat() / hero.totalStats.coerceAtLeast(1) * 0.75f).coerceIn(0.15f, 0.9f),
                        seed = region.tier,
                    )
                    load()
                    null
                }
            }
        }
        item { Text("") }
    }
    battle?.let { BattleOverlay(it) { battle = null } }
    }
}

@Composable
private fun RegionCard(region: RegionInfo, enabled: Boolean, onEnter: () -> Unit) {
    Panel(highlight = region.available) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Portraits.region(region.tier), fontSize = 34.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("${region.tier}. ${region.name}", style = MaterialTheme.typography.titleMedium, color = GoldBright)
                Text(region.description, color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("⚔ 던전: ${region.dungeonName} · 보스 ${region.boss}" + if (region.clears > 0) "  (공략 ${region.clears}회)" else "", color = Parchment, fontWeight = FontWeight.SemiBold)
        Text("입장 조건: Lv.${region.minLevel} · 능력치 합 ${region.minTotalStats} · 행동력 ${region.staminaCost}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        Text("기본 보상: 강화석 ${region.rewardStones} · 골드 ${region.rewardGold} · 장비 (상자 등급에 따라 50~100%)", color = Gold, style = MaterialTheme.typography.bodySmall)
        if (region.available) {
            Text("승률 ${region.winChance}%", color = if (region.winChance >= 90) GoldBright else if (region.winChance >= 70) Gold else Crimson, fontWeight = FontWeight.Bold)
        }
        Text("장비 등급: ${region.dropGrades}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        if (!region.available) {
            Text("부족: " + region.reasons.joinToString(", "), color = Crimson, style = MaterialTheme.typography.bodySmall)
        }
        GoldButton("던전 공략 ⚡${region.staminaCost}", onClick = onEnter, enabled = enabled && region.available, modifier = Modifier.fillMaxWidth())
    }
}
