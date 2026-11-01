package com.example.health.ui.sprites

import androidx.compose.ui.graphics.Color

/** 보물 상자 (16×14). 등급(나무·은·황금)마다 색이 다르고, 열리면 안에서 빛이 난다 */
object ChestSprites {
    private val closed = listOf(
        "................",
        "................",
        "..KKKKKKKKKKKK..",
        ".KWWWWWWWWWWWWK.",
        ".KWMMMMMMMMMMWK.",
        ".KWWWWWWWWWWWWK.",
        ".KKKKKKGGKKKKKK.",
        ".KWWWWWGGWWWWWK.",
        ".KWMMMMGGMMMMWK.",
        ".KWWWWWWWWWWWWK.",
        ".KWMMMMMMMMMMWK.",
        ".KWWWWWWWWWWWWK.",
        ".KKKKKKKKKKKKKK.",
        "................",
    )
    private val open = listOf(
        "..KKKKKKKKKKKK..",
        ".KWWWWWWWWWWWWK.",
        ".KWMMMMMMMMMMWK.",
        ".KWWWWWWWWWWWWK.",
        ".KKKKKKKKKKKKKK.",
        ".KYYYYYYYYYYYYK.",
        ".KYYYYYYYYYYYYK.",
        ".KWWWWWGGWWWWWK.",
        ".KWMMMMGGMMMMWK.",
        ".KWWWWWWWWWWWWK.",
        ".KWMMMMMMMMMMWK.",
        ".KWWWWWWWWWWWWK.",
        ".KKKKKKKKKKKKKK.",
        "................",
    )

    /** 상자 색 (몸통, 띠) */
    private fun colors(tier: String): Pair<Color, Color> = when (tier) {
        "GOLD" -> Color(0xFFE2B13C) to Color(0xFF9C6B12)
        "SILVER" -> Color(0xFFC9D1D9) to Color(0xFF6E7A86)
        else -> Color(0xFF9C6232) to Color(0xFF5E3A1A)
    }

    fun chest(tier: String, opened: Boolean): Sprite {
        val (body, band) = colors(tier)
        return Sprite(
            if (opened) open else closed,
            mapOf(
                'K' to Pal.outline,
                'W' to body,
                'M' to band,
                'G' to Pal.gold,
                'Y' to Color(0xFFFFF3B0),
            ),
        )
    }
}
