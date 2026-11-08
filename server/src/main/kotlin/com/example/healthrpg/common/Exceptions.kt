package com.example.healthrpg.common

import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

class NotFoundException(message: String, val code: String = "NOT_FOUND") : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)

/** 로그인 안 함 / 토큰 만료 */
class UnauthorizedException(message: String = "로그인이 필요합니다") : RuntimeException(message)

/** 이미 있는 아이디·이름 등 */
class ConflictException(message: String) : RuntimeException(message)

/** 로그인 실패가 너무 많음 등 잠시 후 다시 시도해야 하는 요청 */
class TooManyRequestsException(message: String) : RuntimeException(message)

data class ErrorResponse(val code: String, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NotFoundException) = ErrorResponse(e.code, e.message ?: "not found")

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(e: UnauthorizedException) = ErrorResponse("UNAUTHORIZED", e.message ?: "unauthorized")

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(e: ConflictException) = ErrorResponse("CONFLICT", e.message ?: "conflict")

    @ExceptionHandler(TooManyRequestsException::class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    fun handleTooMany(e: TooManyRequestsException) = ErrorResponse("TOO_MANY_REQUESTS", e.message ?: "too many requests")

    @ExceptionHandler(BadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(e: BadRequestException) = ErrorResponse("BAD_REQUEST", e.message ?: "bad request")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(e: MethodArgumentNotValidException): ErrorResponse {
        val message = e.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" }
        return ErrorResponse("VALIDATION_ERROR", message)
    }
}
