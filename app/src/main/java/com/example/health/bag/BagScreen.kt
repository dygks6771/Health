package com.example.health.bag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import com.example.health.game.HeroInfo
import com.example.health.game.ItemInfo
import com.example.health.game.Portraits
import com.example.health.net.ApiClient
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

private val slotOrder = listOf("WEAPON" to "무기", "ARMOR" to "갑옷", "BOOTS" to "장화", "ACCESSORY" to "목걸이")

/** 영웅 · 가방: 능력치 찍기, 장착 장비, 보유 장비 장착/판매 */
@Composable
fun BagScreen(api: ApiClient, hero: HeroInfo, onHeroJson: (JSONObject) -> Unit, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<ItemInfo>>(emptyList()) }
    val actions = rememberActions()

    suspend fun load() {
        val json = api.items()
        items = ItemInfo.listFrom(json.getJSONArray("items"))
        onHeroJson(json.getJSONObject("hero"))
    }

    /** 장착·판매 응답도 {items, hero} 형태 */
    fun applyResponse(json: JSONObject) {
        items = ItemInfo.listFrom(json.getJSONArray("items"))
        onHeroJson(json.getJSONObject("hero"))
    }

    LaunchedEffect(Unit) { actions.run { load(); null } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("영웅 · 가방", onBack)

        Panel(highlight = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Portraits.appearance(hero.appearance), fontSize = 48.sp)
                Column {
                    Text(hero.name, style = MaterialTheme.typography.titleLarge, color = GoldBright)
                    Text("Lv.${hero.level} ${Portraits.job(hero.job)} ${hero.jobName} (${hero.jobRole})", color = Parchment)
                    Text("⚔ 전투력 %,d".format(hero.power), color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        }

        Panel(title = "능력치  ·  남은 포인트 ${hero.statPoints}") {
            hero.stats.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(s.name, color = Parchment, modifier = Modifier.weight(0.3f))
                    Text(
                        "${s.total}" + if (s.bonus > 0) "  (${s.base} + 장비 ${s.bonus})" else "",
                        color = GoldBright,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.5f),
                    )
                    GhostButton(
                        "+1",
                        onClick = { actions.run { onHeroJson(api.allocateStat(s.code)); null } },
                        enabled = !actions.busy && hero.statPoints > 0,
                    )
                }
            }
            Text("레벨업마다 포인트 3 + 직업 주 능력치 +1", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        }

        Panel(title = "장착 장비") {
            slotOrder.forEach { (slot, label) ->
                val item = hero.equipment.firstOrNull { it.slot == slot }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(label, color = ParchmentDim, modifier = Modifier.weight(0.25f))
                    Text(
                        item?.let { "${it.displayName}  ${it.statText}" } ?: "— 비어 있음",
                        color = item?.let { gradeColor(it.gradeName) } ?: ParchmentDim,
                        modifier = Modifier.weight(0.75f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        ActionFeedback(actions)
        Panel(title = "가방 (${items.size})") {
            items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        (if (item.equipped) "[장착] " else "") + "[${item.gradeName}] ${item.displayName}",
                        color = gradeColor(item.gradeName),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("${item.slotName} · 티어 ${item.tier} · ${item.statText}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.equipped) {
                            GhostButton("해제", onClick = { actions.run { applyResponse(api.unequip(item.id)); null } }, enabled = !actions.busy)
                        } else {
                            GhostButton("장착", onClick = { actions.run { applyResponse(api.equip(item.id)); null } }, enabled = !actions.busy)
                            GhostButton(
                                "판매 ${item.sellPrice}G",
                                onClick = { actions.run { applyResponse(api.sell(item.id)); "판매 완료 +${item.sellPrice} 골드" } },
                                enabled = !actions.busy,
                            )
                        }
                    }
                }
            }
        }
    }
}
