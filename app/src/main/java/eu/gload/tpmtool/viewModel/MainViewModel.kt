package eu.gload.tpmtool.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.gload.tpmtool.domain.model.AttestationResult
import eu.gload.tpmtool.domain.model.AttestationResultType
import eu.gload.tpmtool.domain.model.Device
import eu.gload.tpmtool.domain.usecase.Attestation
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationEvent {
    NONE,
    TO_ATTESTATION_RESULT,
    TO_MANAGE_DEVICE,
    TO_MAIN
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val stateRepository: StateRepository = StateRepository.getInstance(getApplication())
    private val _uiState = stateRepository.uiState
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val manageDevicesUseCase = stateRepository.manageDevicesUseCase
    private val attestationUseCase = stateRepository.attestationUseCase

    init {
        if (uiState.value.devices == null) {
            loadDevices()
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                manageDevicesUseCase.getDeviceList().collect { devices ->
                    _uiState.update {
                        it.copy(
                            devices = devices,
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationEvent = NavigationEvent.TO_MAIN,
                            devices = null
                        )
                    }
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

    fun navigateToEditDevice() {
        _uiState.update { it.copy(navigationEvent = NavigationEvent.TO_MANAGE_DEVICE) }
    }
    fun handleBackToMain() {
        _uiState.update {
            it.copy(
                navigationEvent = NavigationEvent.TO_MAIN,
                attestationResult = null
            )
        }
    }
    fun resetNavigation() {
        _uiState.update { it.copy(navigationEvent = NavigationEvent.NONE) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveDevice(device: Device?, name: String, pubKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val success = if (device == null) {
                    // Adding a new device
                    manageDevicesUseCase.addDevice(name, pubKey)
                } else {
                    // Updating existing device
                    manageDevicesUseCase.editDevice(device, name, pubKey)
                }

                if (success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationEvent = NavigationEvent.TO_MAIN
                        )
                    }
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
                        errorMessage = "Error: ${e.message}"
                    )
                }
            }
        }
    }



    fun viewLastResult() = viewModelScope.launch{
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val oldResult = AttestationResult()
        oldResult.device = uiState.value.selectedDevice
        oldResult.type = AttestationResultType.REPLAY
        oldResult.newAttestationJson = uiState.value.selectedDevice!!.attestationJson
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                attestationResult = oldResult,
                navigationEvent = NavigationEvent.TO_ATTESTATION_RESULT
            )
        }
    }


    fun deleteSelectedDevice() = viewModelScope.launch {
        if (uiState.value.selectedDevice == null) {
            return@launch
        }
        try {
            manageDevicesUseCase.deleteDevice(uiState.value.selectedDevice!!)
            _uiState.update { it.copy(
                selectedDevice = null,
                navigationEvent = NavigationEvent.TO_MAIN,
                devices = null
            ) }
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = attestationUseCase.attest(device?.id, input, nonce)
                _uiState.update {
                    it.copy(
                        attestationResult = result,
                        isLoading = false,
                        navigationEvent = NavigationEvent.TO_ATTESTATION_RESULT
                    )
                }
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
                navigationEvent = NavigationEvent.TO_MANAGE_DEVICE
            )
        }
    }
}