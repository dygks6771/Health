package com.example.health.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.health.ui.theme.HealthTheme

/**
 * Health Connect 권한 화면에서 "개인정보처리방침"을 누르면 열리는 화면.
 * 정식 출시 전에는 실제 개인정보처리방침 페이지로 교체해야 함.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("걸음수 데이터 사용 안내", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "이 앱은 Health Connect 에 저장된 걸음수(삼성헬스 등)를 읽어 " +
                                "캐릭터 경험치와 레벨을 계산하는 데 사용합니다. " +
                                "걸음수 외의 건강 데이터는 읽지 않으며, 권한은 언제든 Health Connect 설정에서 해제할 수 있습니다.",
                        )
                        Button(onClick = { finish() }) { Text("확인") }
                    }
                }
            }
        }
    }
}
