package eu.gload.tpmtool.domain.model

import java.io.Serializable

enum class AttestationResultType {
    OK,
    CHANGED,
    FAILED,
    REPLAY
}

data class AttestationResult(
    var type: AttestationResultType = AttestationResultType.FAILED,
    var newAttestationJson: String = "",
    var failReason: String = "",
    var differencesString: String = "",
    var device: Device? = null
) : Serializable