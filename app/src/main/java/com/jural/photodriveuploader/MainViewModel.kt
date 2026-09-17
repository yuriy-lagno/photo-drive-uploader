package com.jural.photodriveuploader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.jural.photodriveuploader.data.AppPrefs
import com.jural.photodriveuploader.data.HistoryEntry
import com.jural.photodriveuploader.data.UploadHistoryStore
import com.jural.photodriveuploader.drive.DriveFolder
import com.jural.photodriveuploader.drive.DriveServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    data class Success(val link: String?) : UploadState()
    data class Error(val message: String) : UploadState()
}

sealed class FoldersState {
    object Idle : FoldersState()
    object Loading : FoldersState()
    data class Loaded(val folders: List<DriveFolder>) : FoldersState()
    data class Error(val message: String) : FoldersState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPrefs(application)
    private val historyStore = UploadHistoryStore(application)

    private val _account = MutableStateFlow<GoogleSignInAccount?>(null)
    val account: StateFlow<GoogleSignInAccount?> = _account.asStateFlow()

    private val _selectedFolder = MutableStateFlow(
        prefs.selectedFolderId?.let { id ->
            DriveFolder(id, prefs.selectedFolderName ?: "")
        }
    )
    val selectedFolder: StateFlow<DriveFolder?> = _selectedFolder.asStateFlow()

    private val _foldersState = MutableStateFlow<FoldersState>(FoldersState.Idle)
    val foldersState: StateFlow<FoldersState> = _foldersState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _history = MutableStateFlow(historyStore.getAll())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    var pendingPhotoFile: File? = null
        private set

    fun setPendingPhoto(file: File) {
        pendingPhotoFile = file
    }

    fun clearPendingPhoto() {
        pendingPhotoFile?.let { if (it.exists()) it.delete() }
        pendingPhotoFile = null
        _uploadState.value = UploadState.Idle
    }

    fun onSignedIn(account: GoogleSignInAccount) {
        _account.value = account
        refreshFolders()
    }

    fun onSignedOut() {
        _account.value = null
        _foldersState.value = FoldersState.Idle
    }

    private fun currentDriveHelper(): DriveServiceHelper? {
        val acc = _account.value ?: return null
        val androidAccount = acc.account ?: return null
        return DriveServiceHelper(getApplication(), androidAccount)
    }

    fun refreshFolders() {
        val helper = currentDriveHelper() ?: return
        _foldersState.value = FoldersState.Loading
        viewModelScope.launch {
            try {
                val folders = helper.listFolders()
                _foldersState.value = FoldersState.Loaded(folders)
            } catch (e: Exception) {
                _foldersState.value = FoldersState.Error(e.message ?: "Не удалось загрузить папки")
            }
        }
    }

    fun createFolder(name: String) {
        val helper = currentDriveHelper() ?: return
        _foldersState.value = FoldersState.Loading
        viewModelScope.launch {
            try {
                val folder = helper.createFolder(name)
                selectFolder(folder)
                refreshFolders()
            } catch (e: Exception) {
                _foldersState.value = FoldersState.Error(e.message ?: "Не удалось создать папку")
            }
        }
    }

    fun selectFolder(folder: DriveFolder?) {
        _selectedFolder.value = folder
        prefs.selectedFolderId = folder?.id
        prefs.selectedFolderName = folder?.name
    }

    fun uploadPendingPhoto() {
        val file = pendingPhotoFile ?: return
        val helper = currentDriveHelper() ?: run {
            _uploadState.value = UploadState.Error("Сначала войдите в аккаунт Google")
            return
        }
        _uploadState.value = UploadState.Uploading
        viewModelScope.launch {
            try {
                val folder = _selectedFolder.value
                val result = helper.uploadPhoto(file, folder?.id)
                _uploadState.value = UploadState.Success(result.webViewLink)
                historyStore.add(
                    HistoryEntry(
                        fileName = result.fileName,
                        driveFileId = result.fileId,
                        webViewLink = result.webViewLink,
                        timestampMillis = System.currentTimeMillis(),
                        folderName = folder?.name
                    )
                )
                _history.value = historyStore.getAll()
                file.delete()
                pendingPhotoFile = null
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
}
