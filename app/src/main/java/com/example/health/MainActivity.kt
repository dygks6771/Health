package com.example.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.health.character.CharacterScreen
import com.example.health.health.StepsTestScreen
import com.example.health.sync.StepsSyncWorker
import com.example.health.ui.theme.HealthTheme

class MainActivity : ComponentActivity() {

    /** 앱으로 돌아올 때마다 증가 → 캐릭터 화면이 자동으로 다시 동기화 */
    private var resumeCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StepsSyncWorker.schedule(this)
        setContent {
            HealthTheme {
                HealthApp(resumeKey = resumeCount)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
    }
}

@Composable
fun HealthApp(resumeKey: Int) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("⚔️") },
                    label = { Text("캐릭터") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("🛠️") },
                    label = { Text("개발자") },
                )
            }
        },
    ) { innerPadding ->
        when (tab) {
            0 -> CharacterScreen(resumeKey = resumeKey, modifier = Modifier.padding(innerPadding))
            else -> StepsTestScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
