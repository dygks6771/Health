package com.example.health.friend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.health.game.FriendEntry
import com.example.health.game.FriendsInfo
import com.example.health.game.HeroInfo
import com.example.health.game.Portraits
import com.example.health.net.ApiClient
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.GhostButton
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.sprites.HeroAvatar
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim

/** 친구: 영웅 이름으로 요청·수락. 레이드에 친구 4명이 필요 */
@Composable
fun FriendsScreen(api: ApiClient, hero: HeroInfo, onBack: () -> Unit) {
    var info by remember { mutableStateOf<FriendsInfo?>(null) }
    var name by remember { mutableStateOf("") }
    val actions = rememberActions()

    LaunchedEffect(Unit) { actions.run { info = FriendsInfo.from(api.friends()); null } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("친구", onBack)
        Text("내 영웅 이름: ${hero.name} — 친구에게 알려 주세요", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)

        Panel(title = "친구 요청 보내기") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("친구의 영웅 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            GoldButton(
                "요청 보내기",
                onClick = {
                    actions.run {
                        info = FriendsInfo.from(api.addFriend(name.trim()))
                        val target = name.trim()
                        name = ""
                        "'$target' 에게 요청을 보냈습니다"
                    }
                },
                enabled = !actions.busy && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            ActionFeedback(actions)
        }

        val f = info ?: return@Column
        if (f.incoming.isNotEmpty()) {
            Panel(title = "받은 요청 (${f.incoming.size})", highlight = true) {
                f.incoming.forEach { e ->
                    FriendRow(e) {
                        GhostButton("수락", onClick = { actions.run { info = FriendsInfo.from(api.acceptFriend(e.friendshipId)); null } }, enabled = !actions.busy)
                        GhostButton("거절", onClick = { actions.run { info = FriendsInfo.from(api.removeFriend(e.friendshipId)); null } }, enabled = !actions.busy)
                    }
                }
            }
        }

        Panel(title = "친구 (${f.friends.size})") {
            if (f.friends.isEmpty()) Text("아직 친구가 없습니다", color = ParchmentDim)
            f.friends.forEach { e ->
                FriendRow(e) {
                    GhostButton("삭제", onClick = { actions.run { info = FriendsInfo.from(api.removeFriend(e.friendshipId)); null } }, enabled = !actions.busy)
                }
            }
        }

        if (f.outgoing.isNotEmpty()) {
            Panel(title = "보낸 요청 (${f.outgoing.size})") {
                f.outgoing.forEach { e ->
                    FriendRow(e) {
                        GhostButton("취소", onClick = { actions.run { info = FriendsInfo.from(api.removeFriend(e.friendshipId)); null } }, enabled = !actions.busy)
                    }
                }
            }
        }

        Panel(title = "🛠 개발용") {
            Text("레이드 테스트용: 내 레벨의 용병 친구를 4명까지 만들어 줍니다 (서버 개발 모드에서만)", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
            GhostButton(
                "테스트 친구 만들기",
                onClick = { actions.run { info = FriendsInfo.from(api.devFriendBots()); "용병 친구를 모았습니다" } },
                enabled = !actions.busy,
            )
        }
    }
}

@Composable
private fun FriendRow(entry: FriendEntry, actions: @Composable () -> Unit) {
    val h = entry.hero
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeroAvatar(h.job, h.appearance, 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(h.name, color = Gold)
            Text("Lv.${h.level} ${Portraits.job(h.job)} ${h.jobName} · 전투력 ${h.power}", color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
        actions()
    }
}
