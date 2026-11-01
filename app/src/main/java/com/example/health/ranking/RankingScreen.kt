package com.example.health.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.health.game.RankingEntry
import com.example.health.game.RankingInfo
import com.example.health.net.ApiClient
import com.example.health.ui.fantasy.ActionFeedback
import com.example.health.ui.fantasy.Panel
import com.example.health.ui.fantasy.ScreenHeader
import com.example.health.ui.fantasy.rememberActions
import com.example.health.ui.sprites.HeroAvatar
import com.example.health.ui.theme.Gold
import com.example.health.ui.theme.GoldBright
import com.example.health.ui.theme.GradeLegendary
import com.example.health.ui.theme.Parchment
import com.example.health.ui.theme.ParchmentDim

/** 랭킹: 가입한 모든 모험가 (레벨 × 100 + 전투력) */
@Composable
fun RankingScreen(api: ApiClient, onBack: () -> Unit) {
    var ranking by remember { mutableStateOf<RankingInfo?>(null) }
    val actions = rememberActions()

    LaunchedEffect(Unit) { actions.run { ranking = RankingInfo.from(api.ranking()); null } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScreenHeader("명예의 전당", onBack)
                ActionFeedback(actions)
                ranking?.let { r ->
                    Text("점수 = ${r.formula} · 전체 ${r.totalHeroes}명", color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
                    r.me?.let { me ->
                        Panel(title = "내 순위", highlight = true) { RankingRow(me, mine = true) }
                    }
                }
            }
        }
        items(ranking?.entries.orEmpty(), key = { it.heroId }) { entry ->
            Panel(highlight = entry.rank <= 3) { RankingRow(entry, mine = entry.heroId == ranking?.me?.heroId) }
        }
        item { Text("") }
    }
}

@Composable
private fun RankingRow(e: RankingEntry, mine: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            when (e.rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "${e.rank}"
            },
            color = if (e.rank <= 3) GradeLegendary else Parchment,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(36.dp),
        )
        HeroAvatar(e.job, e.appearance, 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(e.name + if (mine) " (나)" else "", color = if (mine) GoldBright else Gold, fontWeight = FontWeight.Bold)
            Text("Lv.${e.level} ${e.jobName} · 전투력 %,d".format(e.power), color = ParchmentDim, style = MaterialTheme.typography.bodySmall)
        }
        Text("%,d".format(e.score), color = GoldBright, fontWeight = FontWeight.Bold)
    }
}
