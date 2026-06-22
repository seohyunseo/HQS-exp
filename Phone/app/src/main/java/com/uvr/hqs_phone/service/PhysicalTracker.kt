package com.uvr.hqs_phone.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.uvr.hqs_phone.data.db.LifelogDatabase
import com.uvr.hqs_phone.data.db.LifelogEntity
import com.uvr.hqs_phone.util.KstTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhysicalTracker(private val context: Context) {

    companion object {
        private const val TAG = "PhysicalTracker"

        val TRACKED_ACTIVITIES = listOf(
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.STILL
        )

        fun activityName(type: Int) = when (type) {
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.STILL -> "STILL"
            else -> "UNKNOWN"
        }

        /** Called from ActivityTransitionReceiver — handles DB writes outside service scope. */
        fun handleEnterEvent(context: Context, activityType: Int, eventTimeMs: Long) {
            if (activityType == DetectedActivity.STILL) {
                // STILL acts as a stop trigger — close all open sessions
                closeAllOpenSessions(context, eventTimeMs)
                return
            }
            val name = activityName(activityType)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val dao = LifelogDatabase.getDatabase(context).lifelogDao()
                // Close any previously open session for this activity first
                closeSession(dao, name, eventTimeMs)
                // Open new session
                val dateStr = KstTimeUtils.epochToKstDate(eventTimeMs)
                dao.insert(
                    LifelogEntity(
                        date = dateStr,
                        category = "PHYSICAL",
                        name = name,
                        startTime = eventTimeMs,
                        endTime = 0L,
                        duration = 0L
                    )
                )
                Log.d(TAG, "Opened session: $name at $eventTimeMs")
            }
        }

        fun handleExitEvent(context: Context, activityType: Int, eventTimeMs: Long) {
            if (activityType == DetectedActivity.STILL) return // not tracked
            val name = activityName(activityType)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val dao = LifelogDatabase.getDatabase(context).lifelogDao()
                closeSession(dao, name, eventTimeMs)
            }
        }

        private fun closeAllOpenSessions(context: Context, endTimeMs: Long) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val dao = LifelogDatabase.getDatabase(context).lifelogDao()
                listOf("WALKING", "RUNNING", "ON_BICYCLE", "IN_VEHICLE").forEach { name ->
                    closeSession(dao, name, endTimeMs)
                }
            }
        }

        private suspend fun closeSession(
            dao: com.uvr.hqs_phone.data.db.LifelogDao,
            name: String,
            endTimeMs: Long
        ) {
            val open = dao.getOpenPhysicalSession(name) ?: return
            val segments = KstTimeUtils.splitAtMidnight(open.startTime, endTimeMs)
            segments.forEachIndexed { index, (date, start, end) ->
                val duration = end - start
                if (index == 0) {
                    dao.update(open.copy(date = date, endTime = end, duration = duration))
                } else {
                    dao.insert(
                        LifelogEntity(
                            date = date,
                            category = "PHYSICAL",
                            name = name,
                            startTime = start,
                            endTime = end,
                            duration = duration
                        )
                    )
                }
            }
            Log.d(TAG, "Closed session: $name, segments=${segments.size}")
        }
    }

    private val client = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        PendingIntent.getBroadcast(
            context, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun register() {
        val transitions = TRACKED_ACTIVITIES.flatMap { activity ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }
        val request = ActivityTransitionRequest(transitions)
        client.requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Activity transition updates registered") }
            .addOnFailureListener { Log.e(TAG, "Failed to register transitions: ${it.message}") }
    }

    fun unregister() {
        client.removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Activity transition updates removed") }
    }
}
