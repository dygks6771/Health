package com.example.health.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.health.settings.PrivacyPolicyContent
import com.example.health.ui.fantasy.FantasyBackground
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.theme.HealthTheme

/**
 * Health Connect 권한 화면에서 "개인정보처리방침"을 누르면 열리는 화면 (Health Connect 필수 요구사항).
 * 앱이 운동 데이터를 왜, 어떻게 쓰는지 보여준다.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthTheme {
                FantasyBackground {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ScreenHeader("개인정보처리방침", onBack = null)
                        PrivacyPolicyContent()
                        GoldButton("확인", onClick = { finish() }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
