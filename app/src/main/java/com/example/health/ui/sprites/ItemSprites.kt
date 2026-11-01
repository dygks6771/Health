package com.example.health.ui.sprites

import androidx.compose.ui.graphics.Color

/** 장비 아이콘 (12×12). 'G' 부분(보석·장식)이 장비 등급 색으로 칠해진다 */
object ItemSprites {
    private val sword = listOf(
        "..........KK",
        ".........KWK",
        "........KWK.",
        ".......KWK..",
        "......KWK...",
        ".....KWK....",
        "..K.KWK.....",
        "..KKWK......",
        "...KGK......",
        "..KGKKK.....",
        ".KGK........",
        ".KK.........",
    )
    private val greatsword = listOf(
        ".........KKK",
        "........KWWK",
        ".......KWWK.",
        "......KWWK..",
        ".....KWWK...",
        "....KWWK....",
        ".K.KWWK.....",
        ".KKWWK......",
        "..KGGK......",
        ".KGKKK......",
        "KGK.........",
        "KK..........",
    )
    private val bow = listOf(
        "....KK......",
        "...KwK......",
        "..KwK.Ks....",
        "..KwK..Ks...",
        ".KwK....s...",
        ".KGK....s...",
        ".KGK....s...",
        ".KwK....s...",
        "..KwK..Ks...",
        "..KwK.Ks....",
        "...KwK......",
        "....KK......",
    )
    private val staff = listOf(
        "........KKK.",
        ".......KGGGK",
        ".......KGGGK",
        "........KKK.",
        ".......KwK..",
        "......KwK...",
        ".....KwK....",
        "....KwK.....",
        "...KwK......",
        "..KwK.......",
        ".KwK........",
        ".KK.........",
    )
    private val mace = listOf(
        ".......KKK..",
        "......KGGGK.",
        ".....KGKGKGK",
        "......KGGGK.",
        ".......KwK..",
        "......KwK...",
        ".....KwK....",
        "....KwK.....",
        "...KwK......",
        "..KwK.......",
        ".KwK........",
        ".KK.........",
    )
    private val armor = listOf(
        "..KK....KK..",
        ".KMMK..KMMK.",
        "KMMMMKKMMMMK",
        "KMMGMMMMGMMK",
        ".KMMMMMMMMK.",
        "..KMMGGMMK..",
        "..KMMMMMMK..",
        "..KMMMMMMK..",
        "..KmmmmmmK..",
        "..KMMMMMMK..",
        "...KKKKKK...",
        "............",
    )
    private val boots = listOf(
        "............",
        "...KKKK.....",
        "...KBBK.....",
        "...KBBK.....",
        "...KGGK.....",
        "...KBBK.....",
        "...KBBBK....",
        "...KBBBBKK..",
        "..KBBBBBBBK.",
        "..KBBBBBBBK.",
        "..KKKKKKKKK.",
        "............",
    )
    private val necklace = listOf(
        "..KK....KK..",
        ".KY.K..K.YK.",
        ".KY..KK..YK.",
        "..KY....YK..",
        "..KY....YK..",
        "...KY..YK...",
        "....KYYK....",
        "....KGGK....",
        "...KGGGGK...",
        "...KGGGGK...",
        "....KGGK....",
        ".....KK.....",
    )

    /** [slot] = WEAPON / ARMOR / BOOTS / ACCESSORY, 무기는 이름으로 종류 구분 */
    fun icon(slot: String, name: String, gradeColor: Color): Sprite {
        val rows = when (slot) {
            "ARMOR" -> armor
            "BOOTS" -> boots
            "ACCESSORY" -> necklace
            else -> when {
                "대검" in name -> greatsword
                "장궁" in name -> bow
                "지팡이" in name -> staff
                "성구" in name -> mace
                else -> sword
            }
        }
        return Sprite(
            rows,
            mapOf(
                'K' to Pal.outline,
                'W' to Pal.bladeLight,
                'w' to Pal.wood,
                's' to Pal.white,
                'M' to Pal.steel,
                'm' to Pal.steelDark,
                'B' to Pal.leather,
                'Y' to Pal.gold,
                'G' to gradeColor,
            ),
        )
    }
}
