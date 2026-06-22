package com.uvr.hqs_phone.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.uvr.hqs_phone.HQSApplication.Companion.TRACKING_CHANNEL_ID
import com.uvr.hqs_phone.HQSApplication.Companion.TRACKING_NOTIFICATION_ID
import com.uvr.hqs_phone.MainActivity
import com.uvr.hqs_phone.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DataCollectionService : LifecycleService() {

    companion object {
        private const val TAG = "DataCollectionService"
        var isRunning = false
    }

    private lateinit var physicalTracker: PhysicalTracker
    private lateinit var digitalTracker: DigitalTracker
    private lateinit var socialTracker: SocialCallTracker
    private var digitalJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        isRunning = true

        physicalTracker = PhysicalTracker(this)
        digitalTracker = DigitalTracker(this)
        socialTracker = SocialCallTracker(this)

        startForeground(TRACKING_NOTIFICATION_ID, buildNotification())
        physicalTracker.register()
        socialTracker.register()
        startDigitalTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        physicalTracker.unregister()
        socialTracker.unregister()
        digitalJob?.cancel()
        lifecycleScope.launch(Dispatchers.IO) {
            digitalTracker.flushOpenSessions()
        }
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private fun startDigitalTracking() {
        digitalJob = lifecycleScope.launch(Dispatchers.IO) {
            digitalTracker.runLoop()
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, TRACKING_CHANNEL_ID)
            .setContentTitle("HQS Lifelog Active")
            .setContentText("Tracking physical, digital & social activity")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun updateNotification(status: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, TRACKING_CHANNEL_ID)
            .setContentTitle("HQS Lifelog Active")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()
        nm.notify(TRACKING_NOTIFICATION_ID, notification)
    }
}
