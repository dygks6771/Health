package com.example.health.sync

import android.content.Context
import java.util.UUID

/** 서버 주소, 기기 ID 등 동기화 설정 저장 */
class SyncSettings(context: Context) {

    private val prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, null) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

    /** 로그인 기능 전까지 사용자 식별용. 앱 설치마다 한 번 생성 */
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_DEVICE_ID, it).apply() }

    var userId: Long?
        get() = prefs.getLong(KEY_USER_ID, -1).takeIf { it > 0 }
        set(value) = prefs.edit().putLong(KEY_USER_ID, value ?: -1).apply()

    /** 마지막으로 동기화에 성공한 시각 (epoch ms, 없으면 0) */
    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC_AT, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_AT, value).apply()

    /** 서버에 연결되지 않을 때도 보여줄 수 있도록 마지막 캐릭터 상태를 저장 */
    var cachedCharacterJson: String?
        get() = prefs.getString(KEY_CHARACTER, null)
        set(value) = prefs.edit().putString(KEY_CHARACTER, value).apply()

    companion object {
        /**
         * USB 연결 + `adb reverse tcp:8080 tcp:8080` 을 하면 폰의 127.0.0.1:8080 이 PC 의 8080 으로 연결됨.
         * 와이파이로 접속할 땐 앱 화면에서 http://<PC IP>:8080 으로 바꾸면 됨.
         */
        const val DEFAULT_SERVER_URL = "http://127.0.0.1:8080"

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_CHARACTER = "character"
    }
}
