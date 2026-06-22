package com.uvr.hqs_phone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.uvr.hqs_phone.sync.DailySyncWorker
import com.uvr.hqs_phone.util.KstTimeUtils
import java.util.concurrent.TimeUnit

class HQSApplication : Application() {

    companion object {
        const val TRACKING_CHANNEL_ID = "hqs_tracking_channel"
        const val TRACKING_NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleDailySyncWorker()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            TRACKING_CHANNEL_ID,
            "HQS Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification for the HQS data collection service"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun scheduleDailySyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialDelayMs = KstTimeUtils.millisUntilNextSyncTime()

        val request = PeriodicWorkRequestBuilder<DailySyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
