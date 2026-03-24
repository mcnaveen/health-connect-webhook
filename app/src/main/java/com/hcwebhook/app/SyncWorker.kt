package com.hcwebhook.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val syncManager = SyncManager(appContext)
    private val writeBackManager = WriteBackManager(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val syncResult = syncManager.performSync()

            // Run write-back after read sync (best-effort, doesn't affect sync result)
            try {
                writeBackManager.processPendingWrites()
            } catch (_: Exception) { }

            when {
                syncResult.isSuccess -> Result.success()
                syncResult.isFailure -> Result.failure()
                else -> Result.success() // No data case
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}