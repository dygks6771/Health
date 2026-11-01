package com.example.healthrpg.run

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/{userId}/runs")
class RunController(
    private val runService: RunService,
) {
    /** 러닝 기록 동기화 → 보상 + 캐릭터 상태 */
    @PostMapping
    fun sync(
        @PathVariable userId: Long,
        @Valid @RequestBody request: SyncRunsRequest,
    ): SyncRunsResponse = runService.sync(userId, request)
}
