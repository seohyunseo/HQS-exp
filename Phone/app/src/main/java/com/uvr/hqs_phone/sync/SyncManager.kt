package com.uvr.hqs_phone.sync

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.uvr.hqs_phone.data.UserPreferences
import com.uvr.hqs_phone.data.db.LifelogDatabase
import com.uvr.hqs_phone.data.db.LifelogEntity
import com.uvr.hqs_phone.util.KstTimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Central sync logic shared between [DailySyncWorker] (automatic) and
 * the manual "Sync Now" button in SettingsScreen.
 */
object SyncManager {

    private const val TAG = "SyncManager"

    /**
     * Performs a full incremental sync cycle:
     * 1. Queries all unsynced records from Room.
     * 2. Groups them by date, uploads one CSV per date to Firebase Storage.
     * 3. On success, marks those records as synced in Room.
     * 4. Uploads a Firestore health-check document for today.
     *
     * @return number of records successfully synced, or -1 on failure.
     * @throws CancellationException when the caller cancels (e.g. withTimeout) —
     *   this is NOT swallowed so that the spinner always stops.
     */
    suspend fun sync(context: Context): Int {
        val dao = LifelogDatabase.getDatabase(context).lifelogDao()
        val rawId = UserPreferences.getParticipantId(context)
        val participantId = UserPreferences.formattedParticipantId(rawId)
        val today = KstTimeUtils.todayKstString()

        return try {
            // ── 1. Fetch all unsynced closed records ───────────────────────
            val unsynced = dao.getUnsynced()
            Log.d(TAG, "Unsynced records: ${unsynced.size}")

            // ── 2. Group by date and upload one CSV per date ───────────────
            val syncedIds = mutableListOf<Long>()
            val byDate: Map<String, List<LifelogEntity>> = unsynced.groupBy { it.date }

            for ((date, rows) in byDate) {
                val uploaded = uploadDateCsv(participantId, date, rows, context)
                if (uploaded) {
                    syncedIds.addAll(rows.map { it.id })
                    Log.d(TAG, "Uploaded $date (${rows.size} rows)")
                } else {
                    Log.w(TAG, "Upload failed for $date, will retry next sync")
                }
            }

            // ── 3. Bulk-mark successfully uploaded records as synced ───────
            if (syncedIds.isNotEmpty()) {
                dao.markSynced(syncedIds)
                Log.d(TAG, "Marked ${syncedIds.size} records as synced")
            }

            // ── 4. Firestore health check for today ────────────────────────
            uploadHealthCheck(participantId, today, dao)

            syncedIds.size

        } catch (e: CancellationException) {
            // Re-throw so withTimeout in MainViewModel can catch it and reset
            // the spinner. Swallowing CancellationException is a coroutines anti-pattern.
            Log.w(TAG, "Sync cancelled (timeout or manual cancel)")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            -1
        }
    }

    /**
     * Builds a CSV for [rows] and uploads it to Firebase Storage.
     * Returns false on any error so the caller can skip marking those IDs.
     */
    private suspend fun uploadDateCsv(
        participantId: String,
        date: String,
        rows: List<LifelogEntity>,
        context: Context
    ): Boolean {
        return try {
            val csvFile = File(context.cacheDir, "sync_${date}.csv")
            csvFile.bufferedWriter().use { writer ->
                writer.write("Date,Category,Name,Start Time,End Time,Duration(ms)\n")
                rows.forEach { row ->
                    writer.write(
                        "${row.date},${row.category},\"${row.name}\"," +
                                "${row.startTime},${row.endTime},${row.duration}\n"
                    )
                }
            }
            // backups/{P01}/Daily_Log_{date}.csv
            val ref = Firebase.storage.reference.child("backups/$participantId/Daily_Log_$date.csv")
            ref.putFile(android.net.Uri.fromFile(csvFile)).await()
            csvFile.delete()
            true
        } catch (e: CancellationException) {
            // Must re-throw so the timeout propagates up to the ViewModel
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "CSV upload failed for $date: ${e.message}")
            false
        }
    }

    private suspend fun uploadHealthCheck(
        participantId: String,
        date: String,
        dao: com.uvr.hqs_phone.data.db.LifelogDao
    ) {
        val rowCount = dao.countByDate(date)
        val lastActive = dao.lastActiveTimestamp(date)
        val unsyncedCount = dao.getUnsynced().size
        val metadata = mapOf(
            "participantId" to participantId,
            "date" to date,
            "totalRows" to rowCount,
            "unsyncedRows" to unsyncedCount,
            "lastActiveTimestamp" to (lastActive ?: 0L),
            "syncTimestamp" to System.currentTimeMillis()
        )
        // users/{P01}/health_checks/{date}
        Firebase.firestore
            .collection("users").document(participantId)
            .collection("health_checks").document(date)
            .set(metadata)
            .await()
        Log.d(TAG, "Health check uploaded for $participantId / $date")
    }
}
