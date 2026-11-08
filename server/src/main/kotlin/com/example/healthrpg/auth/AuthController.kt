package com.example.healthrpg.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import com.example.healthrpg.common.UnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class AuthRequest(
    @field:NotBlank val loginId: String,
    @field:NotBlank val password: String,
)

data class TokenResponse(val token: String, val loginId: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val limits: AuthLimits,
) {
    /** 회원가입 후 바로 로그인 상태(토큰 발급) */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: AuthRequest, http: HttpServletRequest): TokenResponse {
        val ip = http.remoteAddr
        limits.signupPerIp.check(ip, limits::waitMessage)
        val account = authService.signup(request.loginId, request.password)
        limits.signupPerIp.record(ip)
        return TokenResponse(authService.issueToken(account), account.loginId)
    }

    /** 아이디·IP 별로 실패가 많으면 잠시 차단 (비밀번호 대입 방지) */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthRequest, http: HttpServletRequest): TokenResponse {
        val loginId = request.loginId.trim().lowercase()
        val ip = http.remoteAddr
        limits.loginPerId.check(loginId, limits::waitMessage)
        limits.loginPerIp.check(ip, limits::waitMessage)
        val token = try {
            authService.login(loginId, request.password)
        } catch (e: UnauthorizedException) {
            limits.loginPerId.record(loginId)
            limits.loginPerIp.record(ip)
            throw e
        }
        limits.loginPerId.reset(loginId)
        return TokenResponse(token, loginId)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestHeader("Authorization", required = false) authorization: String?) {
        authService.logout(LoginAccountResolver.bearerToken(authorization))
    }
}
