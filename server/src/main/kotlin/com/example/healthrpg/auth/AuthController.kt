package com.example.healthrpg.auth

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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
) {
    /** 회원가입 후 바로 로그인 상태(토큰 발급) */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: AuthRequest): TokenResponse {
        val account = authService.signup(request.loginId, request.password)
        return TokenResponse(authService.issueToken(account), account.loginId)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthRequest): TokenResponse =
        TokenResponse(authService.login(request.loginId, request.password), request.loginId.trim().lowercase())

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestHeader("Authorization", required = false) authorization: String?) {
        authService.logout(LoginAccountResolver.bearerToken(authorization))
    }
}
