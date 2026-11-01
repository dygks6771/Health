package com.example.health.ui.sprites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.health.game.ItemInfo
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.gradeColor
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.Ink
import com.example.health.ui.theme.PanelBrownLight
import com.example.health.ui.theme.Parchment

/** 영웅 도트 그림 */
@Composable
fun HeroAvatar(job: String, appearance: Int, size: Dp, modifier: Modifier = Modifier) {
    val sprite = remember(job, appearance) { HeroSprites.hero(job, appearance) }
    PixelSprite(sprite, modifier, size)
}

/** 장비 아이콘: 등급 색 테두리 칸 안에 도트 아이콘 */
@Composable
fun ItemIcon(item: ItemInfo?, size: Dp = 44.dp, emptySlot: String = "WEAPON") {
    val color = item?.let { gradeColor(it.gradeName) } ?: GoldDim
    Box(
        modifier = Modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(6.dp))
            .background(Brush.radialGradient(listOf(PanelBrownLight, Ink)), RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (item != null) {
            val sprite = remember(item.slot, item.name, item.gradeName) { ItemSprites.icon(item.slot, item.name, color) }
            PixelSprite(sprite, size = size - 8.dp)
        } else {
            val sprite = remember(emptySlot) { ItemSprites.icon(emptySlot, "", GoldDim) }
            PixelSprite(sprite, Modifier.alpha(0.35f), size - 8.dp)
        }
    }
}

@Composable
fun MonsterAvatar(name: String, size: Dp) {
    val sprite = remember(name) { MonsterSprites.monster(name) }
    PixelSprite(sprite, size = size)
}

/** NPC 초상화 + 말풍선 */
@Composable
fun NpcBanner(npc: NpcSprites.Npc, line: String) {
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelSprite(npc.sprite, size = 72.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${npc.name} · ${npc.title}", color = Gold, style = MaterialTheme.typography.titleMedium)
                Text("\"$line\"", color = Parchment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
