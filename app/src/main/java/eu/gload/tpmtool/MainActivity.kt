package eu.gload.tpmtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.gload.tpmtool.data.DeviceRepository
import eu.gload.tpmtool.data.database.AppDatabase
import eu.gload.tpmtool.domain.usecase.AttestationUseCase
import eu.gload.tpmtool.domain.usecase.ManageDevicesUseCase
import eu.gload.tpmtool.ui.Layout
import eu.gload.tpmtool.viewModel.MainViewModel

class MainActivity : ComponentActivity() {
    // Database and repository setup using lazy delegation
    // (no need to initialize in cases where instances are retrieved from the ViewModelStore
    private val database by lazy { AppDatabase.getInstance(application) }
    private val deviceDao by lazy { database.deviceDao() }
    private val deviceRepository by lazy { DeviceRepository(deviceDao = deviceDao) }

    // Use cases
    private val manageDevicesUseCase by lazy {
        ManageDevicesUseCase(repository = deviceRepository)
    }
    private val attestationUseCase by lazy {
        AttestationUseCase(repository = deviceRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(manageDevicesUseCase, attestationUseCase) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class]

        setContent {
            Layout(viewModel)
        }
    }
}