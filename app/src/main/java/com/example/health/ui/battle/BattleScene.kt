package com.example.health.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.ui.fantasy.FantasyBar
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.sprites.PixelSprite
import com.example.health.ui.sprites.Sprite
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Emerald
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.Parchment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 전투에 나서는 쪽 (영웅·몬스터) */
data class Fighter(val name: String, val sprite: Sprite)

/** 공격 1번 */
data class BattleRound(
    /** 공격하는 영웅 (파티 순번) */
    val attacker: Int,
    val damage: Long,
    val crit: Boolean = false,
    /** 공격 뒤 적 체력 (0~1) */
    val enemyHpAfter: Float,
    /** 적이 이 공격으로 모습이 바뀌면 (레이드 진화) */
    val enemySprite: Sprite? = null,
    val caption: String? = null,
)

data class BattleSpec(
    val title: String,
    val heroes: List<Fighter>,
    val enemy: Fighter,
    val enemySize: Dp = 140.dp,
    val rounds: List<BattleRound>,
    val resultTitle: String,
    val resultLines: List<String>,
)

/**
 * 전투 장면: 서버가 이미 정한 결과를 애니메이션으로 보여준다.
 * 영웅이 돌진하며 공격 → 적이 흔들리고 데미지 숫자가 떠오름 → 체력 바 감소. 건너뛰기 가능.
 */
@Composable
fun BattleOverlay(spec: BattleSpec, onClose: () -> Unit) {
    val lunge = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val popup = remember { Animatable(1f) }
    var attacker by remember { mutableIntStateOf(-1) }
    var enemyHp by remember { mutableFloatStateOf(1f) }
    var enemySprite by remember { mutableStateOf(spec.enemy.sprite) }
    var damageText by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    val shownHp by animateFloatAsState(enemyHp, tween(300), label = "hp")

    fun skip() {
        spec.rounds.lastOrNull()?.let { last -> enemyHp = last.enemyHpAfter }
        spec.rounds.lastOrNull { it.enemySprite != null }?.enemySprite?.let { enemySprite = it }
        attacker = -1
        finished = true
    }

    BackHandler { if (finished) onClose() else skip() }

    LaunchedEffect(spec) {
        delay(400)
        for (round in spec.rounds) {
            if (finished) break
            attacker = round.attacker
            lunge.animateTo(1f, tween(150))
            if (finished) break
            damageText = (if (round.crit) "치명타! " else "") + "-%,d".format(round.damage)
            launch {
                popup.snapTo(0f)
                popup.animateTo(1f, tween(700))
            }
            launch {
                repeat(3) {
                    shake.animateTo(8f, tween(35))
                    shake.animateTo(-8f, tween(35))
                }
                shake.animateTo(0f, tween(35))
            }
            enemyHp = round.enemyHpAfter
            round.enemySprite?.let { enemySprite = it }
            round.caption?.let { caption = it }
            lunge.animateTo(0f, tween(180))
            delay(380)
        }
        if (!finished) skip()
        lunge.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0F1A), Color(0xFF1C140D), Color(0xFF3A2A18))))
            // 전투 중 뒤 화면 버튼이 눌리지 않도록 터치를 여기서 소비
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(spec.title, style = MaterialTheme.typography.headlineSmall, color = Gold)
            Text(spec.enemy.name, color = Parchment, fontWeight = FontWeight.Bold)
            FantasyBar(shownHp, Crimson)

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 영웅(파티)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    spec.heroes.forEachIndexed { i, hero ->
                        val heroSize = if (spec.heroes.size > 1) 64.dp else 120.dp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset {
                                IntOffset(if (i == attacker) (lunge.value * 70.dp.toPx()).roundToInt() else 0, 0)
                            },
                        ) {
                            PixelSprite(hero.sprite, size = heroSize)
                            if (spec.heroes.size > 1) Text(hero.name, color = Parchment, fontSize = 10.sp)
                        }
                    }
                }

                // 적
                Box(contentAlignment = Alignment.TopCenter) {
                    PixelSprite(
                        enemySprite,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .offset { IntOffset(shake.value.dp.roundToPx(), 0) },
                        size = spec.enemySize,
                        flipX = true,
                    )
                    if (damageText.isNotEmpty()) {
                        Text(
                            damageText,
                            color = if (damageText.startsWith("치명타")) GoldBright else Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .offset { IntOffset(0, -(popup.value * 40.dp.toPx()).roundToInt()) }
                                .graphicsLayer { alpha = 1f - popup.value },
                        )
                    }
                }
            }

            caption?.let { Text(it, color = GoldBright, fontWeight = FontWeight.Bold) }

            if (finished) {
                Panel(title = spec.resultTitle, highlight = true) {
                    spec.resultLines.forEach { Text(it, color = if (it.startsWith("획득")) Emerald else Parchment) }
                    GoldButton("확인", onClick = onClose, modifier = Modifier.fillMaxWidth())
                }
            } else {
                GhostButton("건너뛰기 ▶▶", onClick = { skip() }, modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.height(4.dp))
        }
    }
}

/** 적 체력을 [hits] 번에 나눠 깎는 전투 (토벌·던전). 결과는 항상 승리 */
fun simpleBattle(
    title: String,
    hero: Fighter,
    enemy: Fighter,
    enemyHp: Long,
    hits: Int,
    resultTitle: String,
    resultLines: List<String>,
    enemySize: Dp = 140.dp,
    seed: Int = 0,
): BattleSpec {
    val random = kotlin.random.Random(seed)
    // 무작위 비율로 나눈 뒤 합이 정확히 enemyHp 가 되도록 마지막 타격에서 맞춤
    val weights = List(hits) { 0.7 + random.nextDouble() * 0.6 }
    var remaining = enemyHp
    val rounds = weights.mapIndexed { i, w ->
        val damage = if (i == hits - 1) remaining else (enemyHp * w / weights.sum()).toLong().coerceAtMost(remaining)
        remaining -= damage
        BattleRound(
            attacker = 0,
            damage = damage,
            crit = w > 1.15,
            enemyHpAfter = remaining.toFloat() / enemyHp,
            caption = if (i == hits - 1) "${enemy.name} 을(를) 쓰러뜨렸다!" else null,
        )
    }
    return BattleSpec(title, listOf(hero), enemy, enemySize, rounds, resultTitle, resultLines)
}
