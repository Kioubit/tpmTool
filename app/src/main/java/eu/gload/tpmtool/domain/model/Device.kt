package eu.gload.tpmtool.domain.model

data class Device(
    internal val id: Int = 0,
    val name: String,
    val base64Pem: String,
    var attestationJson: String,
    var lastSuccessTime: Long? = null
)