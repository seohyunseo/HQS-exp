package com.uvr.hqs_phone.export

import android.content.Context
import android.net.Uri
import android.util.Log
import com.uvr.hqs_phone.data.UserPreferences
import com.uvr.hqs_phone.data.db.LifelogDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExportManager {

    private const val TAG = "ExportManager"

    /**
     * Dumps the entire database to a CSV at the given SAF URI.
     * Call this after receiving a URI from ActivityResultContracts.CreateDocument.
     */
    suspend fun exportAll(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val dao = LifelogDatabase.getDatabase(context).lifelogDao()
        val rows = dao.getAll()

        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write("Date,Category,Name,Start Time,End Time,Duration(ms)\n")
            rows.forEach { row ->
                writer.write(
                    "${row.date},${row.category},\"${row.name}\"," +
                            "${row.startTime},${row.endTime},${row.duration}\n"
                )
            }
        }
        Log.d(TAG, "Exported ${rows.size} rows to $uri")
    }

    /** Returns a suggested filename: Lifelog_Final_{UUID}.csv */
    suspend fun suggestedFilename(context: Context): String {
        val uuid = UserPreferences.getUserUUID(context)
        return "Lifelog_Final_$uuid.csv"
    }
}
