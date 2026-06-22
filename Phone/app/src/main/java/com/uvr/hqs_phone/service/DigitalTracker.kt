package com.uvr.hqs_phone.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.uvr.hqs_phone.data.db.LifelogDatabase
import com.uvr.hqs_phone.data.db.LifelogEntity
import com.uvr.hqs_phone.util.KstTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class DigitalTracker(private val context: Context) {

    companion object {
        private const val TAG = "DigitalTracker"
        private const val POLL_INTERVAL_MS = 4_000L
        /** Minimum session length to record (filters sub-second noise). */
        private const val MIN_SESSION_MS = 1_000L
    }

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    // ── Stateful single-active-session tracking ───────────────────────────
    // Only ONE app can be in the foreground at a time. These two variables
    // represent that single foreground session, preventing duplicate rows
    // caused by orientation changes or internal activity transitions.
    private var currentActivePackage: String? = null
    private var currentSessionStartTime: Long = 0L

    private var lastPollTime = System.currentTimeMillis() - POLL_INTERVAL_MS
    private val homeLauncherPackage: String = resolveHomeLauncher()

    private fun resolveHomeLauncher(): String {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName ?: ""
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun shouldIgnorePackage(pkg: String): Boolean =
        pkg == homeLauncherPackage || pkg == context.packageName

    /** Runs as a continuous suspend loop; call from a CoroutineScope on Dispatchers.IO. */
    suspend fun runLoop() = withContext(Dispatchers.IO) {
        val dao = LifelogDatabase.getDatabase(context).lifelogDao()
        Log.d(TAG, "Digital tracker loop started")

        while (isActive) {
            val now = System.currentTimeMillis()
            try {
                val events = usageStatsManager.queryEvents(lastPollTime, now)
                val event = UsageEvents.Event()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    if (shouldIgnorePackage(pkg)) continue

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            if (pkg == currentActivePackage) {
                                // ── SAME app resumed (e.g. orientation change): IGNORE ──
                                Log.v(TAG, "Ignored re-resume for same package: $pkg")
                                continue
                            }

                            // ── DIFFERENT app came to foreground ──────────────────────
                            // First, finalize the previous session (if any)
                            val prevPkg = currentActivePackage
                            if (prevPkg != null && currentSessionStartTime > 0L) {
                                saveSession(dao, prevPkg, currentSessionStartTime, event.timeStamp)
                            }

                            // Start tracking the new foreground app
                            currentActivePackage = pkg
                            currentSessionStartTime = event.timeStamp
                            Log.d(TAG, "Started session: $pkg")
                        }

                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            if (pkg != currentActivePackage) {
                                // Stale event for a non-active package — ignore
                                continue
                            }

                            // Finalize the active session
                            saveSession(dao, pkg, currentSessionStartTime, event.timeStamp)

                            // Reset state
                            currentActivePackage = null
                            currentSessionStartTime = 0L
                            Log.d(TAG, "Ended session: $pkg")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in digital poll: ${e.message}")
            }

            lastPollTime = now
            delay(POLL_INTERVAL_MS)
        }
    }

    /** Saves a session to Room, with midnight splitting. Ignores sub-second noise. */
    private suspend fun saveSession(
        dao: com.uvr.hqs_phone.data.db.LifelogDao,
        pkg: String,
        startMs: Long,
        endMs: Long
    ) {
        if (endMs - startMs < MIN_SESSION_MS) return
        val label = getAppLabel(pkg)
        val segments = KstTimeUtils.splitAtMidnight(startMs, endMs)
        for ((date, start, end) in segments) {
            dao.insert(
                LifelogEntity(
                    date = date,
                    category = "DIGITAL",
                    name = label,
                    startTime = start,
                    endTime = end,
                    duration = end - start
                )
            )
        }
        Log.d(TAG, "Saved: $label  ${endMs - startMs}ms")
    }

    /** Flush the currently active session to DB on service stop. */
    suspend fun flushOpenSessions() = withContext(Dispatchers.IO) {
        val pkg = currentActivePackage ?: return@withContext
        if (currentSessionStartTime <= 0L) return@withContext
        val dao = LifelogDatabase.getDatabase(context).lifelogDao()
        saveSession(dao, pkg, currentSessionStartTime, System.currentTimeMillis())
        currentActivePackage = null
        currentSessionStartTime = 0L
    }
}
