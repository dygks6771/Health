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
import com.example.health.content.ContentScreen
import com.example.health.health.StepsTestScreen
import com.example.health.sync.SyncWorker
import com.example.health.ui.theme.HealthTheme

class MainActivity : ComponentActivity() {

    /** 앱으로 돌아올 때마다 증가 → 화면이 자동으로 다시 동기화 */
    private var resumeCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SyncWorker.schedule(this)
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

private data class Tab(val icon: String, val label: String)

private val tabs = listOf(Tab("🧍", "캐릭터"), Tab("⚔️", "컨텐츠"), Tab("🛠️", "개발자"))

@Composable
fun HealthApp(resumeKey: Int) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(t.icon) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (tab) {
            0 -> CharacterScreen(resumeKey = resumeKey, modifier = modifier)
            1 -> ContentScreen(resumeKey = resumeKey, modifier = modifier)
            else -> StepsTestScreen(modifier = modifier)
        }
    }
}
