package com.example.health.auction

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.health.game.HeroInfo
import com.example.health.game.ItemInfo
import com.example.health.game.Listing
import com.example.health.net.ApiClient
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ResourceChip
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.sprites.ItemIcon
import com.example.health.ui.sprites.NpcBanner
import com.example.health.ui.sprites.NpcSprites
import com.example.health.ui.theme.Emerald
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

private val slotFilters = listOf(null to "전체", "WEAPON" to "무기", "ARMOR" to "갑옷", "BOOTS" to "장화", "ACCESSORY" to "목걸이")

/** 경매장: 다른 모험가의 장비 구매 / 내 장비 판매 등록 (수수료 5%) */
@Composable
fun AuctionScreen(api: ApiClient, hero: HeroInfo, onHeroJson: (JSONObject) -> Unit, onBack: () -> Unit) {
    var sellTab by remember { mutableStateOf(false) }
    var slot by remember { mutableStateOf<String?>(null) }
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var myListings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var sellable by remember { mutableStateOf<List<ItemInfo>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<Long?>(null) }
    var priceText by remember { mutableStateOf("") }
    val actions = rememberActions()

    suspend fun loadBoard() {
        listings = Listing.listFrom(api.auction(slot).getJSONArray("listings"))
    }

    fun applyMine(json: JSONObject) {
        myListings = Listing.listFrom(json.getJSONArray("listings"))
        sellable = ItemInfo.listFrom(json.getJSONArray("sellable"))
        onHeroJson(json.getJSONObject("hero"))
    }

    LaunchedEffect(sellTab, slot) {
        actions.run {
            if (sellTab) applyMine(api.auctionMine()) else loadBoard()
            null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("경매장", onBack)
        NpcBanner(NpcSprites.auctioneer, "좋은 물건은 주인을 기다리지 않습니다. 거래 대금의 5%는 수수료로 받겠습니다.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sellTab) GhostButton("구매", onClick = { sellTab = false }) else GoldButton("구매", onClick = {})
            if (sellTab) GoldButton("판매", onClick = {}) else GhostButton("판매", onClick = { sellTab = true })
            ResourceChip("💰", "%,d".format(hero.gold))
        }
        ActionFeedback(actions)

        if (!sellTab) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                slotFilters.forEach { (code, label) ->
                    Text(
                        label,
                        color = if (slot == code) Gold else ParchmentDim,
                        fontWeight = if (slot == code) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .border(1.dp, if (slot == code) Gold else GoldDim, RoundedCornerShape(12.dp))
                            .clickable { slot = code }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (listings.isEmpty() && !actions.busy) Text("판매 중인 물건이 없습니다", color = ParchmentDim)
            listings.forEach { l ->
                Panel {
                    ItemRow(l.item)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("판매자 ${l.sellerName}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("💰 %,d".format(l.price), color = Gold, fontWeight = FontWeight.Bold)
                    }
                    if (l.mine) {
                        Text("내가 올린 물건입니다", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    } else {
                        GoldButton(
                            "구매하기",
                            onClick = {
                                actions.run {
                                    val json = api.buyListing(l.id)
                                    onHeroJson(json.getJSONObject("hero"))
                                    loadBoard()
                                    "구매 완료! 가방을 확인하세요"
                                }
                            },
                            enabled = !actions.busy && hero.gold >= l.price,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        } else {
            Panel(title = "판매 등록") {
                if (sellable.isEmpty()) Text("등록할 수 있는 장비가 없습니다 (장착 중인 장비는 해제 후 등록)", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                sellable.forEach { item ->
                    val selected = item.id == selectedItem
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selected) Gold else GoldDim, RoundedCornerShape(8.dp))
                            .clickable { selectedItem = item.id }
                            .padding(6.dp),
                    ) { ItemRow(item) }
                }
                if (selectedItem != null) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter(Char::isDigit).take(8) },
                        label = { Text("판매 가격 (골드)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val price = priceText.toLongOrNull() ?: 0L
                    Text("팔리면 수수료 5%%를 뺀 %,d 골드를 받습니다".format(price - price * 5 / 100), color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    GoldButton(
                        "경매장에 등록",
                        onClick = {
                            val itemId = selectedItem ?: return@GoldButton
                            actions.run {
                                applyMine(api.listItem(itemId, price))
                                selectedItem = null
                                priceText = ""
                                "등록했습니다"
                            }
                        },
                        enabled = !actions.busy && price > 0,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Panel(title = "내 판매 내역") {
                if (myListings.isEmpty()) Text("아직 없습니다", color = ParchmentDim)
                myListings.forEach { l ->
                    ItemRow(l.item)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (l.status) {
                                "ACTIVE" -> "판매 중 · %,d".format(l.price)
                                "SOLD" -> "판매 완료 → ${l.buyerName ?: "?"} · +%,d".format(l.sellerReceives)
                                else -> "등록 취소"
                            },
                            color = when (l.status) {
                                "ACTIVE" -> Gold
                                "SOLD" -> Emerald
                                else -> ParchmentDim
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (l.status == "ACTIVE") {
                            GhostButton("취소", onClick = { actions.run { applyMine(api.cancelListing(l.id)); "등록을 취소했습니다" } }, enabled = !actions.busy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: ItemInfo) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ItemIcon(item, 40.dp)
        Column {
            Text("[${item.gradeName}] ${item.displayName}", color = gradeColor(item.gradeName), fontWeight = FontWeight.SemiBold)
            Text("${item.slotName} · 티어 ${item.tier} · ${item.statText}", color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
    }
}

