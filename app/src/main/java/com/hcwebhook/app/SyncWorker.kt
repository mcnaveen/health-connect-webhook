package com.hcwebhook.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val syncManager = SyncManager(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val syncResult = syncManager.performSync()
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                mapFailure(syncResult.exceptionOrNull())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            mapFailure(e)
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun mapFailure(error: Throwable?): Result {
        return when (error) {
            is CancellationException -> throw error
            is IOException -> if (WebhookManager.isRetryableException(error)) Result.retry() else Result.failure()
            else -> Result.failure()
        }
    }
}
