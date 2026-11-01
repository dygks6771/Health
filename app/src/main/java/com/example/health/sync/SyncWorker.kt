package com.example.health.sync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.health.health.RunReader
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 앱을 열지 않아도 1시간마다 러닝 기록을 서버로 동기화 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reader = RunReader(applicationContext)
        if (reader.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return Result.success()

        return try {
            if (!reader.hasAllPermissions()) return Result.success()
            RunSyncer(reader, SyncSettings(applicationContext)).sync()
            Result.success()
        } catch (e: SecurityException) {
            // 백그라운드 읽기 권한이 없거나 지원하지 않는 기기: 앱을 열 때 동기화됨
            Result.success()
        } catch (e: IOException) {
            // 서버 꺼짐, 네트워크 끊김: 잠시 후 다시 시도
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "run-sync"

        /** 이전 버전(걸음수 동기화)에서 예약한 작업. 클래스 이름이 바뀌어 실행되지 않으므로 취소 */
        private const val OLD_WORK_NAME = "steps-sync"

        /** 이미 예약돼 있으면 그대로 둔다 (앱을 열 때마다 호출해도 됨) */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(OLD_WORK_NAME)
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
