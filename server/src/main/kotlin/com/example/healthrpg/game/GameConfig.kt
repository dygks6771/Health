package com.example.healthrpg.game

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.random.Random

@Configuration
class GameConfig {
    /** 레이드 장신구 종류 뽑기용. 테스트에서 고정값으로 바꿀 수 있도록 빈으로 둠 */
    @Bean
    fun gameRandom(): Random = Random.Default
}
