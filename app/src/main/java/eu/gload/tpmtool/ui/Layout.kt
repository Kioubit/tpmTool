package eu.gload.tpmtool.ui

import android.util.Log
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.gload.tpmtool.R
import eu.gload.tpmtool.ui.theme.TpmToolTheme
import eu.gload.tpmtool.viewModel.MainViewModel
import eu.gload.tpmtool.viewModel.NavigationEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Layout(vm: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vm, navController) {
        vm.navigationEvent.collect { event ->
            Log.i("Navigation", event.routeString.toString())
            when (event) {
                is NavigationEvent.To -> {
                    navController.navigate(event.routeString)
                }
                NavigationEvent.Back -> {
                    if (!navController.popBackStack()) {
                        navController.navigate(NavigationEvent.Routes.MAIN.routeString)
                    }
                }
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
                        startDestination = NavigationEvent.Routes.MAIN.routeString,
                        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 400)) },
                        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 400)) },
                    ) {
                        composable(NavigationEvent.Routes.MAIN.routeString) {
                            MainPage(vm)
                        }
                        composable(NavigationEvent.Routes.ATTESTATION_RESULT.routeString) {
                            AttestationResultPage(vm)
                        }
                        composable(NavigationEvent.Routes.MANAGE_DEVICE.routeString) {
                            ManageDevicePage(vm)
                        }
                    }

                    LoadingOverlay(isVisible = uiState.isLoading)
                    ErrorOverlay(uiState.errorMessage) {
                        vm.dismissError()
                    }
                }
            })
        }
    }
}