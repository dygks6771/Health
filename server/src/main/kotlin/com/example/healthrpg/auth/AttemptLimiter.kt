package com.example.healthrpg.auth

import com.example.healthrpg.common.TooManyRequestsException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 짧은 시간에 같은 키로 너무 많이 시도하면 잠시 막는다 (서버 1대 기준, 메모리에만 저장).
 * [maxAttempts] 번을 [window] 안에 넘기면 그 창이 끝날 때까지 거절.
 */
class AttemptLimiter(
    private val maxAttempts: Int,
    private val window: Duration,
    private val now: () -> Instant = Instant::now,
) {
    private class Bucket(var start: Instant, var count: Int)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    /** 막혀 있으면 남은 시간(초), 아니면 null */
    fun blockedSeconds(key: String): Long? {
        val bucket = buckets[key] ?: return null
        synchronized(bucket) {
            val end = bucket.start.plus(window)
            if (!now().isBefore(end)) {
                buckets.remove(key, bucket)
                return null
            }
            return if (bucket.count >= maxAttempts) Duration.between(now(), end).seconds.coerceAtLeast(1) else null
        }
    }

    fun record(key: String) {
        val bucket = buckets.computeIfAbsent(key) { Bucket(now(), 0) }
        synchronized(bucket) {
            if (!now().isBefore(bucket.start.plus(window))) {
                bucket.start = now()
                bucket.count = 0
            }
            bucket.count++
        }
        // 오래된 기록 정리 (메모리가 계속 늘지 않도록)
        if (buckets.size > MAX_KEYS) buckets.entries.removeIf { !now().isBefore(it.value.start.plus(window)) }
    }

    fun reset(key: String) {
        buckets.remove(key)
    }

    fun check(key: String, message: (Long) -> String) {
        blockedSeconds(key)?.let { throw TooManyRequestsException(message(it)) }
    }

    companion object {
        private const val MAX_KEYS = 10_000
    }
}

/** 로그인 비밀번호 대입·계정 대량 생성 방지. 횟수는 application.yml 의 game.auth.* 로 조정 */
@Component
class AuthLimits(
    @Value("\${game.auth.login-fails-per-id:5}") loginFailsPerId: Int,
    @Value("\${game.auth.login-fails-per-ip:20}") loginFailsPerIp: Int,
    @Value("\${game.auth.signups-per-ip-per-hour:10}") signupsPerIp: Int,
) {
    /** 같은 아이디로 15분 안에 5번 틀리면 15분 동안 로그인 차단 */
    val loginPerId = AttemptLimiter(loginFailsPerId, Duration.ofMinutes(15))

    /** 같은 IP 에서 15분 안에 20번 틀리면 차단 (여러 아이디를 돌려가며 시도하는 경우) */
    val loginPerIp = AttemptLimiter(loginFailsPerIp, Duration.ofMinutes(15))

    /** 같은 IP 에서 1시간에 회원가입 10번까지 */
    val signupPerIp = AttemptLimiter(signupsPerIp, Duration.ofHours(1))

    fun waitMessage(seconds: Long): String = "시도가 너무 많습니다. ${(seconds + 59) / 60}분 후 다시 시도해 주세요"
}
