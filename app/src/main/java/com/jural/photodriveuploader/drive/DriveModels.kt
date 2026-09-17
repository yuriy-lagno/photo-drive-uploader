package com.jural.photodriveuploader.drive

data class DriveFolder(
    val id: String,
    val name: String
)

data class UploadResult(
    val fileId: String,
    val fileName: String,
    val webViewLink: String?
)
