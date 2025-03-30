package eu.gload.tpmtool.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.gload.tpmtool.R
import eu.gload.tpmtool.ui.theme.TpmToolTheme
import eu.gload.tpmtool.viewModel.MainViewModel
import eu.gload.tpmtool.viewModel.NavigationEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Layout() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigationEvent) {
        when (uiState.navigationEvent) {
            NavigationEvent.TO_ATTESTATION_RESULT -> {
                navController.navigate("attestation_result")
                viewModel.resetNavigation()
            }

            NavigationEvent.TO_MANAGE_DEVICE -> {
                navController.navigate("manage_device")
                viewModel.resetNavigation()
            }

            NavigationEvent.TO_MAIN -> {
                navController.popBackStack()
                viewModel.resetNavigation()
            }

            NavigationEvent.NONE -> {
                // No navigation needed
            }
        }
    }

    TpmToolTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(topBar = {
                TopAppBar(
                    title = { Text(text = context.getString(R.string.app_name)) },
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
            }, content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 400)) },
                        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 400)) },
                    ) {
                        composable("main") {
                            MainPage(viewModel)
                        }
                        composable("attestation_result") {
                            AttestationResultPage(viewModel)
                        }
                        composable("manage_device") {
                            ManageDevicePage(viewModel)
                        }
                    }

                    LoadingOverlay(isVisible = uiState.isLoading)
                    ErrorOverlay(uiState.errorMessage) {
                        viewModel.dismissError()
                    }
                }
            })
        }
    }
}