package com.example.health.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.sprites.ChestSprites
import com.example.health.ui.sprites.PixelSprite
import com.example.health.ui.theme.Emerald
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.GradeLegendary
import com.example.health.ui.theme.Parchment
import kotlinx.coroutines.delay

/** 보상 상자: [tier] = WOOD / SILVER / GOLD, [rewards] 는 한 줄씩 튀어나오는 보상 */
data class ChestInfo(val tier: String, val name: String, val rewards: List<String>)

/**
 * 보물 상자 열기 연출: 상자가 흔들리다 → 열리며 빛이 퍼지고 → 보상이 하나씩 튀어나온다.
 */
@Composable
fun ChestReveal(chest: ChestInfo, onDone: () -> Unit) {
    val wiggle = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    var opened by remember { mutableStateOf(false) }
    var shown by remember { mutableIntStateOf(0) }

    LaunchedEffect(chest) {
        repeat(4) {
            wiggle.animateTo(12f, tween(70))
            wiggle.animateTo(-12f, tween(70))
        }
        wiggle.animateTo(0f, tween(60))
        opened = true
        glow.animateTo(1f, tween(450))
        for (i in chest.rewards.indices) {
            delay(380)
            shown = i + 1
        }
    }

    Panel(title = "🎁 ${chest.name}", highlight = true) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // 열릴 때 뒤에서 퍼지는 빛
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = 0.4f + glow.value * 0.8f
                        scaleY = 0.4f + glow.value * 0.8f
                        alpha = glow.value * 0.8f
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(if (chest.tier == "GOLD") GradeLegendary else Color(0xFFFFF3B0), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
            PixelSprite(
                ChestSprites.chest(chest.tier, opened),
                modifier = Modifier.rotate(wiggle.value),
                size = 120.dp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            chest.rewards.take(shown).forEach { reward ->
                RewardLine(reward)
            }
        }
        if (shown >= chest.rewards.size && opened) {
            GoldButton("획득!", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RewardLine(text: String) {
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(text) { pop.animateTo(1f, tween(220)) }
    Text(
        "✦ $text",
        color = when {
            text.startsWith("획득") -> Emerald
            text.contains("골드") -> GoldBright
            else -> Parchment
        },
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
            alpha = pop.value
        },
    )
}
