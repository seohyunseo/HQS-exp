package com.uvr.hqs_phone.service

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.util.Log
import com.uvr.hqs_phone.data.db.LifelogDatabase
import com.uvr.hqs_phone.data.db.LifelogEntity
import com.uvr.hqs_phone.util.KstTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Observes [CallLog.Calls.CONTENT_URI] and inserts SOCIAL entries into the
 * lifelog Room database whenever a qualifying call (non-zero duration, not missed
 * or rejected) is logged by the OS.
 *
 * Registration / unregistration is managed by [DataCollectionService].
 *
 * Requires runtime permissions: READ_CALL_LOG, READ_CONTACTS, READ_PHONE_STATE.
 * If any permission is absent the observer silently no-ops.
 */
class SocialCallTracker(private val context: Context) {

    companion object {
        private const val TAG = "SocialCallTracker"
        /** Number of most-recent rows to query when the observer fires. */
        private const val QUERY_LIMIT = 1
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            scope.launch { handleCallLogChange() }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    fun register() {
        try {
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                /* notifyForDescendants = */ true,
                observer
            )
            Log.d(TAG, "ContentObserver registered on CallLog")
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing READ_CALL_LOG permission — social tracking disabled")
        }
    }

    fun unregister() {
        context.contentResolver.unregisterContentObserver(observer)
        Log.d(TAG, "ContentObserver unregistered")
    }

    // ── Core logic ────────────────────────────────────────────────────────

    private suspend fun handleCallLogChange() {
        try {
            val (startTimeMs, durationMs, contactName, callType) = queryLatestCall() ?: return

            // Skip missed, rejected, or zero-duration calls
            if (durationMs <= 0L) {
                Log.d(TAG, "Skipping zero-duration call (missed/rejected)")
                return
            }
            if (callType == CallLog.Calls.MISSED_TYPE || callType == CallLog.Calls.REJECTED_TYPE) {
                Log.d(TAG, "Skipping missed/rejected call")
                return
            }

            val endTimeMs = startTimeMs + durationMs

            val segments = KstTimeUtils.splitAtMidnight(startTimeMs, endTimeMs)
            val dao = LifelogDatabase.getDatabase(context).lifelogDao()

            segments.forEach { (date, start, end) ->
                dao.insert(
                    LifelogEntity(
                        date = date,
                        category = "SOCIAL",
                        name = contactName,
                        startTime = start,
                        endTime = end,
                        duration = end - start
                    )
                )
                Log.d(TAG, "Inserted SOCIAL: $contactName  $date  ${(end - start) / 1000}s")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_CALL_LOG denied at query time — skipping")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing call log: ${e.message}", e)
        }
    }

    /**
     * Queries the single most-recent call log entry.
     * Returns null if no entry found or permission is denied.
     */
    private fun queryLatestCall(): CallEntry? {
        val projection = arrayOf(
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE
        )
        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null

                val dateMs = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val durationSec = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                val cachedName = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE))

                val resolvedName = when {
                    !cachedName.isNullOrBlank() -> cachedName.trim()
                    !number.isNullOrBlank() -> number.trim()
                    else -> "Unknown"
                }

                CallEntry(
                    startTimeMs = dateMs,
                    durationMs = durationSec * 1_000L,
                    contactName = resolvedName,
                    callType = type
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied when querying call log")
            null
        }
    }

    private data class CallEntry(
        val startTimeMs: Long,
        val durationMs: Long,
        val contactName: String,
        val callType: Int
    )
}
