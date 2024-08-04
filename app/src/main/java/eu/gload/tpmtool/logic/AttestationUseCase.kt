package eu.gload.tpmtool.logic

import eu.gload.tpmtool.App
import eu.gload.tpmtool.logic.Attestation.AttestationResult
import eu.gload.tpmtool.logic.database.AppDatabase
import eu.gload.tpmtool.logic.database.Device
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64

class AttestationUseCase(private val ioDispatcher: CoroutineDispatcher, private val defaultDispatcher: CoroutineDispatcher) {
    suspend fun getDeviceList(): List<Device> = withContext(ioDispatcher) {
        val list = AppDatabase.getInstance(App.getMContext()).devicesDao().deviceList
        return@withContext list
    }

    suspend fun acceptChanges(result: AttestationResult) = withContext(ioDispatcher) {
        result.device?.let {
            result.device?.attestationJson = result.newAttestationJson
            result.device?.lastSuccessTime = System.currentTimeMillis()/ 1000L
            App.applicationScope.launch {
                AppDatabase.getInstance(App.getMContext()).devicesDao().updateDevice(it)
            }.join()
        }
    }

    suspend fun addDevice(name: String, pubKey: String) {
        val pubKeyTrimmed = pubKey.trim()
        checkNewDeviceParameters(name, pubKeyTrimmed)
        withContext(ioDispatcher) {
            val device = Device()
            device.name = name
            device.base64Pem = pubKeyTrimmed
            device.attestationJson = ""
            App.applicationScope.launch {
                AppDatabase.getInstance(App.getMContext()).devicesDao().insertDevice(device)
            }.join()
        }
    }

    suspend fun editDevice(device: Device, name: String, pubKey: String) {
        val pubKeyTrimmed = pubKey.trim()
        checkNewDeviceParameters(name, pubKeyTrimmed)
        withContext(ioDispatcher) {
            device.name = name
            device.base64Pem = pubKeyTrimmed
            App.applicationScope.launch {
                AppDatabase.getInstance(App.getMContext()).devicesDao().updateDevice(device)
            }.join()
        }
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

    suspend fun deleteDevice(device: Device) = withContext(ioDispatcher){
        App.applicationScope.launch {
            AppDatabase.getInstance(App.getMContext()).devicesDao().deleteDevice(device)
        }.join()
    }

    suspend fun attest(device: Device?, input: String?, nonce: String) : AttestationResult = withContext(defaultDispatcher){
        if (device === null) {
            throw Exception("No device selected")
        }
        if (input === null) {
            throw Exception("Scan failed")
        }
        return@withContext Attestation.perform(input,nonce,device)
    }
}

