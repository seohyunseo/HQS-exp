package com.uvr.hqs_phone.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailySyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Daily sync worker started")
        val synced = SyncManager.sync(applicationContext)
        return if (synced >= 0) {
            Log.d(TAG, "Daily sync complete: $synced records uploaded")
            Result.success()
        } else {
            Log.w(TAG, "Daily sync failed, scheduling retry")
            Result.retry()
        }
    }
}
