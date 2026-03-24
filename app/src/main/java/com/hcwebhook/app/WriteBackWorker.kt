package com.hcwebhook.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WriteBackWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val writeBackManager = WriteBackManager(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val writeResult = writeBackManager.processPendingWrites()
            if (writeResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
