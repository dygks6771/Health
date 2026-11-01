package com.example.health.world

import androidx.compose.foundation.layout.Arrangement
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
import com.example.health.ui.fantasy.ActionFeedback
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
    val actions = rememberActions()

    suspend fun load() {
        regions = RegionInfo.listFrom(api.world())
    }

    LaunchedEffect(Unit) { actions.run { load(); null } }

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
                    result = DungeonResult(
                        dungeon = json.getString("dungeon"),
                        stones = json.getInt("rewardStones"),
                        gold = json.getLong("rewardGold"),
                        item = if (json.isNull("item")) null else ItemInfo.from(json.getJSONObject("item")),
                    )
                    load()
                    null
                }
            }
        }
        item { Text("") }
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
        Text("⚔ 던전: ${region.dungeonName}" + if (region.clears > 0) "  (공략 ${region.clears}회)" else "", color = Parchment, fontWeight = FontWeight.SemiBold)
        Text("입장 조건: Lv.${region.minLevel} · 능력치 합 ${region.minTotalStats} · 행동력 ${region.staminaCost}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        Text("보상: 강화석 ${region.rewardStones} · 골드 ${region.rewardGold} · 장비 ${region.dropRate}%", color = Gold, style = MaterialTheme.typography.bodySmall)
        Text("장비 등급: ${region.dropGrades}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        if (!region.available) {
            Text("부족: " + region.reasons.joinToString(", "), color = Crimson, style = MaterialTheme.typography.bodySmall)
        }
        GoldButton("던전 공략 ⚡${region.staminaCost}", onClick = onEnter, enabled = enabled && region.available, modifier = Modifier.fillMaxWidth())
    }
}
