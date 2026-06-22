package com.uvr.hqs_phone.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult

class ActivityTransitionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ActivityTransitionRcvr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            val type = event.activityType
            val transition = event.transitionType
            val timeMs = event.elapsedRealTimeNanos / 1_000_000 +
                    (System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime())

            Log.d(TAG, "Transition: type=$type transition=$transition")

            when (transition) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER ->
                    PhysicalTracker.handleEnterEvent(context, type, timeMs)
                ActivityTransition.ACTIVITY_TRANSITION_EXIT ->
                    PhysicalTracker.handleExitEvent(context, type, timeMs)
            }
        }
    }
}
