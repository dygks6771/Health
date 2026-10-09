package com.example.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.health.health.StepsTestScreen
import com.example.health.ui.theme.HealthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StepsTestScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
