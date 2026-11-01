package com.example.health.ui.sprites

import androidx.compose.ui.graphics.Color

/** 토벌 몬스터 (16×16). 이름에 따라 모양과 색을 고른다 */
object MonsterSprites {
    private val slime = listOf(
        "................",
        "................",
        "................",
        "................",
        "......KKKK......",
        "....KKMMMMKK....",
        "...KMMWMMMMMK...",
        "..KMMWMMMMMMMK..",
        "..KMMMMMMMMMMK..",
        ".KMMMEMMMMEMMMK.",
        ".KMMMEMMMMEMMMK.",
        ".KMMMMMMMMMMMMK.",
        ".KmMMMMMMMMMMmK.",
        "..KmmmmmmmmmmK..",
        "...KKKKKKKKKK...",
        "................",
    )
    private val beast = listOf(
        "................",
        "................",
        "..KK............",
        ".KMMK.......KK..",
        ".KMMMK.....KMK..",
        "KMEMMMKKKKKMMK..",
        "KMMMMMMMMMMMMMK.",
        ".KWKMMMMMMMMMMK.",
        "..KKMMMMMMMMMMMK",
        "....KMMMMMMMMMMK",
        "....KMmMMMMmMMK.",
        "....KMmK..KMmMK.",
        "....KMK...KMK...",
        "....KKK...KKK...",
        "................",
        "................",
    )
    private val goblin = listOf(
        "................",
        "......KKKK......",
        ".K..KKMMMMKK..K.",
        ".KMKMMMMMMMMKMK.",
        "..KMMEMMMMEMMK..",
        "...KMMMMMMMMK...",
        "....KMWWWWMK....",
        ".....KKMMKK.....",
        "....KOOOOOOK....",
        "...KMOOOOOOMK.S.",
        "..KMKOOOOOOKMKS.",
        "..KK.KOOOOK.KKS.",
        ".....KOO.OOK....",
        ".....KMM.MMK....",
        "....KKKK.KKKK...",
        "................",
    )
    private val skeleton = listOf(
        "......KKKK......",
        ".....KWWWWK.....",
        "....KWWWWWWK....",
        "....KWKWWKWK....",
        "....KWWWWWWK....",
        ".....KWKKWK.....",
        "......KWWK......",
        "....KKWKKWKK....",
        "...KWKWWWWKWK...",
        "...KWKKWWKKWK.S.",
        "...KK.KWWK.KK.S.",
        "......KWWK....S.",
        ".....KWKKWK..KMK",
        ".....KWK.KWK....",
        "....KWWK.KWWK...",
        "....KKKK.KKKK...",
    )
    private val golem = listOf(
        "................",
        ".....KKKKKK.....",
        "....KMMMMMMK....",
        "....KMEMMEMK....",
        "..KKKMMMMMMKKK..",
        ".KMMMKMMMMKMMMK.",
        "KMMMMKMWWMKMMMMK",
        "KMMMKMMWWMMKMMMK",
        "KMMMKMMMMMMKMMMK",
        ".KKKKMMMMMMKKKK.",
        "....KMMKKMMK....",
        "....KMMK.KMMK...",
        "...KMMMK.KMMMK..",
        "...KKKKK.KKKKK..",
        "................",
        "................",
    )
    private val ghost = listOf(
        "................",
        ".....KKKKKK.....",
        "....KMMMMMMK....",
        "...KMMMMMMMMK...",
        "...KMEEMMEEMK...",
        "...KMEEMMEEMK...",
        "...KMMMMMMMMK...",
        "..KMMMMKKMMMMK..",
        "..KMMMMMMMMMMK..",
        "..KMMMMMMMMMMK..",
        "..KMMMMMMMMMMK..",
        "..KMMKMMMMKMMK..",
        "..KMK.KMMK.KMK..",
        "..KK...KK...KK..",
        "................",
        "................",
    )
    private val demon = listOf(
        "..K..........K..",
        "..KK..KKKK..KK..",
        "...KKKMMMMKKK...",
        "....KMMMMMMK....",
        "....KMEMMEMK....",
        "....KMMWWMMK....",
        ".....KMMMMK.....",
        "..KKKKOOOOKKKK..",
        ".KMMKOOOOOOKMMK.",
        ".KMKKOOGGOOKKMK.",
        ".KK.KOOOOOOK.KK.",
        "....KOOOOOOK....",
        "....KOOKKOOK....",
        "....KMMK.KMMK...",
        "...KKKKK.KKKKK..",
        "................",
    )

    private fun shapeFor(name: String): List<String> = when {
        listOf("슬라임", "독버섯", "독두꺼비").any { it in name } -> slime
        listOf("들쥐", "늑대", "박쥐", "전갈", "웜", "도마뱀", "사냥개", "그리폰").any { it in name } -> beast
        listOf("해골", "미라").any { it in name } -> skeleton
        "골렘" in name -> golem
        listOf("정령", "망령", "유령").any { it in name } -> ghost
        listOf("임프", "데스나이트", "서큐버스", "수도사").any { it in name } -> demon
        else -> goblin // 고블린, 트롤, 리자드맨, 코볼트, 예티, 하피
    }

    private val colors = listOf(
        "슬라임" to 0xFF4FC3F7, "들쥐" to 0xFF8D6E63, "늑대" to 0xFF9E9E9E, "고블린" to 0xFF7CB342,
        "독버섯" to 0xFFAB47BC, "트롤" to 0xFF558B2F, "리자드맨" to 0xFF2E7D32, "독두꺼비" to 0xFF9CCC65,
        "망령" to 0xFFB0BEC5, "박쥐" to 0xFF5E35B1, "골렘" to 0xFF8D8D8D, "코볼트" to 0xFFA1887F,
        "설원" to 0xFFE0E0E0, "얼음" to 0xFF81D4FA, "예티" to 0xFFECEFF1, "해골" to 0xFFE8E0C8,
        "수도사" to 0xFF6D4C41, "유령" to 0xFFB39DDB, "전갈" to 0xFFD4A33C, "미라" to 0xFFD7CCA8,
        "웜" to 0xFFC8A060, "임프" to 0xFFE53935, "용암" to 0xFFFF7043, "도마뱀" to 0xFFFF8F00,
        "하피" to 0xFF90A4AE, "번개" to 0xFFFFEE58, "그리폰" to 0xFFBCAAA4, "데스나이트" to 0xFF37474F,
        "서큐버스" to 0xFFAD1457, "사냥개" to 0xFF4E342E,
    )

    fun monster(name: String): Sprite {
        val main = Color(colors.firstOrNull { it.first in name }?.second ?: 0xFF7CB342)
        return Sprite(
            shapeFor(name),
            mapOf(
                'K' to Pal.outline,
                'M' to main,
                'm' to main.copy(red = main.red * 0.7f, green = main.green * 0.7f, blue = main.blue * 0.7f),
                'E' to Color(0xFFFF3B30),
                'W' to Pal.white,
                'O' to Color(0xFF3E2723),
                'S' to Pal.steel,
                'G' to Pal.gold,
            ),
        )
    }
}

/** 레이드 보스: 고대 마룡 바르카스 (24×20). 진화 단계마다 색이 바뀐다 */
object DragonSprite {
    private val rows = listOf(
        ".........KK.....KK......",
        "........KMMK...KMMK.....",
        "........KMMMKKKMMMK.....",
        ".......KMMMMMMMMMMK.....",
        "......KMMEMMMMMMEMK.....",
        "......KMMMMMMMMMMMK.....",
        ".....KMMMWWWWWWMMMK.....",
        "..KK.KMMMMMMMMMMMK..KK..",
        ".KWWKKMMMMMMMMMMMKKKWWK.",
        "KWWWWKMMMMMMMMMMMMKWWWWK",
        "KWWWWKMMMMGGGGMMMMKWWWWK",
        "KWWWKKMMMGGGGGGMMMKKWWWK",
        ".KWK.KMMMGGGGGGMMMK.KWK.",
        "..K..KMMMMGGGGMMMMK..K..",
        ".....KMMMMMMMMMMMMK.....",
        "......KMMMMMMMMMMK......",
        "......KMMK....KMMKK.....",
        ".....KMMMK....KMMMKMK...",
        ".....KKKKK....KKKKKMMK..",
        "....................KK..",
    )

    /** 단계별 (몸, 날개, 배, 눈) */
    private val stages = listOf(
        listOf(0xFF5FAF4A, 0xFF9AD07A, 0xFFE8E0A0, 0xFF1A120B),
        listOf(0xFFB3362B, 0xFFE07A50, 0xFFF2C48A, 0xFFFFD54F),
        listOf(0xFF5B3A9E, 0xFF8E6CD8, 0xFFC9B8F0, 0xFFFF5252),
        listOf(0xFF2A2230, 0xFF6E1F2B, 0xFFE2B13C, 0xFFFF1744),
    ).map { it.map(::Color) }

    /** [stage] 1~4 */
    fun stage(stage: Int): Sprite {
        val c = stages[(stage - 1).coerceIn(0, 3)]
        return Sprite(rows, mapOf('K' to Pal.outline, 'M' to c[0], 'W' to c[1], 'G' to c[2], 'E' to c[3]))
    }
}
