package com.example.health.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.Parchment

/** 시행일 (내용을 바꾸면 함께 수정) */
const val PRIVACY_POLICY_DATE = "2026-11-08"

/**
 * 개인정보처리방침. 스토어 등록용 웹 페이지(docs/privacy-policy.md)와 내용을 같게 유지한다.
 * Health Connect 권한 화면의 "개인정보처리방침"과 설정 화면에서 보여준다.
 */
val PRIVACY_POLICY: List<Pair<String, String>> = listOf(
    "1. 수집하는 정보" to
        "• 계정: 아이디, 비밀번호(복원할 수 없는 형태로 암호화해 저장)\n" +
        "• 게임: 영웅 이름·직업·외형, 레벨·장비·재화 등 게임 진행 기록, 친구 관계\n" +
        "• 운동(Health Connect): 러닝 운동 기록의 시작·종료 시각과 거리, 날짜별 걸음 수 합계\n" +
        "• 그 외 위치, 심박수, 체중 등 다른 건강 정보는 읽지 않습니다.",
    "2. 이용 목적" to
        "러닝 거리로 경험치·골드·원정 진행을, 걸음 수로 행동력을 계산하고 주간 목표·랭킹·레이드 등 게임 기능을 제공하는 데에만 사용합니다. " +
        "광고, 마케팅, 판매 목적으로 사용하지 않으며 제3자에게 제공하지 않습니다.",
    "3. Health Connect 데이터" to
        "Health Connect 에서 읽은 데이터는 위 목적에만 쓰며, 사용자가 허용한 항목(운동·거리·걸음 수 읽기)만 읽습니다. " +
        "권한은 언제든 휴대폰의 Health Connect 설정에서 해제할 수 있고, 해제하면 이후 기록은 읽지 않습니다.",
    "4. 보관과 삭제" to
        "정보는 회원 탈퇴 전까지 보관합니다. 앱의 설정 → 회원 탈퇴(또는 서버의 계정 삭제 웹 페이지)를 하면 계정과 영웅, 장비, 러닝·걸음 기록, 친구 관계가 즉시 모두 삭제되며 복구할 수 없습니다.",
    "5. 안전성 확보" to
        "비밀번호는 암호화해 저장하고, 로그인 정보는 휴대폰 안 앱 전용 저장소에만 보관합니다. 서버와의 통신은 암호화(HTTPS)됩니다.",
    "6. 문의" to
        "개인정보 관련 문의는 스토어 등록 정보의 개발자 연락처(이메일)로 보내 주세요.",
)

@Composable
fun PrivacyPolicyContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("러너스 크로니클은 게임에 꼭 필요한 정보만 수집합니다. (시행일 $PRIVACY_POLICY_DATE)", color = Parchment, style = MaterialTheme.typography.bodySmall)
        PRIVACY_POLICY.forEach { (title, body) ->
            Text(title, color = Gold, fontWeight = FontWeight.Bold)
            Text(body, color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
    }
}
