package com.example.healthrpg.auth

import com.example.healthrpg.common.UnauthorizedException
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 컨트롤러 파라미터에 붙이면 Authorization: Bearer <토큰> 으로 로그인한 계정 ID 가 들어온다.
 * 토큰이 없거나 틀리면 401.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginAccount

class LoginAccountResolver(private val authService: AuthService) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginAccount::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any = authService.accountIdFor(bearerToken(webRequest.getHeader("Authorization")))

    companion object {
        fun bearerToken(header: String?): String {
            if (header == null || !header.startsWith("Bearer ")) throw UnauthorizedException()
            return header.removePrefix("Bearer ").trim().ifEmpty { throw UnauthorizedException() }
        }
    }
}

@Configuration
class AuthWebConfig(private val authService: AuthService) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(LoginAccountResolver(authService))
    }
}
