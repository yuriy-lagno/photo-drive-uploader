package com.jural.photodriveuploader.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jural.photodriveuploader.MainViewModel
import com.jural.photodriveuploader.UploadState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: MainViewModel,
    photoFile: File,
    onRetake: () -> Unit,
    onDone: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()

    val bitmap = remember(photoFile) {
        BitmapFactory.decodeFile(photoFile.absolutePath)
    }

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            onDone()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Предпросмотр") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Снимок",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    "Не удалось загрузить изображение",
                    modifier = Modifier.padding(16.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (account == null) {
                        "Вы не вошли в Google-аккаунт — сначала откройте настройки"
                    } else {
                        "Папка назначения: ${selectedFolder?.name ?: "корень Google Drive"}"
                    }
                )

                Spacer(Modifier.height(8.dp))

                when (val state = uploadState) {
                    is UploadState.Uploading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                            Spacer(Modifier.height(0.dp))
                            Text("  Загрузка на Google Drive…")
                        }
                    }
                    is UploadState.Error -> {
                        Text("Ошибка: ${state.message}")
                    }
                    else -> {}
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        enabled = uploadState !is UploadState.Uploading
                    ) {
                        Text("Переснять")
                    }

                    if (account == null) {
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Войти")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.uploadPendingPhoto() },
                            modifier = Modifier.weight(1f),
                            enabled = uploadState !is UploadState.Uploading
                        ) {
                            Text("Загрузить")
                        }
                    }
                }
            }
        }
    }
}
