package com.example.health.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.health.auth.AuthScreen
import com.example.health.bag.BagScreen
import com.example.health.friend.FriendsScreen
import com.example.health.game.HeroInfo
import com.example.health.guild.GuildScreen
import com.example.health.hero.CreateHeroScreen
import com.example.health.lobby.LobbyScreen
import com.example.health.net.ApiClient
import com.example.health.net.ApiException
import com.example.health.net.Session
import com.example.health.raid.RaidScreen
import com.example.health.settings.SettingsScreen
import com.example.health.smith.SmithScreen
import com.example.health.ui.fantasy.FantasyBackground
import com.example.health.ui.fantasy.GoldButton
import com.example.health.ui.theme.Crimson
import com.example.health.ui.theme.Gold
import com.example.health.world.WorldScreen
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class Screen { LOADING, AUTH, CREATE_HERO, LOBBY, WORLD, GUILD, RAID, SMITH, BAG, FRIENDS, SETTINGS }

/** 화면 이동: 로그인 → (영웅 없으면) 영웅 생성 → 로비 → 각 메뉴 */
@Composable
fun HealthRpgApp(resumeKey: Int) {
    val context = LocalContext.current
    val session = remember { Session(context.applicationContext) }
    val api = remember { ApiClient(session) }

    var screen by rememberSaveable { mutableStateOf(if (session.token == null) Screen.AUTH else Screen.LOADING) }
    var hero by remember {
        mutableStateOf(session.cachedHeroJson?.let { runCatching { HeroInfo.from(JSONObject(it)) }.getOrNull() })
    }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    /** 서버 응답의 영웅 JSON 으로 상태와 캐시를 함께 갱신 */
    val onHeroJson: (JSONObject) -> Unit = { json ->
        session.cachedHeroJson = json.toString()
        hero = HeroInfo.from(json)
    }

    fun logout() {
        session.logout()
        hero = null
        screen = Screen.AUTH
    }

    LaunchedEffect(screen, reloadKey) {
        if (screen != Screen.LOADING) return@LaunchedEffect
        loadError = null
        try {
            onHeroJson(api.hero())
            screen = Screen.LOBBY
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            when {
                e.heroNotFound -> screen = Screen.CREATE_HERO
                e.unauthorized -> logout()
                else -> loadError = e.message
            }
        } catch (e: Exception) {
            // 서버에 연결 못 해도 저장된 영웅이 있으면 로비는 보여줌
            if (hero != null) screen = Screen.LOBBY else loadError = "서버에 연결하지 못했습니다: ${e.message}"
        }
    }

    val inMenu = screen !in setOf(Screen.LOADING, Screen.AUTH, Screen.CREATE_HERO, Screen.LOBBY)
    BackHandler(enabled = inMenu) { screen = Screen.LOBBY }
    val back: () -> Unit = { screen = Screen.LOBBY }

    FantasyBackground {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            val current = hero
            when {
                screen == Screen.AUTH -> AuthScreen(session, api) { screen = Screen.LOADING }
                screen == Screen.CREATE_HERO -> CreateHeroScreen(api, onCreated = { json ->
                    onHeroJson(json)
                    screen = Screen.LOBBY
                }, onLogout = ::logout)
                screen == Screen.LOADING || current == null -> LoadingView(loadError) { reloadKey++ }
                screen == Screen.LOBBY -> LobbyScreen(current, session, resumeKey, onHeroJson) { screen = it }
                screen == Screen.WORLD -> WorldScreen(api, current, onHeroJson, back)
                screen == Screen.GUILD -> GuildScreen(api, current, onHeroJson, back)
                screen == Screen.RAID -> RaidScreen(api, current, onHeroJson, back)
                screen == Screen.SMITH -> SmithScreen(api, current, onHeroJson, back)
                screen == Screen.BAG -> BagScreen(api, current, onHeroJson, back)
                screen == Screen.FRIENDS -> FriendsScreen(api, current, back)
                screen == Screen.SETTINGS -> SettingsScreen(session, api, onHeroJson, onLogout = ::logout, onBack = back)
            }
        }
    }
}

@Composable
private fun LoadingView(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error == null) {
            Text("⚔️ 모험을 준비하는 중...", color = Gold)
        } else {
            Text(error, color = Crimson)
            GoldButton("다시 시도", onRetry)
        }
    }
}
