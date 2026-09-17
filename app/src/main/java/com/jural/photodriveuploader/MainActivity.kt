package com.jural.photodriveuploader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jural.photodriveuploader.ui.CameraScreen
import com.jural.photodriveuploader.ui.HistoryScreen
import com.jural.photodriveuploader.ui.PreviewScreen
import com.jural.photodriveuploader.ui.SettingsScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GoogleSignIn.getLastSignedInAccount(this)?.let { account ->
            viewModel.onSignedIn(account)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(viewModel: MainViewModel) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "camera") {
        composable("camera") {
            CameraScreen(
                onPhotoCaptured = { file ->
                    viewModel.setPendingPhoto(file)
                    navController.navigate("preview")
                },
                onOpenSettings = { navController.navigate("settings") },
                onOpenHistory = { navController.navigate("history") }
            )
        }
        composable("preview") {
            val photoFile = viewModel.pendingPhotoFile
            if (photoFile != null) {
                PreviewScreen(
                    viewModel = viewModel,
                    photoFile = photoFile,
                    onRetake = {
                        viewModel.clearPendingPhoto()
                        navController.popBackStack()
                    },
                    onDone = {
                        viewModel.clearPendingPhoto()
                        navController.popBackStack("camera", inclusive = false)
                    },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
