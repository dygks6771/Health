package com.example.health.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.sprites.NpcSprites
import com.example.health.ui.sprites.PixelSprite
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim

/** 튜토리얼 한 장면에서 화살표로 가리킬 곳 */
private enum class Pointer { NONE, TOP_RIGHT_MENU, BOTTOM_BAR }

private data class TutorialStep(
    val icon: String,
    val title: String,
    /** 길드 접수원 리나의 대사 */
    val line: String,
    val tips: List<String> = emptyList(),
    val pointer: Pointer = Pointer.NONE,
)

private val STEPS = listOf(
    TutorialStep(
        "⚔️", "모험자 길드에 오신 걸 환영해요!",
        "저는 길드 접수원 리나예요. 이 세계에서는 실제로 달린 만큼 영웅이 강해진답니다. 차근차근 알려 드릴게요!",
    ),
    TutorialStep(
        "🏃", "달리면 경험치",
        "삼성헬스 같은 운동 앱으로 러닝을 기록하면, 그 거리만큼 경험치와 골드를 받아요.",
        listOf(
            "러닝 1km = 경험치 50 · 골드 20",
            "기록은 자동으로 가져와요 (앱을 열 때, 그리고 꺼져 있어도 15분마다)",
            "무리는 금물! 하루 10km 를 넘는 거리는 보상이 절반이에요",
        ),
    ),
    TutorialStep(
        "👣", "걸으면 행동력",
        "평소에 걷는 걸음 수는 행동력이 돼요. 행동력으로 토벌·던전·레이드에 도전할 수 있어요.",
        listOf("100보 = 행동력 1 (하루 최대 150)", "행동력은 최대 300 까지 모아 둘 수 있어요"),
    ),
    TutorialStep(
        "🎯", "이번 주 원정",
        "매주 월요일, 당신의 지난 기록에 맞춘 주간 목표 거리가 생겨요. 로비에서 진행 상황을 확인하세요.",
        listOf("목표를 달성하면 보물 상자!", "🔥 3주 연속 달성하면 장비가 든 황금 상자"),
    ),
    TutorialStep(
        "🗺️", "원정 지도",
        "지금까지 달린 거리가 쌓이면 새로운 지역이 열려요. 초보자 마을에서 마왕성 외곽까지, 달려서 개척해 보세요!",
        listOf("지역마다 던전과 그 지역의 장비가 있어요"),
    ),
    TutorialStep(
        "📜", "바로가기",
        "화면 아래에서 자주 쓰는 곳으로 바로 갈 수 있어요.",
        listOf(
            "모험: 지역 던전 공략 (장비 획득)",
            "길드: 매일 바뀌는 토벌 의뢰 5개",
            "레이드: 친구 4명과 주 1회 마룡 토벌 — 모두가 많이 뛸수록 강해져요",
            "영웅·가방: 레벨 업 포인트 찍기, 장비 장착",
        ),
        Pointer.BOTTOM_BAR,
    ),
    TutorialStep(
        "☰", "메뉴",
        "오른쪽 위 ☰ 를 누르면 상점, 경매장, 대장간(강화), 랭킹, 친구, 설정이 있어요.",
        listOf("친구가 부족하면 친구 메뉴에서 용병을 모을 수 있어요", "이 안내는 설정에서 다시 볼 수 있어요"),
        Pointer.TOP_RIGHT_MENU,
    ),
    TutorialStep(
        "🔐", "마지막으로, 권한 허용",
        "운동 기록을 읽으려면 Health Connect 권한이 필요해요. 이어서 나오는 화면에서 운동·거리·걸음 수 읽기를 허용해 주세요.",
        listOf(
            "삼성헬스: 설정 → Health Connect 연동을 켜 두세요",
            "알림을 허용하면 레벨 업과 원정 달성을 알려 드려요",
            "자, 이제 첫 러닝을 떠나 볼까요?",
        ),
    ),
)

/**
 * 처음 한 번 보여주는 앱 사용 안내. 길드 접수원 리나가 한 장씩 설명하고,
 * 바로가기·메뉴 설명에서는 실제 위치를 화살표로 가리킨다.
 */
@Composable
fun TutorialOverlay(onFinish: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val step = STEPS[index]
    val last = index == STEPS.lastIndex
    BackHandler { if (index > 0) index-- else onFinish() }

    val bounce = rememberInfiniteTransition(label = "pointer")
    val offset by bounce.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pointerOffset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            // 뒤쪽 화면이 눌리지 않도록 터치를 여기서 받음
            .clickable(onClick = {}),
    ) {
        when (step.pointer) {
            Pointer.TOP_RIGHT_MENU -> Text(
                "▲",
                color = GoldBright,
                fontSize = 30.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 22.dp).offset(y = offset.dp),
            )
            Pointer.BOTTOM_BAR -> Text(
                "▼  ▼  ▼  ▼",
                color = GoldBright,
                fontSize = 26.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp).offset(y = offset.dp),
            )
            Pointer.NONE -> Unit
        }

        Panel(
            highlight = true,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelSprite(NpcSprites.receptionist.sprite, size = 76.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("${NpcSprites.receptionist.name} · ${NpcSprites.receptionist.title}", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    Text("${step.icon} ${step.title}", color = GoldBright, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Text("\"${step.line}\"", color = Parchment, style = MaterialTheme.typography.bodyMedium)
            step.tips.forEach { tip ->
                Text("• $tip", color = Gold, style = MaterialTheme.typography.bodySmall)
            }

            // 진행 점
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                STEPS.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == index) 10.dp else 7.dp)
                            .background(if (i <= index) Gold else GoldDim.copy(alpha = 0.5f), CircleShape),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    GhostButton("이전", onClick = { index-- }, modifier = Modifier.weight(1f))
                } else {
                    TextButton(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("건너뛰기", color = ParchmentDim) }
                }
                GoldButton(
                    if (last) "모험 시작!" else "다음 (${index + 1}/${STEPS.size})",
                    onClick = { if (last) onFinish() else index++ },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
