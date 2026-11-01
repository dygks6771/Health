package com.example.health.hero

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.game.GameMeta
import com.example.health.game.Portraits
import com.example.health.net.ApiClient
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.sprites.HeroAvatar
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldDim
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim
import org.json.JSONObject

/** 영웅 생성: 외형 선택 → 직업 선택 → 이름 → 모험자 길드 등록 */
@Composable
fun CreateHeroScreen(api: ApiClient, onCreated: (JSONObject) -> Unit, onLogout: () -> Unit) {
    var meta by remember { mutableStateOf<GameMeta?>(null) }
    var appearance by remember { mutableIntStateOf(0) }
    var job by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var nameCheck by remember { mutableStateOf<String?>(null) }
    val actions = rememberActions()

    LaunchedEffect(Unit) {
        actions.run {
            meta = GameMeta.from(api.meta())
            null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("모험자 길드 등록", onBack = null) {
            TextButton(onClick = onLogout) { Text("로그아웃", color = ParchmentDim) }
        }
        Text("이 세계에 처음 발을 들인 당신, 어떤 모험가가 되시겠습니까?", color = ParchmentDim)

        val m = meta
        if (m == null) {
            ActionFeedback(actions)
            if (actions.busy) Text("길드 문서를 펼치는 중...", color = Gold)
            return@Column
        }

        Panel(title = "1. 외형 선택") {
            m.appearances.chunked(3).forEachIndexed { row, names ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    names.forEachIndexed { col, label ->
                        val index = row * 3 + col
                        val selected = appearance == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(if (selected) 2.dp else 1.dp, if (selected) Gold else GoldDim, RoundedCornerShape(8.dp))
                                .clickable { appearance = index }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            HeroAvatar(job ?: "WARRIOR", index, 64.dp)
                            Text(label, color = if (selected) Gold else Parchment, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        Panel(title = "2. 직업 선택") {
            m.jobs.forEach { j ->
                val selected = job == j.job
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(if (selected) 2.dp else 1.dp, if (selected) Gold else GoldDim, RoundedCornerShape(8.dp))
                        .clickable { job = j.job }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HeroAvatar(j.job, appearance, 52.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${j.name}  ·  ${j.role}", fontWeight = FontWeight.Bold, color = if (selected) Gold else Parchment)
                        Text(j.description, style = MaterialTheme.typography.bodySmall, color = ParchmentDim)
                        Text(
                            "${j.primaryName} +3 · ${j.secondaryName} +2 · 레벨업마다 ${j.primaryName} +1",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gold,
                        )
                    }
                }
            }
        }

        Panel(title = "3. 이름") {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(12)
                    nameCheck = null
                },
                label = { Text("영웅 이름 (한글·영문·숫자 2~12자)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(
                    "중복 확인",
                    onClick = {
                        actions.run {
                            val json = api.nameAvailable(name)
                            nameCheck = if (json.getBoolean("available")) "✓ 사용할 수 있는 이름입니다" else "✗ " + json.optString("reason")
                            null
                        }
                    },
                    enabled = !actions.busy && name.trim().length >= 2,
                )
                nameCheck?.let { Text(it, color = if (it.startsWith("✓")) Gold else Crimson, style = MaterialTheme.typography.bodySmall) }
            }
            ActionFeedback(actions)
            GoldButton(
                "모험 시작",
                onClick = {
                    val selectedJob = job ?: return@GoldButton
                    actions.run {
                        onCreated(api.createHero(name.trim(), selectedJob, appearance))
                        null
                    }
                },
                enabled = !actions.busy && job != null && name.trim().length >= 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
