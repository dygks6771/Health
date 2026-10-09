package com.example.healthrpg.steps

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/users/{userId}/steps")
class StepsController(
    private val stepsService: StepsService,
) {
    @PostMapping
    fun sync(
        @PathVariable userId: Long,
        @Valid @RequestBody request: SyncStepsRequest,
    ): List<DailyStepsResponse> =
        stepsService.sync(userId, request).map(DailyStepsResponse::from)

    @GetMapping
    fun summary(
        @PathVariable userId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): StepsSummaryResponse = stepsService.summary(userId, from, to)
}
