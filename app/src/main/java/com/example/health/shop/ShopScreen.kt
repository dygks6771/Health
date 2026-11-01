package com.example.health.shop

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
import com.example.health.game.HeroInfo
import com.example.health.game.ItemInfo
import com.example.health.game.ShopInfo
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
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

/** 상점: 골드로 무기 / 방어구 무작위 뽑기 */
@Composable
fun ShopScreen(api: ApiClient, hero: HeroInfo, onHeroJson: (JSONObject) -> Unit, onBack: () -> Unit) {
    var shop by remember { mutableStateOf<ShopInfo?>(null) }
    var drawn by remember { mutableStateOf<ItemInfo?>(null) }
    val actions = rememberActions()

    LaunchedEffect(Unit) { actions.run { shop = ShopInfo.from(api.shop()); null } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("상점", onBack)
        NpcBanner(NpcSprites.merchant, "어서 오시게! 왕국 공방에서 갓 들여온 장비라네. 뭐가 나올지는 나도 모르지, 허허.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResourceChip("💰", "%,d".format(hero.gold))
        }

        drawn?.let { item ->
            Panel(title = "✨ 획득!", highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ItemIcon(item, 64.dp)
                    Column {
                        Text("[${item.gradeName}] ${item.displayName}", color = gradeColor(item.gradeName), style = MaterialTheme.typography.titleMedium)
                        Text("${item.slotName} · ${item.statText}", color = Parchment)
                        Text(if (item.equipped) "빈 부위라 바로 장착했습니다" else "가방에 넣었습니다", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        ActionFeedback(actions)

        val s = shop ?: return@Column
        s.offers.forEach { offer ->
            Panel(title = "${if (offer.category == "WEAPON") "⚔️" else "🛡️"} ${offer.name} 뽑기") {
                Text("${offer.slots.joinToString(" · ")} 중 하나 · ${s.regionName} 수준 (티어 ${s.tier})", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                GoldButton(
                    "%,d 골드로 뽑기".format(offer.price),
                    onClick = {
                        actions.run {
                            val json = api.draw(offer.category)
                            onHeroJson(json.getJSONObject("hero"))
                            drawn = ItemInfo.from(json.getJSONObject("item"))
                            null
                        }
                    },
                    enabled = !actions.busy && hero.gold >= offer.price,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Panel(title = "등급 확률") {
            s.chances.forEach { (grade, percent) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(grade, color = gradeColor(grade), fontWeight = FontWeight.Bold)
                    Text("%.1f%%".format(percent), color = Gold)
                }
            }
            Text("가격과 장비 수치는 내 레벨의 지역을 따라 올라갑니다", color = GoldBright, style = MaterialTheme.typography.bodySmall)
        }
    }
}
