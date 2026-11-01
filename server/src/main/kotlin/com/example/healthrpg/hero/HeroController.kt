package com.example.healthrpg.hero

import com.example.healthrpg.auth.LoginAccount
import com.example.healthrpg.game.Appearances
import com.example.healthrpg.game.Job
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class HeroController(
    private val heroService: HeroService,
) {
    /** 영웅 생성 화면용: 직업·외형 목록 (로그인 불필요) */
    @GetMapping("/api/game/meta")
    fun meta() = GameMetaResponse(
        jobs = Job.entries.map {
            JobInfo(
                it, it.displayName, it.role, it.primary, it.primary.displayName,
                it.secondary, it.secondary.displayName, it.description,
            )
        },
        appearances = Appearances.names.mapIndexed { i, name -> AppearanceInfo(i, name) },
    )

    /** 영웅 이름 중복 확인 (로그인 불필요) */
    @GetMapping("/api/game/names/available")
    fun nameAvailable(@RequestParam name: String): NameCheckResponse {
        val problem = heroService.nameProblem(name)
        return NameCheckResponse(name.trim(), available = problem == null, reason = problem)
    }

    /** 내 영웅. 아직 없으면 404 (code=HERO_NOT_FOUND) → 앱이 영웅 생성 화면으로 이동 */
    @GetMapping("/api/me/hero")
    fun hero(@LoginAccount accountId: Long): HeroResponse = heroService.response(heroService.require(accountId))

    @PostMapping("/api/me/hero")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@LoginAccount accountId: Long, @Valid @RequestBody request: CreateHeroRequest): HeroResponse =
        heroService.create(accountId, request)

    @PostMapping("/api/me/hero/stats")
    fun allocate(@LoginAccount accountId: Long, @Valid @RequestBody request: AllocateStatsRequest): HeroResponse =
        heroService.allocate(accountId, request)
}
