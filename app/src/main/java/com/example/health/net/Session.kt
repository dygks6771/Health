package com.example.health.net

import android.content.Context

/** 서버 주소, 로그인 토큰, 마지막 영웅 상태를 폰에 저장 */
class Session(context: Context) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, null) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

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
         * USB 연결 + `adb reverse tcp:8080 tcp:8080` 을 하면 폰의 127.0.0.1:8080 이 PC 의 8080 으로 연결됨.
         * 와이파이로 접속할 땐 설정 화면에서 http://<PC IP>:8080 으로 바꾸면 됨.
         */
        const val DEFAULT_SERVER_URL = "http://127.0.0.1:8080"

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_LOGIN_ID = "login_id"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_HERO = "hero"
    }
}
