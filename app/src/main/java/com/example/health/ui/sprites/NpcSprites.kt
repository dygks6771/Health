package com.example.health.ui.sprites

import androidx.compose.ui.graphics.Color

/** 마을 NPC: 길드 접수원, 상인, 대장장이, 경매인 */
object NpcSprites {
    data class Npc(val name: String, val title: String, val sprite: Sprite)

    private val head = listOf(
        "......KKKK......",
        "....KKHHHHKK....",
        "...KHHHHHHHHK...",
        "...KHHHHHHHHK...",
        "...KHSSSSSSHK...",
        "...KSESSSSESK...",
        "...KSSSSSSSSK...",
        "....KSSssSSK....",
        ".....KKSSKK.....",
    )

    private fun npc(hatRows: List<String>?, body: List<String>, palette: Map<Char, Color>): Sprite {
        val rows = (hatRows?.let { it + head.drop(it.size) } ?: head) + body
        return Sprite(rows, mapOf('K' to Pal.outline, 'E' to Pal.outline) + palette)
    }

    /** 모험자 길드 접수원 리나 */
    val receptionist = Npc(
        "리나", "모험자 길드 접수원",
        npc(
            null,
            listOf(
                "....KOOOOOOK....",
                "...KOOWWWWOOK...",
                "..KSKOOWWOOKSK..",
                "..KSKOOOOOOKSK..",
                "...KOOOOOOOOK...",
                "..KOOOOOOOOOOK..",
                "..KOOOOOOOOOOK..",
                "...KKKK..KKKK...",
            ),
            mapOf(
                'H' to Color(0xFF7A3E9D), 'S' to Color(0xFFF5CBA7), 's' to Color(0xFFDCA98A),
                'O' to Color(0xFF2F5DA8), 'W' to Pal.white,
            ),
        ),
    )

    /** 떠돌이 상인 마르코 (모자) */
    val merchant = Npc(
        "마르코", "떠돌이 상인",
        npc(
            listOf(
                ".....KKKKKK.....",
                "....KWWWWWWK....",
                "..KKWWWWWWWWKK..",
                ".KGGGGGGGGGGGGK.",
            ),
            listOf(
                "....KOOOOOOK....",
                "...KOOGOOGOOK...",
                "..KSKOOOOOOKSK..",
                "..KSKOWWWWOKSK..",
                "...KKOWWWWOKK...",
                ".....KOOOOK.....",
                ".....KBBKBBK....",
                "....KBBBKBBBK...",
            ),
            mapOf(
                'H' to Color(0xFF4A2E17), 'S' to Color(0xFFE0AC69), 's' to Color(0xFFC08A4A),
                'O' to Color(0xFF2E7D6B), 'W' to Color(0xFF8B5A2B), 'G' to Pal.gold, 'B' to Pal.leather,
            ),
        ),
    )

    /** 대장장이 브론 (수염, 앞치마) */
    val blacksmith = Npc(
        "브론", "대장장이",
        npc(
            null,
            listOf(
                "...KHHHHHHHHK...",
                "..KSKHHHHHHKSK..",
                ".KSSKOOOOOOKSSK.",
                ".KSSKOWWWWOKSSK.",
                "..KKKOWWWWOKKMK.",
                ".....KWWWWK.KMMK",
                ".....KBBKBBK.KK.",
                "....KBBBKBBBK...",
            ),
            mapOf(
                'H' to Color(0xFFB5402E), 'S' to Color(0xFFD89C6A), 's' to Color(0xFFB57B4E),
                'O' to Color(0xFF5E3A1A), 'W' to Color(0xFF3A3A3A), 'M' to Pal.steel, 'B' to Pal.leather,
            ),
        ),
    )

    /** 경매인 세바스찬 (실크햇) */
    val auctioneer = Npc(
        "세바스찬", "경매장 관리인",
        npc(
            listOf(
                ".....KKKKKK.....",
                ".....KTTTTK.....",
                ".....KGGGGK.....",
                "...KKKKKKKKKK...",
            ),
            listOf(
                "....KOOWWOOK....",
                "...KOOOWWOOOK...",
                "..KSKOOOGOOKSK..",
                "..KSKOOOOOOKSK..",
                "...KKOOOOOOKK...",
                ".....KOOOOK.....",
                ".....KooKooK....",
                "....KKKKKKKKK...",
            ),
            mapOf(
                'H' to Color(0xFF9AA0A6), 'S' to Color(0xFFF1C27D), 's' to Color(0xFFD9A066),
                'O' to Color(0xFF26262E), 'o' to Color(0xFF15151A), 'T' to Color(0xFF15151A),
                'W' to Pal.white, 'G' to Color(0xFF9E2A2B),
            ),
        ),
    )
}
