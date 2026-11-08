package com.example.health.sync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.health.game.WeeklyInfo
import com.example.health.health.RunReader
import com.example.health.net.ApiClient
import com.example.health.net.ApiException
import com.example.health.net.Session
import com.example.health.notify.Notifier
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 앱을 열지 않아도 러닝·걸음 기록을 서버로 자동 동기화.
 * - 15분마다 (안드로이드가 허용하는 최소 주기. 배터리 절약 모드에서는 더 늦어질 수 있음)
 * - 동기화가 네트워크·서버 문제로 실패하면, 인터넷이 연결되는 대로 다시 시도 ([syncSoon])
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reader = RunReader(applicationContext)
        if (reader.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return Result.success()

        val session = Session(applicationContext)
        if (session.token == null) return Result.success() // 로그인 전

        return try {
            if (!reader.hasAllPermissions()) return Result.success()
            val api = ApiClient(session)
            val result = RunSyncer(reader, session, api).sync()
            // 알림은 덤: 실패해도 동기화는 성공으로 처리
            runCatching {
                val weekly = WeeklyInfo.from(api.weekly())
                Notifier.afterSync(applicationContext, session, result.reward.takeIf { it.newRuns > 0 }, weekly)
            }
            Result.success()
        } catch (e: SecurityException) {
            // 백그라운드 읽기 권한이 없거나 지원하지 않는 기기: 앱을 열 때 동기화됨
            Result.success()
        } catch (e: ApiException) {
            // 로그인 만료·영웅 없음 등: 재시도해도 같으므로 앱을 열 때 처리
            Result.success()
        } catch (e: IOException) {
            // 서버 꺼짐, 네트워크 끊김: 잠시 후 다시 시도
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "run-sync"
        private const val SOON_WORK_NAME = "run-sync-soon"
        private const val INTERVAL_MINUTES = 15L

        /** 이전 버전(걸음수 동기화)에서 예약한 작업. 클래스 이름이 바뀌어 실행되지 않으므로 취소 */
        private const val OLD_WORK_NAME = "steps-sync"

        private val NETWORK = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** 주기 동기화 예약 (앱을 열 때마다 호출해도 됨. 주기가 바뀌었으면 새 주기로 갱신) */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(NETWORK)
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(OLD_WORK_NAME)
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /**
         * 인터넷이 연결되는 즉시 한 번 동기화 (지금 서버에 닿지 않을 때 호출).
         * 실패하면 WorkManager 가 간격을 늘려가며 다시 시도한다.
         */
        fun syncSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(NETWORK)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(SOON_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
