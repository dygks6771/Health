package com.example.health.auth

import com.example.health.BuildConfig
import com.example.health.settings.PrivacyPolicyContent
import com.example.health.settings.ServerAddressHelp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.net.ApiClient
import com.example.health.net.Session
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.ParchmentDim

/** 로그인 / 회원가입. 성공하면 토큰을 저장하고 [onLoggedIn] */
@Composable
fun AuthScreen(session: Session, api: ApiClient, onLoggedIn: () -> Unit) {
    var signup by remember { mutableStateOf(false) }
    var loginId by remember { mutableStateOf(session.loginId ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var showServer by remember { mutableStateOf(false) }
    var showPolicy by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(session.serverUrl) }
    val actions = rememberActions()

    fun submit() = actions.run {
        if (BuildConfig.DEBUG) session.serverUrl = serverUrl
        if (signup && password != passwordCheck) error("비밀번호 확인이 일치하지 않습니다")
        val result = if (signup) api.signup(loginId, password) else api.login(loginId, password)
        session.token = result.getString("token")
        session.loginId = result.getString("loginId")
        onLoggedIn()
        null
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚔️", fontSize = 64.sp)
        Text("러너스 크로니클", style = MaterialTheme.typography.headlineMedium, color = Gold)
        Text(
            "달린 만큼 강해지는 모험가의 이야기",
            color = ParchmentDim,
            textAlign = TextAlign.Center,
        )

        Panel(title = if (signup) "모험가 등록" else "모험가 입장") {
            OutlinedTextField(
                value = loginId,
                onValueChange = { loginId = it },
                label = { Text("아이디 (영문 소문자·숫자 4~20자)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호 (8자 이상)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (signup) {
                OutlinedTextField(
                    value = passwordCheck,
                    onValueChange = { passwordCheck = it },
                    label = { Text("비밀번호 확인") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ActionFeedback(actions)
            GoldButton(
                if (actions.busy) "..." else if (signup) "등록하고 시작하기" else "입장하기",
                onClick = { submit() },
                enabled = !actions.busy && loginId.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { signup = !signup }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (signup) "이미 계정이 있나요? 로그인" else "처음인가요? 회원가입", color = Gold)
            }
        }

        if (signup) {
            Text(
                "가입하면 개인정보처리방침(운동 기록은 게임 보상 계산에만 사용)에 동의하는 것으로 봅니다.",
                color = ParchmentDim,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row {
            TextButton(onClick = { showPolicy = !showPolicy }) { Text("개인정보처리방침", color = ParchmentDim) }
            // 서버 주소는 개발 빌드에서만 (출시 빌드는 정해진 서버)
            if (BuildConfig.DEBUG) {
                TextButton(onClick = { showServer = !showServer }) { Text("서버 설정", color = ParchmentDim) }
            }
        }
        if (showPolicy) PrivacyPolicyContent()
        if (showServer && BuildConfig.DEBUG) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("서버 주소") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            ServerAddressHelp()
        }
    }
}
