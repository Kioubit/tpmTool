package eu.gload.tpmtool.viewModel

import android.content.Context
import eu.gload.tpmtool.data.DeviceRepository
import eu.gload.tpmtool.data.database.AppDatabase
import eu.gload.tpmtool.domain.model.AttestationResult
import eu.gload.tpmtool.domain.model.Device
import eu.gload.tpmtool.domain.usecase.Attestation
import eu.gload.tpmtool.domain.usecase.AttestationUseCase
import eu.gload.tpmtool.domain.usecase.ManageDevicesUseCase
import kotlinx.coroutines.flow.MutableStateFlow



data class MainUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val navigationEvent: NavigationEvent = NavigationEvent.NONE,

    val devices: List<Device>? = null,
    val selectedDevice: Device? = null,
    val nonce: String = Attestation.getNonce(),
    val attestationResult: AttestationResult? = null,
)


class StateRepository private constructor(appContext: Context){
    val uiState = MutableStateFlow(MainUiState())

    private val database = AppDatabase.getInstance(appContext)
    private val deviceDao by lazy { database.deviceDao() }
    private val deviceRepository by lazy {
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

    companion object {
        @Volatile private var INSTANCE: StateRepository? = null

        fun getInstance(appContext: Context): StateRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StateRepository(appContext).also { INSTANCE = it }
            }
        }
    }
}