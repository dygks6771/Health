package com.example.healthrpg.common

import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

class NotFoundException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)

data class ErrorResponse(val code: String, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NotFoundException) = ErrorResponse("NOT_FOUND", e.message ?: "not found")

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
