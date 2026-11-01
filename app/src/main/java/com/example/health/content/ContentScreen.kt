package com.example.health.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.health.game.ContentInfo
import com.example.health.game.ContentResult
import com.example.health.game.DifficultyInfo
import com.example.health.health.RunReader
import com.example.health.sync.RunSyncer
import com.example.health.sync.ServerApi
import com.example.health.sync.SyncSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 토벌(골드) / 던전(강화석) / 레이드(장신구). 각 1~20단계 */
@Composable
fun ContentScreen(resumeKey: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { SyncSettings(context.applicationContext) }
    val syncer = remember { RunSyncer(RunReader(context.applicationContext), settings) }
    val scope = rememberCoroutineScope()

    var contents by remember { mutableStateOf<List<ContentInfo>>(emptyList()) }
    var selected by remember { mutableIntStateOf(0) }
    var character by remember { mutableStateOf(syncer.cachedCharacter()) }
    var result by remember { mutableStateOf<ContentResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        val userId = settings.userId
        if (userId == null) {
            error = "캐릭터 탭에서 먼저 동기화해 주세요."
            return
        }
        busy = true
        try {
            contents = ContentInfo.listFrom(ServerApi(settings.serverUrl).content(userId))
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "서버에 연결하지 못했습니다: ${e.message ?: e.javaClass.simpleName}"
        } finally {
            busy = false
        }
    }

    fun attempt(type: String, difficulty: Int) {
        val userId = settings.userId ?: return
        busy = true
        error = null
        scope.launch {
            try {
                val json = ServerApi(settings.serverUrl).attemptContent(userId, type, difficulty)
                val r = ContentResult.from(json)
                syncer.save(json.getJSONObject("character"))
                character = r.character
                result = r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = false
            }
            load()
        }
    }

    LaunchedEffect(resumeKey) {
        character = syncer.cachedCharacter()
        load()
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                character?.let {
                    Text(
                        "Lv.${it.level} · ⚡ 행동력 ${it.stamina}/${it.maxStamina} · 무기 +${it.weapon.enhance} [${it.weapon.gradeName}]",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contents.forEachIndexed { i, content ->
                        if (i == selected) {
                            Button(onClick = {}) { Text(content.name) }
                        } else {
                            OutlinedButton(onClick = { selected = i }) { Text(content.name) }
                        }
                    }
                }
                result?.let { ResultBanner(it) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy && contents.isEmpty()) Text("불러오는 중...")
            }
        }

        val content = contents.getOrNull(selected)
        if (content != null) {
            item {
                Text(
                    "${content.name} · 입장 시 행동력 ${content.staminaCost} · 보상: ${rewardKind(content.type)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items(content.difficulties, key = { "${content.type}-${it.difficulty}" }) { d ->
                DifficultyCard(content, d, enabled = !busy) { attempt(content.type, d.difficulty) }
            }
            item { Text("") }
        }
    }
}

@Composable
private fun DifficultyCard(content: ContentInfo, d: DifficultyInfo, enabled: Boolean, onAttempt: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${content.name} ${d.difficulty}단계", fontWeight = FontWeight.Bold)
                    Text("보상: ${d.reward}", style = MaterialTheme.typography.bodySmall)
                }
                Button(enabled = enabled && d.available, onClick = onAttempt) { Text("도전 ⚡${content.staminaCost}") }
            }
            Text(
                "조건: Lv.${d.minLevel} · ${d.minGradeName} 무기 · " + d.minStats.joinToString(" · ") { "${it.first} ${it.second}" },
                style = MaterialTheme.typography.bodySmall,
            )
            if (!d.available) {
                Text(
                    "부족: " + d.reasons.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ResultBanner(r: ContentResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🏆 ${r.content} 클리어!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("보상: ${r.rewardText}")
        }
    }
}

private fun rewardKind(type: String): String = when (type) {
    "HUNT" -> "골드"
    "DUNGEON" -> "무기 강화석"
    "RAID" -> "장신구"
    else -> "-"
}
