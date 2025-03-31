package eu.gload.tpmtool.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.gload.tpmtool.domain.model.AttestationResult
import eu.gload.tpmtool.domain.model.AttestationResultType
import eu.gload.tpmtool.domain.model.Device
import eu.gload.tpmtool.domain.usecase.Attestation
import eu.gload.tpmtool.domain.usecase.AttestationUseCase
import eu.gload.tpmtool.domain.usecase.ManageDevicesUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class MainViewModel(
    private val manageDevicesUseCase: ManageDevicesUseCase,
    private val attestationUseCase: AttestationUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            monitorDevices()
        }
    }

    private suspend fun monitorDevices() {
        try {
            // The flow will continuously update
            manageDevicesUseCase.getDeviceList().collect { devices ->
                _uiState.update {
                    it.copy(
                        devices = devices,
                        // Any function editing the database table should set this to true first
                        isLoading = false,
                        // Ensure selected device is still valid
                        selectedDevice = it.selectedDevice?.let { selected ->
                            devices.find { device -> device.id == selected.id }
                        }
                    )
                }

                // Select the first one
                if (uiState.value.selectedDevice == null) {
                    uiState.value.devices?.getOrNull(0)?.let { device ->
                        _uiState.update { it.copy(selectedDevice = device) }
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Failed to load devices: ${e.message}"
                )
            }
        }
    }

    fun selectDevice(device: Device) {
        _uiState.update { it.copy(selectedDevice = device) }
    }

    fun acceptChanges() {
        val currentState = _uiState.value
        val result = currentState.attestationResult ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = attestationUseCase.acceptChanges(result)
                if (success) {
                    _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.MAIN))
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to save attestation changes"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error saving changes: ${e.message}"
                    )
                }
            }
        }
    }

    fun navigateToEditDevice() = viewModelScope.launch {
        _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.MANAGE_DEVICE))
    }

    fun navigateToMain() = viewModelScope.launch {
        _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.MAIN))
    }

    fun navigateBack() = viewModelScope.launch {
        _navigationEvent.emit(NavigationEvent.Back)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveDevice(device: Device?, name: String, pubKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = if (device == null) {
                    // Adding a new device
                    manageDevicesUseCase.addDevice(name, pubKey)
                } else {
                    // Updating existing device
                    manageDevicesUseCase.editDevice(device, name, pubKey)
                }

                if (success) {
                    _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.MAIN))
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to save device"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "${e.message}"
                    )
                }
            }
        }
    }


    fun viewLastResult() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val oldResult = AttestationResult()
        oldResult.device = uiState.value.selectedDevice
        oldResult.type = AttestationResultType.REPLAY
        oldResult.newAttestationJson = uiState.value.selectedDevice!!.attestationJson
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                attestationResult = oldResult,
            )
        }
        _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.ATTESTATION_RESULT))
    }


    fun deleteSelectedDevice() = viewModelScope.launch {
        if (uiState.value.selectedDevice == null) {
            return@launch
        }
        try {
            _uiState.update {
                it.copy(
                    isLoading = true, // Await DB update on flow
                    selectedDevice = null,
                )
            }
            manageDevicesUseCase.deleteDevice(uiState.value.selectedDevice!!)
            _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.MAIN))
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Failed to delete device: ${e.message}")
            }
        }
    }

    fun performAttestation(input: String?) {
        val currentState = _uiState.value
        val device = currentState.selectedDevice
        val nonce = currentState.nonce

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    attestationResult = null
                )
            }
            try {
                val result = attestationUseCase.attest(device?.id, input, nonce)
                _uiState.update {
                    it.copy(
                        attestationResult = result,
                        isLoading = false,
                    )
                }
                _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.ATTESTATION_RESULT))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Attestation failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun setNonce(nonce: String) {
        _uiState.update { currentState ->
            currentState.copy(
                nonce = nonce.takeIf { nonce.isNotBlank() } ?: Attestation.getNonce()
            )
        }
    }

    fun addDeviceScreen() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDevice = null,
            )
        }
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.To(NavigationEvent.Routes.MANAGE_DEVICE))
        }
    }
}