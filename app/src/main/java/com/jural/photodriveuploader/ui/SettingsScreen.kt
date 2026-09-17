package com.jural.photodriveuploader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.jural.photodriveuploader.FoldersState
import com.jural.photodriveuploader.MainViewModel
import com.google.android.gms.common.api.Scope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val account by viewModel.account.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val foldersState by viewModel.foldersState.collectAsStateWithLifecycle()

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
    }
    val signInClient: GoogleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    var signInError by remember { mutableStateOf<String?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val acc = task.getResult(ApiException::class.java)
            viewModel.onSignedIn(acc)
            signInError = null
        } catch (e: ApiException) {
            signInError = "Ошибка входа (код ${e.statusCode}): ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)}"
        }
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Google-аккаунт", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (account == null) {
                Button(onClick = { signInLauncher.launch(signInClient.signInIntent) }) {
                    Text("Войти через Google")
                }
                signInError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it)
                }
            } else {
                Text("Вы вошли как: ${account?.email ?: account?.displayName ?: "неизвестно"}")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    signInClient.signOut()
                    viewModel.onSignedOut()
                }) {
                    Text("Выйти")
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text("Папка на Google Drive", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Текущая: ${selectedFolder?.name ?: "корень Google Drive (по умолчанию)"}")
            Spacer(Modifier.height(8.dp))

            if (account != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { viewModel.refreshFolders() }) {
                        Text("Обновить список")
                    }
                    OutlinedButton(onClick = { showCreateFolderDialog = true }) {
                        Text("Создать папку")
                    }
                }

                Spacer(Modifier.height(8.dp))

                when (val state = foldersState) {
                    is FoldersState.Loading -> Text("Загрузка папок…")
                    is FoldersState.Error -> Text("Ошибка: ${state.message}")
                    is FoldersState.Loaded -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                ListItem(
                                    headlineContent = { Text("Корень Google Drive (по умолчанию)") },
                                    leadingContent = {
                                        RadioButton(
                                            selected = selectedFolder == null,
                                            onClick = { viewModel.selectFolder(null) }
                                        )
                                    }
                                )
                            }
                            items(state.folders) { folder ->
                                ListItem(
                                    headlineContent = { Text(folder.name) },
                                    leadingContent = {
                                        RadioButton(
                                            selected = selectedFolder?.id == folder.id,
                                            onClick = { viewModel.selectFolder(folder) }
                                        )
                                    }
                                )
                            }
                        }
                    }
                    else -> {}
                }
            } else {
                Text("Войдите в аккаунт, чтобы выбрать папку")
            }
        }
    }

    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Новая папка") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Название папки") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName.trim())
                    }
                    showCreateFolderDialog = false
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Отмена") }
            }
        )
    }
}
