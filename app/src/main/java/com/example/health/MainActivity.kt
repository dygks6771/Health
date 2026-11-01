package com.example.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.health.app.HealthRpgApp
import com.example.health.sync.SyncWorker
import com.example.health.ui.theme.HealthTheme

class MainActivity : ComponentActivity() {

    /** 앱으로 돌아올 때마다 증가 → 로비가 러닝 기록을 다시 동기화 */
    private var resumeCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SyncWorker.schedule(this)
        setContent {
            HealthTheme {
                HealthRpgApp(resumeKey = resumeCount)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
    }
}
