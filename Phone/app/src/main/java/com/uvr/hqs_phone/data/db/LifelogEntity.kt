package com.uvr.hqs_phone.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lifelog")
data class LifelogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,           // "YYYY-MM-DD" in KST
    val category: String,       // "PHYSICAL" or "DIGITAL"
    val name: String,           // e.g. "WALKING" or "com.instagram.android"
    val startTime: Long,        // Epoch ms
    val endTime: Long = 0L,     // Epoch ms; 0 = still open
    val duration: Long = 0L,    // endTime - startTime in ms
    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false // false until successfully uploaded to Firebase
)
