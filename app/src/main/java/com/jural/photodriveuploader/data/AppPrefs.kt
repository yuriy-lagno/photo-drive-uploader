package com.jural.photodriveuploader.data

import android.content.Context

class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("photo_drive_uploader_prefs", Context.MODE_PRIVATE)

    var selectedFolderId: String?
        get() = prefs.getString(KEY_FOLDER_ID, null)
        set(value) = prefs.edit().putString(KEY_FOLDER_ID, value).apply()

    var selectedFolderName: String?
        get() = prefs.getString(KEY_FOLDER_NAME, null)
        set(value) = prefs.edit().putString(KEY_FOLDER_NAME, value).apply()

    fun clearFolder() {
        prefs.edit().remove(KEY_FOLDER_ID).remove(KEY_FOLDER_NAME).apply()
    }

    companion object {
        private const val KEY_FOLDER_ID = "selected_folder_id"
        private const val KEY_FOLDER_NAME = "selected_folder_name"
    }
}
