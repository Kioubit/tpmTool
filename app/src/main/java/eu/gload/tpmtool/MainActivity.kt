package eu.gload.tpmtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import eu.gload.tpmtool.data.DeviceRepository
import eu.gload.tpmtool.data.database.AppDatabase
import eu.gload.tpmtool.domain.usecase.AttestationUseCase
import eu.gload.tpmtool.domain.usecase.ManageDevicesUseCase
import eu.gload.tpmtool.ui.Layout
import eu.gload.tpmtool.viewModel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(application)
        val deviceDao by lazy { database.deviceDao() }
        val deviceRepository by lazy {
            DeviceRepository(
                deviceDao = deviceDao
            )
        }
        val manageDevicesUseCase by lazy {
            ManageDevicesUseCase(
                repository = deviceRepository
            )
        }
        val attestationUseCase by lazy {
            AttestationUseCase(
                repository = deviceRepository,
            )
        }
        val vm = MainViewModel(manageDevicesUseCase, attestationUseCase)
        setContent {
            Layout(vm)
        }
    }
}