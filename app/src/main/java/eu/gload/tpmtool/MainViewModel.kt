package eu.gload.tpmtool

import android.os.Parcelable
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.gload.tpmtool.logic.Attestation
import eu.gload.tpmtool.logic.Attestation.AttestationResult
import eu.gload.tpmtool.logic.AttestationUseCase
import eu.gload.tpmtool.logic.database.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Page : Parcelable {
    Main, ManageDevice, AttestationResult, Loading
}

data class MainUiState(
    val page: Page = Page.Loading,
    val deviceList: List<Device>? = null,
    val selectedDevice: Device? = null,
    val nonce: String = Attestation.getNonce(),
    val attestationResult: AttestationResult? = null,
)


class MainViewModel : ViewModel() {
    private val attestation = AttestationUseCase(Dispatchers.IO, Dispatchers.Default)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshDeviceList()
            changePage(Page.Main)
        }
    }

    fun selectDevice(device: Device) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDevice = device
            )
        }
    }

    fun changePage(page: Page) {
        _uiState.update { currentState ->
            currentState.copy(
                page = page
            )
        }
    }

    private fun refreshDeviceList() = viewModelScope.launch {
        val deviceList = attestation.getDeviceList()
        var selected: Device? = null
        if (deviceList.isNotEmpty()) {
            selected = deviceList[0]
        }
        _uiState.update { currentState ->
            currentState.copy(
                deviceList = deviceList,
                selectedDevice = selected
            )
        }
    }

    fun acceptChanges() = viewModelScope.launch {
        changePage(Page.Loading)
        uiState.value.attestationResult?.let { attestation.acceptChanges(it) }
        refreshDeviceList()
        changePage(Page.Main)
    }

    fun addOrEditDevice(deviceName: String, base64PubKey: String) = viewModelScope.launch {
        changePage(Page.Loading)
        try {
            val selectedDevice = uiState.value.selectedDevice
            if (selectedDevice != null) {
                attestation.editDevice(selectedDevice, deviceName, base64PubKey)
            } else {
                attestation.addDevice(deviceName, base64PubKey)
            }
            refreshDeviceList()
            changePage(Page.Main)
        } catch (ex: Exception) {
            Toast.makeText(App.getMContext(), ex.message, Toast.LENGTH_SHORT).show()
            changePage(Page.ManageDevice)
        }
    }

    fun viewLastResult() = viewModelScope.launch{
        changePage(Page.Loading)
        val oldResult = AttestationResult()
        oldResult.device = uiState.value.selectedDevice
        oldResult.type = Attestation.AttestationResultType.REPLAY
        oldResult.newAttestationJson = uiState.value.selectedDevice!!.attestationJson
        _uiState.update { currentState ->
            currentState.copy(
                attestationResult = oldResult
            )
        }
        changePage(Page.AttestationResult)
    }


    fun deleteSelectedDevice() = viewModelScope.launch {
        if (uiState.value.selectedDevice == null) {
            return@launch
        }
        changePage(Page.Loading)
        attestation.deleteDevice(uiState.value.selectedDevice!!)
        refreshDeviceList()
        changePage(Page.Main)
    }

    fun attest(input: String?) = viewModelScope.launch {
        changePage(Page.Loading)
        try {
            val result =
                attestation.attest(uiState.value.selectedDevice, input, uiState.value.nonce)
            _uiState.update { currentState ->
                currentState.copy(
                    attestationResult = result
                )
            }
            changePage(Page.AttestationResult)
        } catch (ex: Exception) {
            Toast.makeText(App.getMContext(), ex.message, Toast.LENGTH_SHORT).show()
            changePage(Page.Main)
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
                selectedDevice = null
            )
        }
        changePage(Page.ManageDevice)
    }
}