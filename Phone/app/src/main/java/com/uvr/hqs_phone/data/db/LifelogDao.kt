package com.uvr.hqs_phone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DurationSummary(
    val category: String,
    val name: String,
    val totalDuration: Long
)

@Dao
interface LifelogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LifelogEntity): Long

    @Update
    suspend fun update(entity: LifelogEntity)

    @Query("SELECT * FROM lifelog WHERE date = :date ORDER BY startTime DESC")
    fun getByDateFlow(date: String): Flow<List<LifelogEntity>>

    @Query("SELECT * FROM lifelog WHERE date = :date AND category = 'SOCIAL' ORDER BY startTime DESC")
    fun getSocialByDateFlow(date: String): Flow<List<LifelogEntity>>

    @Query("SELECT * FROM lifelog WHERE date = :date ORDER BY startTime DESC")
    suspend fun getByDate(date: String): List<LifelogEntity>

    @Query("SELECT * FROM lifelog ORDER BY startTime DESC")
    suspend fun getAll(): List<LifelogEntity>

    @Query("""
        SELECT category, name, SUM(duration) AS totalDuration
        FROM lifelog
        WHERE date = :date AND duration > 0
        GROUP BY category, name
        ORDER BY totalDuration DESC
    """)
    fun getSummaryByDateFlow(date: String): Flow<List<DurationSummary>>

    /** Finds the last open physical session (endTime=0) for a given activity name. */
    @Query("""
        SELECT * FROM lifelog
        WHERE category = 'PHYSICAL' AND name = :name AND endTime = 0
        ORDER BY startTime DESC LIMIT 1
    """)
    suspend fun getOpenPhysicalSession(name: String): LifelogEntity?

    /** Count of all rows — used for health-check metadata. */
    @Query("SELECT COUNT(*) FROM lifelog WHERE date = :date")
    suspend fun countByDate(date: String): Int

    @Query("SELECT MAX(startTime) FROM lifelog WHERE date = :date")
    suspend fun lastActiveTimestamp(date: String): Long?

    // ── Incremental Sync Queries ──────────────────────────────────────────

    /** All records that have not yet been uploaded to Firebase. */
    @Query("SELECT * FROM lifelog WHERE isSynced = 0 AND duration > 0 ORDER BY date ASC, startTime ASC")
    suspend fun getUnsynced(): List<LifelogEntity>

    /** Count of unsynced records — used for the Settings UI badge. */
    @Query("SELECT COUNT(*) FROM lifelog WHERE isSynced = 0 AND duration > 0")
    fun getUnsyncedCountFlow(): Flow<Int>

    /** Mark a specific set of record IDs as synced after successful upload. */
    @Query("UPDATE lifelog SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
