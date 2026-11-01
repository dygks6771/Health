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

/** 공격 1번 (영웅의 공격 또는 적의 반격) */
data class BattleRound(
    /** 공격하는 영웅 (파티 순번). 적의 반격이면 무시 */
    val attacker: Int,
    val damage: Long,
    val crit: Boolean = false,
    /** 이 공격 뒤 적 체력 (0~1) */
    val enemyHpAfter: Float,
    /** 적이 이 공격으로 모습이 바뀌면 (레이드 진화) */
    val enemySprite: Sprite? = null,
    val caption: String? = null,
    /** true 면 적이 영웅(파티)을 공격 */
    val enemyAttack: Boolean = false,
    /** 이 공격 뒤 영웅(파티) 체력 (0~1) */
    val heroHpAfter: Float? = null,
)

data class BattleSpec(
    val title: String,
    val heroes: List<Fighter>,
    val enemy: Fighter,
    val enemySize: Dp = 140.dp,
    val rounds: List<BattleRound>,
    val resultTitle: String,
    val resultLines: List<String>,
    val victory: Boolean = true,
    /** 승리 보상 상자 (있으면 결과 뒤에 상자 열기) */
    val chest: ChestInfo? = null,
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
    val enemyLunge = remember { Animatable(0f) }
    val heroShake = remember { Animatable(0f) }
    val heroPopup = remember { Animatable(1f) }
    var heroHp by remember { mutableFloatStateOf(1f) }
    var heroDamageText by remember { mutableStateOf("") }
    val shownHeroHp by animateFloatAsState(heroHp, tween(300), label = "heroHp")
    var attacker by remember { mutableIntStateOf(-1) }
    var enemyHp by remember { mutableFloatStateOf(1f) }
    var enemySprite by remember { mutableStateOf(spec.enemy.sprite) }
    var damageText by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    var chestOpen by remember { mutableStateOf(false) }
    val shownHp by animateFloatAsState(enemyHp, tween(300), label = "hp")

    fun skip() {
        spec.rounds.lastOrNull()?.let { last -> enemyHp = last.enemyHpAfter }
        spec.rounds.lastOrNull { it.heroHpAfter != null }?.heroHpAfter?.let { heroHp = it }
        spec.rounds.lastOrNull { it.enemySprite != null }?.enemySprite?.let { enemySprite = it }
        attacker = -1
        finished = true
    }

    BackHandler { if (finished) onClose() else skip() }

    LaunchedEffect(spec) {
        delay(400)
        for (round in spec.rounds) {
            if (finished) break
            if (round.enemyAttack) {
                // 적의 반격: 적이 왼쪽으로 돌진 → 영웅이 흔들리고 빨간 데미지 숫자
                enemyLunge.animateTo(1f, tween(170))
                if (finished) break
                heroDamageText = "-%,d".format(round.damage)
                launch {
                    heroPopup.snapTo(0f)
                    heroPopup.animateTo(1f, tween(700))
                }
                launch {
                    repeat(3) {
                        heroShake.animateTo(7f, tween(35))
                        heroShake.animateTo(-7f, tween(35))
                    }
                    heroShake.animateTo(0f, tween(35))
                }
                round.heroHpAfter?.let { heroHp = it }
                round.caption?.let { caption = it }
                enemyLunge.animateTo(0f, tween(200))
                delay(380)
                continue
            }
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
        enemyLunge.snapTo(0f)
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
                Box(contentAlignment = Alignment.TopCenter) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.offset { IntOffset(heroShake.value.dp.roundToPx(), 0) },
                ) {
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
                if (heroDamageText.isNotEmpty()) {
                    Text(
                        heroDamageText,
                        color = Crimson,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .offset { IntOffset(0, -(heroPopup.value * 40.dp.toPx()).roundToInt()) }
                            .graphicsLayer { alpha = 1f - heroPopup.value },
                    )
                }
                }

                // 적
                Box(contentAlignment = Alignment.TopCenter) {
                    PixelSprite(
                        enemySprite,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .offset {
                                IntOffset(shake.value.dp.roundToPx() - (enemyLunge.value * 70.dp.toPx()).roundToInt(), 0)
                            },
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

            Text(if (spec.heroes.size > 1) "파티 체력" else "내 체력", color = Parchment, fontWeight = FontWeight.Bold)
            FantasyBar(shownHeroHp, Emerald)
            caption?.let { Text(it, color = GoldBright, fontWeight = FontWeight.Bold) }

            if (finished) {
                val chest = spec.chest
                if (chestOpen && chest != null) {
                    ChestReveal(chest, onDone = onClose)
                } else {
                    Panel(title = spec.resultTitle, highlight = spec.victory) {
                        if (!spec.victory) Text("💀 패배...", color = Crimson, style = MaterialTheme.typography.headlineSmall)
                        spec.resultLines.forEach { Text(it, color = if (it.startsWith("획득")) Emerald else Parchment) }
                        if (spec.victory && chest != null) {
                            GoldButton("🎁 ${chest.name} 열기", onClick = { chestOpen = true }, modifier = Modifier.fillMaxWidth())
                        } else {
                            GoldButton("확인", onClick = onClose, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                GhostButton("건너뛰기 ▶▶", onClick = { skip() }, modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * 토벌·던전 전투 장면 만들기. 영웅이 공격하면 적이 반격한다.
 * 승리: 적 체력을 [hits] 번에 모두 깎고, 영웅은 [heroLoss] (0~0.9) 만큼만 잃는다.
 * 패배: 적 체력을 절반쯤 깎다가 영웅이 쓰러진다.
 */
fun simpleBattle(
    title: String,
    hero: Fighter,
    enemy: Fighter,
    enemyHp: Long,
    hits: Int,
    resultTitle: String,
    resultLines: List<String>,
    heroMaxHp: Long,
    heroLoss: Float,
    victory: Boolean = true,
    chest: ChestInfo? = null,
    enemySize: Dp = 140.dp,
    seed: Int = 0,
): BattleSpec {
    val random = kotlin.random.Random(seed)
    val heroHits = hits.coerceAtLeast(1)
    val enemyHits = if (victory) (heroHits - 1).coerceAtLeast(1) else heroHits
    val dealt = if (victory) enemyHp else (enemyHp * (0.4 + random.nextDouble() * 0.3)).toLong()
    val taken = if (victory) (heroMaxHp * heroLoss.coerceIn(0.05f, 0.9f)).toLong() else heroMaxHp

    /** [total] 을 [n] 번으로 무작위 분배 (합이 정확히 total) */
    fun split(total: Long, n: Int): List<Long> {
        val weights = List(n) { 0.7 + random.nextDouble() * 0.6 }
        var left = total
        return weights.mapIndexed { i, w ->
            val part = if (i == n - 1) left else (total * w / weights.sum()).toLong().coerceIn(0L, left)
            left -= part
            part
        }
    }

    val heroDamage = split(dealt, heroHits)
    val enemyDamage = split(taken, enemyHits)
    var enemyLeft = enemyHp
    var heroLeft = heroMaxHp
    val rounds = mutableListOf<BattleRound>()
    for (i in 0 until heroHits) {
        enemyLeft -= heroDamage[i]
        val kills = victory && i == heroHits - 1
        rounds += BattleRound(
            attacker = 0,
            damage = heroDamage[i],
            crit = heroDamage[i] > dealt / heroHits * 1.15,
            enemyHpAfter = enemyLeft.toFloat() / enemyHp,
            caption = if (kills) "${enemy.name} 을(를) 쓰러뜨렸다!" else null,
        )
        if (i < enemyHits && !kills) {
            val lastBlow = !victory && i == enemyHits - 1
            // 승리하는 전투에서는 영웅이 쓰러지지 않도록 체력 1 이상 남김
            val hit = if (lastBlow) heroLeft else enemyDamage[i].coerceIn(0L, (heroLeft - 1).coerceAtLeast(0L))
            heroLeft -= hit
            rounds += BattleRound(
                attacker = 0,
                damage = hit,
                enemyHpAfter = enemyLeft.toFloat() / enemyHp,
                enemyAttack = true,
                heroHpAfter = heroLeft.toFloat() / heroMaxHp,
                caption = if (lastBlow) "${hero.name} 이(가) 쓰러졌다..." else "${enemy.name}의 반격!",
            )
        }
    }
    return BattleSpec(title, listOf(hero), enemy, enemySize, rounds, resultTitle, resultLines, victory, chest)
}
