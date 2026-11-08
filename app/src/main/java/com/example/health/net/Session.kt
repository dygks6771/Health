package com.example.health.net

import android.content.Context

/** 서버 주소, 로그인 토큰, 마지막 영웅 상태를 폰에 저장 */
class Session(context: Context) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, null) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, normalizeServerUrl(value)).apply()

    /** 로그인 토큰. 없으면 로그아웃 상태 */
    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var loginId: String?
        get() = prefs.getString(KEY_LOGIN_ID, null)
        set(value) = prefs.edit().putString(KEY_LOGIN_ID, value).apply()

    /** 마지막으로 러닝 동기화에 성공한 시각 (epoch ms, 없으면 0) */
    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC_AT, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_AT, value).apply()

    /** 서버에 연결되지 않을 때도 로비를 보여줄 수 있도록 마지막 영웅 상태 저장 */
    var cachedHeroJson: String?
        get() = prefs.getString(KEY_HERO, null)
        set(value) = prefs.edit().putString(KEY_HERO, value).apply()

    fun logout() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_HERO).remove(KEY_LAST_SYNC_AT).apply()
    }

    companion object {
        /**
         * 서버 주소 (설정 화면에서 변경)
         * - USB: `adb reverse tcp:8080 tcp:8080` 후 http://127.0.0.1:8080
         * - 같은 와이파이: http://<PC 와이파이 IP>:8080
         * - 밖에서(원격): Tailscale 주소 http://100.x.x.x:8080 또는 ngrok 등의 https 주소
         * 서버를 켜면 콘솔에 폰에서 쓸 수 있는 주소가 출력된다.
         */
        const val DEFAULT_SERVER_URL = "http://127.0.0.1:8080"

        /**
         * 사용자가 입력한 주소 정리: 앞뒤 공백·끝의 / 와 /api 제거, http(s):// 가 없으면 붙인다.
         * IP·localhost·포트가 있는 주소는 개발용 PC 서버로 보고 http, 그 외 도메인은 https.
         */
        fun normalizeServerUrl(input: String): String {
            var url = input.trim().trimEnd('/')
            if (url.isEmpty()) return DEFAULT_SERVER_URL
            if (url.endsWith("/api")) url = url.removeSuffix("/api")
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                val host = url.substringBefore('/').substringBefore(':')
                val looksLocal = host == "localhost" || host.all { it.isDigit() || it == '.' } || ':' in url.substringBefore('/')
                url = (if (looksLocal) "http://" else "https://") + url
            }
            return url.trimEnd('/')
        }

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_LOGIN_ID = "login_id"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_HERO = "hero"
    }
}
