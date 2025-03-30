package eu.gload.tpmtool.domain.usecase

import eu.gload.tpmtool.data.DeviceRepository
import eu.gload.tpmtool.domain.mapper.DeviceMapper
import eu.gload.tpmtool.domain.model.AttestationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AttestationUseCase(private val repository: DeviceRepository) {
    suspend fun attest(deviceID: Int?, input: String?, nonce: String) : AttestationResult = withContext(
        Dispatchers.Default){
        if (deviceID === null) {
            throw Exception("No device selected")
        }
        val device = repository.getDeviceById(deviceID)?.let { DeviceMapper.mapToDomain(it) }
            ?: throw IllegalArgumentException("Device not found")

        if (input === null) {
            throw Exception("Scan failed")
        }
        return@withContext Attestation.perform(input,nonce,device)
    }

    suspend fun acceptChanges(result: AttestationResult): Boolean = withContext(Dispatchers.IO) {
        result.device?.let {
            it.attestationJson = result.newAttestationJson
            it.lastSuccessTime = System.currentTimeMillis() / 1000L
            return@withContext repository.updateDevice(DeviceMapper.mapToEntity(it))
        } ?: false
    }
}