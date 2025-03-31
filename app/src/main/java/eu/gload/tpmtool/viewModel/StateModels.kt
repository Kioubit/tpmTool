package eu.gload.tpmtool.viewModel

import eu.gload.tpmtool.domain.model.AttestationResult
import eu.gload.tpmtool.domain.model.Device
import eu.gload.tpmtool.domain.usecase.Attestation

data class MainUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    val devices: List<Device>? = null,
    val selectedDevice: Device? = null,
    val nonce: String = Attestation.getNonce(),
    val attestationResult: AttestationResult? = null,
)

sealed class NavigationEvent {
    abstract val routeString: String?

    data class To(val route: Routes) : NavigationEvent() {
        override val routeString: String = route.routeString
    }

    data object Back : NavigationEvent() {
        override val routeString: String? = null
    }

    enum class Routes(val routeString: String) {
        ATTESTATION_RESULT("attestation_result"),
        MANAGE_DEVICE("manage_device"),
        MAIN("main")
    }
}