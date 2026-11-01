package com.example.health.ui.sprites

import androidx.compose.ui.graphics.Color

/**
 * 영웅 도트 (16×19). 머리·다리는 공통, 몸통·무기는 직업별.
 * 외형(0~5)은 머리색·피부색, 직업은 옷·무기 색을 바꾼다.
 */
object HeroSprites {
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

    private val legs = listOf(
        ".....KooKooK....",
        ".....KooKooK....",
        ".....KBBKBBK....",
        "....KBBBKBBBK...",
    )

    /** 몸통 6줄 (무기·방패 포함) */
    private val bodies = mapOf(
        "KNIGHT" to listOf(
            ".KKKKMMMMMMK..W.",
            "KwwwKMMOOMMMK.W.",
            "KwGwKMOOOOMKSKW.",
            "KwwwKMOOOOMKSKW.",
            ".KKKKKMMMMKKKKG.",
            ".....KGGGGK.....",
        ),
        "WARRIOR" to listOf(
            "....KOOOOOOK.WW.",
            "...KOOMMMMOOKWW.",
            "..KSKOOOOOOKSWW.",
            "..KSKOMMMMOKSWK.",
            "...KKOOOOOOKKG..",
            ".....KGGGGK.....",
        ),
        "ARCHER" to listOf(
            "....KOOOOOOK.W..",
            "...KOOOOOOOOKwW.",
            "..KSKOOOOOOKS.Ww",
            "..KSKOOOOOOKS.Ww",
            "...KKOOGGOOKKwW.",
            ".....KOOOOK..W..",
        ),
        "MAGE" to listOf(
            "....KOOOOOOK.GG.",
            "...KOOOOOOOOKGG.",
            "..KSKOOGGOOKSKW.",
            "..KSKOOOOOOKSKW.",
            "...KOOOOOOOOKW..",
            "..KOOOOOOOOOOKW.",
        ),
        "PRIEST" to listOf(
            "....KOOOOOOK..G.",
            "...KOOGGGGOOKGGG",
            "..KSKOOOGOOKSKG.",
            "..KSKOOOGOOKSKW.",
            "...KOOOOOOOOKW..",
            "..KOOOOOOOOOOKW.",
        ),
    )

    /** 외형별 (머리, 머리 그림자, 피부, 피부 그림자) */
    private val looks = listOf(
        listOf(0xFF6B4423, 0xFF4A2E17, 0xFFF1C27D, 0xFFD9A066), // 견습 기사
        listOf(0xFFB5402E, 0xFF7E2A1E, 0xFFF5CBA7, 0xFFDCA98A), // 여검사
        listOf(0xFF2B2B2B, 0xFF151515, 0xFFC68642, 0xFFA06A32), // 떠돌이 용병
        listOf(0xFFE8C86A, 0xFFB89A44, 0xFFF7D9B9, 0xFFE0B996), // 엘프 레인저
        listOf(0xFFC9CCD6, 0xFF8E93A0, 0xFFEBC9A9, 0xFFCFA788), // 은빛 마녀
        listOf(0xFFF2D16B, 0xFFC9A944, 0xFFF8DCC4, 0xFFE2BFA3), // 성녀
    ).map { it.map(::Color) }

    /** 직업별 (옷, 옷 그림자, 무기, 무기 보조) */
    private val outfits = mapOf(
        "KNIGHT" to listOf(Color(0xFF2F5DA8), Color(0xFF1E3D70), Pal.bladeLight, Pal.wood),
        "WARRIOR" to listOf(Color(0xFF8E2B2B), Color(0xFF5E1A1A), Pal.bladeLight, Pal.steelDark),
        "ARCHER" to listOf(Color(0xFF3E7D3A), Color(0xFF28532A), Pal.wood, Color(0xFFE8E0C8)),
        "MAGE" to listOf(Color(0xFF5B3A9E), Color(0xFF3B2468), Pal.wood, Pal.woodDark),
        "PRIEST" to listOf(Color(0xFFE8E4D8), Color(0xFFB9B2A0), Pal.wood, Pal.woodDark),
    )

    fun hero(job: String, appearance: Int): Sprite {
        val look = looks[appearance.coerceIn(0, looks.size - 1)]
        val outfit = outfits[job] ?: outfits.getValue("WARRIOR")
        val gem = if (job == "MAGE") Color(0xFF5BC0EB) else Pal.gold
        return Sprite(
            rows = head + (bodies[job] ?: bodies.getValue("WARRIOR")) + legs,
            palette = mapOf(
                'K' to Pal.outline,
                'H' to look[0], 'h' to look[1], 'S' to look[2], 's' to look[3],
                'E' to Pal.outline,
                'O' to outfit[0], 'o' to outfit[1],
                'W' to outfit[2], 'w' to outfit[3],
                'M' to Pal.steel, 'G' to gem, 'B' to Pal.leather,
            ),
        )
    }
}
