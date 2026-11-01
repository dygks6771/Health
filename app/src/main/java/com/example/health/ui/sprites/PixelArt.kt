package com.example.health.ui.sprites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 직접 그린 도트(픽셀) 그림. [rows] 의 글자 하나가 픽셀 하나이고, [palette] 에 없는 글자('.')는 투명.
 * 그림 파일 없이 코드로 그리므로 저작권 걱정 없이 색만 바꿔 여러 캐릭터를 만들 수 있다.
 */
class Sprite(val rows: List<String>, val palette: Map<Char, Color>) {
    val width: Int = rows.maxOf { it.length }
    val height: Int = rows.size

    fun recolor(vararg colors: Pair<Char, Color>): Sprite = Sprite(rows, palette + colors)
}

@Composable
fun PixelSprite(sprite: Sprite, modifier: Modifier = Modifier, size: Dp = 64.dp, flipX: Boolean = false) {
    Canvas(modifier = modifier.size(size)) {
        val cell = minOf(this.size.width / sprite.width, this.size.height / sprite.height)
        val left = (this.size.width - cell * sprite.width) / 2
        val top = (this.size.height - cell * sprite.height) / 2
        sprite.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = sprite.palette[ch] ?: return@forEachIndexed
                val px = if (flipX) sprite.width - 1 - x else x
                // 0.5px 겹쳐 그려 칸 사이 틈이 보이지 않게
                drawRect(color, Offset(left + px * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

/** 공통 색 */
internal object Pal {
    val outline = Color(0xFF1A120B)
    val steel = Color(0xFFB8C2CC)
    val steelDark = Color(0xFF6E7A86)
    val bladeLight = Color(0xFFE4EAF0)
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF5E3A1A)
    val gold = Color(0xFFE2B13C)
    val leather = Color(0xFF5A3A1E)
    val white = Color(0xFFF2EEE4)
}
