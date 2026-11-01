package com.example.health.ui.fantasy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.GradeCommon
import com.example.health.ui.theme.GradeEpic
import com.example.health.ui.theme.GradeLegendary
import com.example.health.ui.theme.GradeRare
import com.example.health.ui.theme.GradeUncommon
import com.example.health.ui.theme.Ink
import com.example.health.ui.theme.Leather
import com.example.health.ui.theme.PanelBrown
import com.example.health.ui.theme.PanelBrownLight
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import com.example.health.ui.theme.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val PanelShape = RoundedCornerShape(10.dp)
private val GoldEdge = Brush.verticalGradient(listOf(GoldBright, Gold, GoldDim))

/** 화면 전체 배경: 위는 가죽색, 아래로 갈수록 어두워지는 그라데이션 */
@Composable
fun FantasyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Leather, Ink))),
        content = content,
    )
}

/** 금장 테두리 패널 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(if (highlight) 2.dp else 1.dp, GoldEdge, PanelShape)
            .background(
                Brush.verticalGradient(if (highlight) listOf(PanelBrownLight, PanelBrown) else listOf(PanelBrown, Leather)),
                PanelShape,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) PanelTitle(title)
        content()
    }
}

@Composable
fun PanelTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Gold)
}

@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (enabled) GoldBright else GoldDim),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Ink,
            disabledContainerColor = PanelBrownLight,
            disabledContentColor = ParchmentDim,
        ),
    ) {
        Text(text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, GoldDim),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Parchment),
    ) {
        Text(text, fontFamily = FontFamily.Serif)
    }
}

/** 상단 제목 줄: ◀ 돌아가기 + 금색 제목 */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("◀", color = Gold) }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Gold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun FantasyBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = Track,
    )
}

/** 금화·행동력 같은 재화 표시 */
@Composable
fun ResourceChip(icon: String, text: String, modifier: Modifier = Modifier) {
    Text(
        "$icon $text",
        modifier = modifier
            .border(1.dp, GoldDim, RoundedCornerShape(12.dp))
            .background(Ink.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Parchment,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
    )
}

fun gradeColor(gradeName: String): Color = when (gradeName) {
    "고급" -> GradeUncommon
    "희귀" -> GradeRare
    "영웅" -> GradeEpic
    "전설" -> GradeLegendary
    else -> GradeCommon
}

/** 버튼 요청의 진행 중/오류/결과 메시지 상태 */
@Stable
class Actions(private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)

    /** [block] 이 돌려준 문자열은 성공 메시지로 표시 */
    fun run(block: suspend () -> String?) {
        if (busy) return
        busy = true
        error = null
        message = null
        scope.launch {
            try {
                message = block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun rememberActions(): Actions {
    val scope = rememberCoroutineScope()
    return remember { Actions(scope) }
}

@Composable
fun ActionFeedback(actions: Actions) {
    actions.error?.let { Text("⚠ $it", color = Crimson, style = MaterialTheme.typography.bodyMedium) }
    actions.message?.let { Text(it, color = GoldBright, style = MaterialTheme.typography.bodyMedium) }
}
