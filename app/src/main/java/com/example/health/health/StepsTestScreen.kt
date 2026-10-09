package com.example.health.health

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch

private sealed interface StepsUiState {
    data object Loading : StepsUiState
    data object NotInstalled : StepsUiState
    data object UpdateRequired : StepsUiState
    data object NeedPermission : StepsUiState
    data class Loaded(val today: TodaySteps, val week: List<DailySteps>) : StepsUiState
    data class Error(val message: String) : StepsUiState
}

/** 삼성헬스(Health Connect) 걸음수 읽기 테스트 화면 */
@Composable
fun StepsTestScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reader = remember { StepsReader(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<StepsUiState>(StepsUiState.Loading) }

    suspend fun refresh() {
        state = StepsUiState.Loading
        state = when (reader.sdkStatus()) {
            HealthConnectClient.SDK_UNAVAILABLE -> StepsUiState.NotInstalled
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> StepsUiState.UpdateRequired
            else -> try {
                if (!reader.hasAllPermissions()) {
                    StepsUiState.NeedPermission
                } else {
                    StepsUiState.Loaded(reader.readToday(), reader.readDaily(7))
                }
            } catch (e: Exception) {
                StepsUiState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { scope.launch { refresh() } }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("걸음수 읽기 테스트", style = MaterialTheme.typography.headlineSmall)

        when (val s = state) {
            StepsUiState.Loading -> Text("불러오는 중...")

            StepsUiState.NotInstalled -> Text("이 기기에서는 Health Connect 를 사용할 수 없습니다.")

            StepsUiState.UpdateRequired -> {
                Text("Health Connect 설치/업데이트가 필요합니다.")
                Button(onClick = {
                    val uri = Uri.parse(
                        "market://details?id=${StepsReader.HEALTH_CONNECT_PACKAGE}&url=healthconnect%3A%2F%2Fonboarding",
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, uri)
                            .setPackage("com.android.vending")
                            .putExtra("overlay", true)
                            .putExtra("callerId", context.packageName),
                    )
                }) { Text("Play 스토어 열기") }
            }

            StepsUiState.NeedPermission -> {
                Text("걸음수 읽기 권한이 필요합니다.")
                Button(onClick = { permissionLauncher.launch(reader.permissions + reader.writePermissions) }) {
                    Text("권한 요청")
                }
            }

            is StepsUiState.Error -> {
                Text("오류: ${s.message}", color = MaterialTheme.colorScheme.error)
            }

            is StepsUiState.Loaded -> {
                LoadedContent(s)
                TestDataButton(
                    onClick = {
                        scope.launch {
                            try {
                                if (reader.hasWritePermission()) {
                                    reader.insertTestSteps()
                                    refresh()
                                } else {
                                    permissionLauncher.launch(reader.writePermissions)
                                }
                            } catch (e: Exception) {
                                state = StepsUiState.Error(e.message ?: e.javaClass.simpleName)
                            }
                        }
                    },
                )
            }
        }

        OutlinedButton(onClick = { scope.launch { refresh() } }) { Text("새로고침") }
    }
}

/** 개발용: 삼성헬스 연동 없이 읽기 코드를 검증하기 위해 Health Connect 에 직접 걸음수를 기록 */
@Composable
private fun TestDataButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text("[테스트] 걸음수 500 기록") }
    Text(
        "누르면 이 앱 이름으로 500걸음이 기록되고, 오늘 걸음수와 데이터 출처에 바로 반영되어야 합니다.",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun LoadedContent(s: StepsUiState.Loaded) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("오늘 (${s.today.date})", style = MaterialTheme.typography.titleMedium)
            Text("%,d 걸음".format(s.today.total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("그중 삼성헬스 출처: %,d 걸음".format(s.today.samsungHealth))
            Text(
                "데이터 출처: " + s.today.sources.ifEmpty { listOf("없음") }.joinToString(),
                style = MaterialTheme.typography.bodySmall,
            )
            if (StepsReader.SAMSUNG_HEALTH_PACKAGE !in s.today.sources) {
                Text(
                    "삼성헬스 데이터가 보이지 않으면: 삼성헬스 → 설정 → Health Connect 에서 걸음수 동기화를 켜주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("최근 7일", style = MaterialTheme.typography.titleMedium)
            s.week.asReversed().forEachIndexed { i, day ->
                if (i > 0) HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(day.date.toString())
                    Text("%,d".format(day.steps))
                }
            }
            Text("합계 %,d 걸음".format(s.week.sumOf { it.steps }), fontWeight = FontWeight.Bold)
        }
    }
}
