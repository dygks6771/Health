package com.example.health.smith

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
import com.example.health.net.ApiClient
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ResourceChip
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.sprites.ItemIcon
import com.example.health.ui.sprites.NpcBanner
import com.example.health.ui.sprites.NpcSprites
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Emerald
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

/** 대장간: 장비를 골라 강화 (+15 까지, 단계가 오를수록 성공률 하락, 실패해도 단계 유지) */
@Composable
fun SmithScreen(api: ApiClient, hero: HeroInfo, onHeroJson: (JSONObject) -> Unit, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<ItemInfo>>(emptyList()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val actions = rememberActions()

    suspend fun load() {
        val json = api.items()
        items = ItemInfo.listFrom(json.getJSONArray("items"))
        onHeroJson(json.getJSONObject("hero"))
    }

    LaunchedEffect(Unit) { actions.run { load(); null } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("대장간", onBack)
        NpcBanner(NpcSprites.blacksmith, "+25 까지 두드려 주지. 다만 +5 부터는 실패하면 쇠가 상해 단계가 떨어지네. 높을수록 더 크게!")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResourceChip("💎", "강화석 ${hero.enhanceStones}")
            ResourceChip("💰", "%,d".format(hero.gold))
        }

        val selected = items.firstOrNull { it.id == selectedId }
        Panel(title = "⚒️ 강화대", highlight = selected != null) {
            if (selected == null) {
                Text("아래 목록에서 강화할 장비를 고르세요", color = ParchmentDim)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ItemIcon(selected, 56.dp)
                    Column {
                        Text("[${selected.gradeName}] ${selected.displayName}", color = gradeColor(selected.gradeName), style = MaterialTheme.typography.titleMedium)
                        Text("${selected.slotName} · ${selected.statText}", color = Parchment)
                    }
                }
                val rate = selected.nextEnhanceRate
                val stones = selected.nextEnhanceStones
                val gold = selected.nextEnhanceGold
                if (rate == null || stones == null || gold == null) {
                    Text("✦ 최대 강화(+25) 달성", color = Gold, fontWeight = FontWeight.Bold)
                } else {
                    Text("+${selected.enhance} → +${selected.enhance + 1}   성공률 $rate%", color = if (rate >= 70) Emerald else Crimson, fontWeight = FontWeight.Bold)
                    Text("비용: 강화석 $stones · 골드 %,d".format(gold), color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (selected.failDrop > 0) "⚠ 실패하면 -${selected.failDrop}강 (+${selected.enhance - selected.failDrop} 로 하락)" else "실패해도 단계 유지",
                        color = if (selected.failDrop > 0) Crimson else Emerald,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    GoldButton(
                        "강화하기",
                        onClick = {
                            actions.run {
                                val json = api.enhance(selected.id)
                                onHeroJson(json.getJSONObject("hero"))
                                load()
                                when {
                                    json.getBoolean("success") -> "🔨 강화 성공! +${json.getInt("enhanceAfter")}"
                                    json.getInt("dropped") > 0 -> "💥 강화 실패... +${json.getInt("enhanceBefore")} → +${json.getInt("enhanceAfter")}"
                                    else -> "💥 강화 실패... (+${json.getInt("enhanceBefore")} 유지)"
                                }
                            }
                        },
                        enabled = !actions.busy && hero.enhanceStones >= stones && hero.gold >= gold,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            ActionFeedback(actions)
        }

        Panel(title = "보유 장비 (${items.size})") {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isSelected) Gold else GoldDim, RoundedCornerShape(8.dp))
                        .clickable { selectedId = item.id }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ItemIcon(item, 36.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            (if (item.equipped) "[장착] " else if (item.listed) "[경매 중] " else "") + item.displayName,
                            color = gradeColor(item.gradeName),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("${item.gradeName} · ${item.slotName} · ${item.statText}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(item.nextEnhanceRate?.let { "$it%" } ?: "MAX", color = Gold, fontSize = 13.sp)
                }
            }
        }
    }
}
