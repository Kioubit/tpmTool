package eu.gload.tpmtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.gload.tpmtool.ui.theme.TpmToolTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TpmToolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(topBar = {
                        TopAppBar(
                            title = { Text(text = getString(R.string.app_name)) },
                            colors = topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = Color.White
                                )
                        )
                    }, content = {
                        Column(modifier = Modifier.padding(it)) {
                            ShowPage()
                        }
                    })
                }
            }
        }
    }

    @Composable
    fun ShowPage(viewModel: MainViewModel = viewModel()) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        viewModel.refreshDeviceList()
        when (uiState.page) {
            Pages.Main -> MainPage()
            Pages.AddDevice -> AddDevicePage()
            Pages.AttestationResult -> AttestationResultPage()
            Pages.Loading -> LoadingPage()
        }
    }
}