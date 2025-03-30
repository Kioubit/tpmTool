package eu.gload.tpmtool.data

import eu.gload.tpmtool.data.database.DeviceDao
import eu.gload.tpmtool.data.database.DeviceEntity
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val deviceDao: DeviceDao) {
    fun getAllDevices(): Flow<List<DeviceEntity>> = deviceDao.getAllDevices()
    suspend fun getDeviceById(deviceId: Int): DeviceEntity? = deviceDao.getDeviceById(deviceId)

    suspend fun updateDevice(device: DeviceEntity): Boolean {
        return try {
            deviceDao.updateDevice(device) > 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun addDevice(device: DeviceEntity): Boolean {
        return try {
            deviceDao.insertDevice(device)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteDevice(device: DeviceEntity): Boolean {
        return try {
            deviceDao.deleteDevice(device) > 0
        } catch (_: Exception) {
            false
        }
    }
}