package com.example.health.game

/**
 * 영웅 초상화·직업 아이콘. 일러스트 리소스가 생기기 전까지 이모지로 표시.
 * 외형 순서는 서버 Appearances.names 와 같아야 함.
 */
object Portraits {
    private val appearances = listOf("🧑‍🦱", "👩‍🦰", "🧔", "🧝", "🧙‍♀️", "👸")

    fun appearance(index: Int): String = appearances.getOrElse(index) { "🧑" }

    fun job(job: String): String = when (job) {
        "KNIGHT" -> "🛡️"
        "WARRIOR" -> "⚔️"
        "ARCHER" -> "🏹"
        "MAGE" -> "🔮"
        "PRIEST" -> "✨"
        else -> "⚔️"
    }

    /** 지역 1~10 */
    fun region(tier: Int): String = listOf("🏘️", "🌲", "🌫️", "⛏️", "❄️", "⛪", "🏜️", "🌋", "☁️", "🏰").getOrElse(tier - 1) { "🗺️" }

    fun raidStage(stage: Int): String = listOf("🐲", "🐉", "🐉", "☠️").getOrElse(stage - 1) { "🐉" }
}
