package com.example.healthrpg.game

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/{userId}/character")
class GameController(
    private val gameService: GameService,
) {
    /** 캐릭터 화면: 레벨, 경험치, 골드, 오늘 걸음수, 퀘스트 진행 상황 */
    @GetMapping
    fun character(@PathVariable userId: Long): CharacterResponse = gameService.character(userId)
}
