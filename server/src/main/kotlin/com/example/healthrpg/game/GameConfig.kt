package com.example.healthrpg.game

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.random.Random

@Configuration
class GameConfig {
    /** 드롭·강화·레이드 치명타 등 확률용. 테스트에서 바꿀 수 있도록 빈으로 둠 */
    @Bean
    fun gameRandom(): Random = Random.Default
}
