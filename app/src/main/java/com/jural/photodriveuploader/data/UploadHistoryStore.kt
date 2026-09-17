package com.jural.photodriveuploader.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val fileName: String,
    val driveFileId: String,
    val webViewLink: String?,
    val timestampMillis: Long,
    val folderName: String?
)

class UploadHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("photo_drive_uploader_prefs", Context.MODE_PRIVATE)

    fun getAll(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val array = JSONArray(raw)
        val result = mutableListOf<HistoryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                HistoryEntry(
                    fileName = obj.getString("fileName"),
                    driveFileId = obj.getString("driveFileId"),
                    webViewLink = obj.optString("webViewLink").ifBlank { null },
                    timestampMillis = obj.getLong("timestampMillis"),
                    folderName = obj.optString("folderName").ifBlank { null }
                )
            )
        }
        return result.sortedByDescending { it.timestampMillis }
    }

    fun add(entry: HistoryEntry) {
        val current = getAll().toMutableList()
        current.add(0, entry)
        val array = JSONArray()
        current.forEach { e ->
            val obj = JSONObject()
            obj.put("fileName", e.fileName)
            obj.put("driveFileId", e.driveFileId)
            obj.put("webViewLink", e.webViewLink ?: "")
            obj.put("timestampMillis", e.timestampMillis)
            obj.put("folderName", e.folderName ?: "")
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val KEY_HISTORY = "upload_history"
    }
}
