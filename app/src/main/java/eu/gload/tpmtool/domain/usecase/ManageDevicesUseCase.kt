package eu.gload.tpmtool.domain.usecase

import eu.gload.tpmtool.data.DeviceRepository
import eu.gload.tpmtool.domain.mapper.DeviceMapper
import eu.gload.tpmtool.domain.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Base64

class ManageDevicesUseCase(
    private val repository: DeviceRepository
) {
    fun getDeviceList(): Flow<List<Device>> {
        return repository.getAllDevices().flowOn(Dispatchers.IO).map { entities ->
            entities.map { DeviceMapper.mapToDomain(it) }
        }
    }

    suspend fun editDevice(device: Device, name: String, pubKey: String): Boolean {
        val pubKeyTrimmed = pubKey.trim()
        checkNewDeviceParameters(name, pubKeyTrimmed)

        return withContext(Dispatchers.IO) {
            val updatedDevice = device.copy(
                name = name,
                base64Pem = pubKeyTrimmed
            )
            repository.updateDevice(DeviceMapper.mapToEntity(updatedDevice))
        }
    }

    suspend fun addDevice(name: String, pubKey: String): Boolean = withContext(Dispatchers.IO) {
            val pubKeyTrimmed = pubKey.trim()
            checkNewDeviceParameters(name, pubKeyTrimmed)
            val device = Device(
                name = name,
                base64Pem = pubKeyTrimmed,
                attestationJson = "",
            )
            val deviceEntity = DeviceMapper.mapToEntity(device)
            repository.addDevice(deviceEntity)
    }

    private fun checkNewDeviceParameters (name: String, pubKey: String) {
        if (name.isBlank() || pubKey.isBlank()) {
            throw Exception("Some values not filled in")
        }
        try {
            if (!Base64.getEncoder().encodeToString(Base64.getDecoder().decode(pubKey)).equals(pubKey)) {
                throw Exception()
            }
        } catch (_ : Exception) {
            throw Exception("Invalid public key value")
        }
    }

    suspend fun deleteDevice(device: Device) = withContext(Dispatchers.IO) {
        val deviceEntity = DeviceMapper.mapToEntity(device)
        repository.deleteDevice(deviceEntity)
    }
}
