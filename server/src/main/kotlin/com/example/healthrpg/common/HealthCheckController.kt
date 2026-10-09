package com.example.healthrpg.common

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 서버/DB 연결 확인용. 앱에서 서버 주소 테스트할 때 사용 */
@RestController
@RequestMapping("/api/health")
class HealthCheckController(
    private val jdbcTemplate: JdbcTemplate,
) {
    @GetMapping
    fun health(): HealthResponse {
        val dbUp = runCatching { jdbcTemplate.queryForObject("SELECT 1", Int::class.java) == 1 }
            .getOrDefault(false)
        return HealthResponse(status = if (dbUp) "UP" else "DEGRADED", database = if (dbUp) "UP" else "DOWN")
    }
}

data class HealthResponse(val status: String, val database: String)
