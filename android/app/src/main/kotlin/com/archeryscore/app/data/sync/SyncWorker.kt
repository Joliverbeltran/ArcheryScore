package com.archeryscore.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.archeryscore.app.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val runner: SyncRunner,
    private val authRepository: AuthRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: runCatching {
            authRepository.requireUserId()
        }.getOrNull() ?: return Result.success()
        if (userId.isBlank()) return Result.success()

        return withContext(Dispatchers.IO) {
            when (runner.syncForUser(userId)) {
                is SyncRunner.SyncResult.Full -> Result.success()
                is SyncRunner.SyncResult.Partial -> {
                    if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
                }
            }
        }
    }

    companion object {
        const val KEY_USER_ID = "user_id"
        const val UNIQUE_NAME = "session_sync"
        private const val MAX_ATTEMPTS = 10
    }
}

class SyncScheduler {
    fun enqueue(context: Context, userId: String) {
        val request = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                15,
                java.util.concurrent.TimeUnit.SECONDS,
            )
            .setInputData(androidx.work.Data.Builder().putString(SyncWorker.KEY_USER_ID, userId).build())
            .build()
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork(SyncWorker.UNIQUE_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request)
    }
}