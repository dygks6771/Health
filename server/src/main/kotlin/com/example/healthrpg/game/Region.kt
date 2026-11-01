package com.example.healthrpg.game

/**
 * 세계의 10개 지역과 각 지역의 던전. 초보자 마을부터 마왕성까지 점점 어려워짐.
 * 던전 입장: 최소 레벨 + 최소 능력치 합계 + 행동력 20.
 * 보상: 강화석, 골드, 확률적으로 그 지역 티어의 장비.
 */
enum class Region(
    val displayName: String,
    val description: String,
    val dungeonName: String,
    /** 토벌 의뢰 몬스터 */
    val monsters: List<String>,
    /** 던전 장비 이름 앞에 붙는 말 */
    val itemPrefix: String,
    /** 장비 등급 확률 (일반, 고급, 희귀, 영웅, 전설) % */
    val gradeWeights: List<Int>,
) {
    BEGINNER_VILLAGE(
        "초보자 마을", "모험이 시작되는 평화로운 마을. 모험자 길드 본부가 있다.", "마을 지하 수로",
        listOf("슬라임", "들쥐 떼", "굶주린 늑대"), "견습생의", listOf(70, 25, 5, 0, 0),
    ),
    WHISPERING_FOREST(
        "속삭이는 숲", "나무들이 속삭인다는 깊은 숲. 고블린이 들끓는다.", "고블린 소굴",
        listOf("고블린 척후병", "독버섯 정령", "숲 트롤"), "숲지기의", listOf(62, 28, 9, 1, 0),
    ),
    MISTY_SWAMP(
        "안개 늪지대", "한 치 앞도 보이지 않는 독안개의 늪.", "독안개 사원",
        listOf("늪지 리자드맨", "독두꺼비", "안개 망령"), "늪지 사냥꾼의", listOf(54, 30, 13, 3, 0),
    ),
    ABANDONED_MINE(
        "버려진 광산 협곡", "드워프들이 떠난 뒤 괴물의 소굴이 된 협곡.", "잊혀진 드워프 광산",
        listOf("동굴 박쥐 떼", "광산 골렘", "코볼트 광부"), "드워프", listOf(46, 32, 17, 5, 0),
    ),
    FROZEN_TUNDRA(
        "얼어붙은 설원", "모든 것이 얼어붙은 북쪽 끝의 대지.", "서리 거인의 요새",
        listOf("설원 늑대", "얼음 정령", "예티"), "서리의", listOf(38, 33, 21, 7, 1),
    ),
    RUINED_SANCTUARY(
        "폐허가 된 성지", "한때 신의 축복을 받았으나 저주받아 무너진 성지.", "타락한 대성당",
        listOf("해골 기사", "타락한 수도사", "원한의 유령"), "성기사의", listOf(30, 34, 25, 9, 2),
    ),
    SANDSTORM_DESERT(
        "모래폭풍 사막", "고대 왕국의 유적이 모래 아래 잠든 사막.", "고대 왕의 무덤",
        listOf("사막 전갈", "미라 병사", "모래 웜"), "사막 왕의", listOf(24, 33, 28, 12, 3),
    ),
    BURNING_LANDS(
        "타오르는 대지", "용암이 흐르고 하늘마저 불타는 대지.", "화염 군주의 용광로",
        listOf("화염 임프", "용암 골렘", "불꽃 도마뱀"), "화염의", listOf(18, 32, 31, 15, 4),
    ),
    SKY_ISLANDS(
        "천공의 부유섬", "구름 위를 떠다니는 폭풍의 섬들.", "폭풍의 첨탑",
        listOf("하피 여왕", "번개 정령", "그리폰"), "폭풍의", listOf(13, 30, 34, 18, 5),
    ),
    DEMON_CASTLE(
        "마왕성 외곽", "마왕의 군세가 집결한 세계의 끝.", "마왕의 심연",
        listOf("데스나이트", "서큐버스", "지옥 사냥개"), "마왕의", listOf(10, 27, 36, 21, 6),
    ),
    ;

    /** 1~10 */
    val tier: Int get() = ordinal + 1

    /** 던전 보스 (전투 장면 표시용): 지역 몬스터 중 가장 강한 마지막 몬스터의 우두머리 */
    val dungeonBoss: String get() = monsters.last() + " 우두머리"

    val dungeonMinLevel: Int get() = 1 + 5 * (tier - 1)

    /** 4가지 능력치 합계 최소값 (장비 포함). 고티어일수록 장비가 필요 */
    val dungeonMinTotalStats: Int get() = 20 + 24 * (tier - 1)

    val dungeonStaminaCost: Int get() = 20
    val dungeonStones: Int get() = 2 * tier
    val dungeonGold: Long get() = 40L * tier

    companion object {
        /** 장비 드롭 확률(%) */
        const val DROP_RATE = 60

        /** 레벨에 맞는 지역 (토벌 의뢰 몬스터 선택용) */
        fun forLevel(level: Int): Region = entries[((level - 1) / 5).coerceIn(0, entries.size - 1)]

        fun byTier(tier: Int): Region? = entries.getOrNull(tier - 1)
    }
}
