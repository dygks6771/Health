package com.example.healthrpg.game

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/{userId}")
class GameController(
    private val characterService: CharacterService,
    private val contentService: ContentService,
) {
    /** 캐릭터: 레벨, 경험치, 골드, 행동력, 스탯, 장비 */
    @GetMapping("/character")
    fun character(@PathVariable userId: Long): CharacterResponse = characterService.character(userId)

    /** 스탯 포인트 찍기 */
    @PostMapping("/stats")
    fun allocate(
        @PathVariable userId: Long,
        @Valid @RequestBody request: AllocateStatsRequest,
    ): CharacterResponse = characterService.allocate(userId, request)

    /** 무기 강화 (+1) */
    @PostMapping("/weapon/enhance")
    fun enhance(@PathVariable userId: Long): EnhanceResponse = characterService.enhanceWeapon(userId)

    /** 토벌/던전/레이드 목록과 단계별 입장 조건 */
    @GetMapping("/content")
    fun content(@PathVariable userId: Long): List<ContentResponse> = contentService.list(userId)

    /** 도전: type = HUNT | DUNGEON | RAID, difficulty = 1~20 */
    @PostMapping("/content/{type}/{difficulty}")
    fun attempt(
        @PathVariable userId: Long,
        @PathVariable type: ContentType,
        @PathVariable difficulty: Int,
    ): ContentResultResponse = contentService.attempt(userId, type, difficulty)
}
