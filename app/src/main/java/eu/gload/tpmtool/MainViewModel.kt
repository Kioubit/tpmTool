package eu.gload.tpmtool

import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import eu.gload.tpmtool.logic.Attestation.AttestationResult
import eu.gload.tpmtool.logic.Attestation.GetNonce
import eu.gload.tpmtool.logic.Callback
import eu.gload.tpmtool.logic.ThreadRunner
import eu.gload.tpmtool.logic.database.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Pages : Parcelable {
    Main, AddDevice, AttestationResult, Loading
}


data class MainUiState(
    val page: Pages = Pages.Main,
    val deviceList: List<Device>? = null,
    val selectedDevice : Device? = null,
    val nonce : String = GetNonce(),
    val attestationResult : AttestationResult? = null
)


class MainViewModel : ViewModel(), Callback {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState : StateFlow<MainUiState> = _uiState.asStateFlow()

    private val tr = ThreadRunner.getInstance(this)

    fun changePage(page : Pages) {
        _uiState.update { currentState ->
            currentState.copy(
                page = page
            )
        }
    }
    fun addDevice(deviceName : String, base64PubKey : String ) {
        tr.AddDevice(deviceName,base64PubKey)
        changePage(Pages.Main)
        tr.requestDeviceList()
    }

    fun deleteSelectedDevice() {
        if (uiState.value.selectedDevice != null) {
            tr.DeleteDevice(uiState.value.selectedDevice)
            tr.requestDeviceList()
        }
    }
    fun attest(input : String?) {
        if (input == null) {
            return
        }
        if (uiState.value.selectedDevice == null) {
            Toast.makeText(App.getMContext(),"No device selected", Toast.LENGTH_SHORT).show()
            return
        }
        changePage(Pages.Loading)
        tr.PerformAttestation(uiState.value.selectedDevice,input, uiState.value.nonce)
    }

    fun refreshDeviceList() {
        Log.d("ViewModel", "Refresh devices")
        if (uiState.value.deviceList == null) {
            tr.requestDeviceList()
        }
    }

    fun selectDevice(device: Device) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDevice = device
            )
        }
    }

    fun acceptChanges(result: AttestationResult) {
        tr.AcceptChanges(result)
        changePage(Pages.Loading)
    }

    fun setNonce(nonce : String) {
        _uiState.update { currentState ->
            currentState.copy(
                nonce = nonce
            )
        }
    }

    override fun DeviceListReady(deviceListMutable: MutableList<Device>?) {
        if (deviceListMutable == null) {
            return
        }

        var selected : Device? = null
        if (deviceListMutable.size > 0) {
                selected = deviceListMutable[0]
        }

        _uiState.update { currentState ->
            currentState.copy(
                deviceList = deviceListMutable.toList(),
                selectedDevice = selected
            )
        }
    }

    override fun DeviceDeleted() {
        Toast.makeText(App.getMContext(), "Device deleted", Toast.LENGTH_SHORT).show()
        tr.requestDeviceList()
    }

    override fun DeviceAdded(error: ThreadRunner.CustomError?) {
        if (error != null) {
            Toast.makeText(App.getMContext(), error.errorMessage, Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(App.getMContext(), "Device added", Toast.LENGTH_SHORT).show()
        tr.requestDeviceList()
    }

    override fun ChangesAccepted() {
        Toast.makeText(App.getMContext(), "Changes accepted", Toast.LENGTH_SHORT).show()
        tr.requestDeviceList()
        changePage(Pages.Main)
    }

    override fun AttestationResultReady(result: AttestationResult?) {
        _uiState.update { currentState ->
            currentState.copy(
                page = Pages.AttestationResult,
                attestationResult = result
            )
        }
    }
}