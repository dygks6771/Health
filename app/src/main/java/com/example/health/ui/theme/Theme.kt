package com.example.health.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FantasyColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    primaryContainer = PanelBrownLight,
    onPrimaryContainer = GoldBright,
    secondary = Mana,
    tertiary = Emerald,
    background = Ink,
    onBackground = Parchment,
    surface = PanelBrown,
    onSurface = Parchment,
    surfaceVariant = PanelBrownLight,
    onSurfaceVariant = ParchmentDim,
    outline = GoldDim,
    error = Crimson,
)

/** 항상 어두운 판타지 테마 (기기 다크모드·동적 색상 무시) */
@Composable
fun HealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FantasyColorScheme, typography = Typography, content = content)
}
