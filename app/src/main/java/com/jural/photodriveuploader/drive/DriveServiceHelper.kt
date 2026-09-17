package com.jural.photodriveuploader.drive

import android.accounts.Account
import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as JavaFile

private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"

class DriveServiceHelper(context: Context, account: Account) {

    private val drive: Drive = run {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            setOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Photo Drive Uploader")
            .build()
    }

    suspend fun listFolders(): List<DriveFolder> = withContext(Dispatchers.IO) {
        val result = drive.files().list()
            .setQ("mimeType='$FOLDER_MIME_TYPE' and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .setOrderBy("name")
            .execute()
        result.files.orEmpty().map { DriveFolder(it.id, it.name) }
    }

    suspend fun createFolder(name: String): DriveFolder = withContext(Dispatchers.IO) {
        val metadata = DriveFile().apply {
            this.name = name
            this.mimeType = FOLDER_MIME_TYPE
        }
        val created = drive.files().create(metadata)
            .setFields("id, name")
            .execute()
        DriveFolder(created.id, created.name)
    }

    suspend fun uploadPhoto(file: JavaFile, folderId: String?): UploadResult = withContext(Dispatchers.IO) {
        val metadata = DriveFile().apply {
            name = file.name
            if (!folderId.isNullOrBlank()) {
                parents = listOf(folderId)
            }
        }
        val mediaContent = FileContent("image/jpeg", file)
        val uploaded = drive.files().create(metadata, mediaContent)
            .setFields("id, name, webViewLink")
            .execute()
        UploadResult(uploaded.id, uploaded.name, uploaded.webViewLink)
    }
}
