package eu.gload.tpmtool.domain.mapper

import eu.gload.tpmtool.data.database.DeviceEntity
import eu.gload.tpmtool.domain.model.Device

object DeviceMapper {
    fun mapToDomain(entity: DeviceEntity): Device {
        return Device(
            id = entity.id,
            name = entity.name,
            base64Pem = entity.base64Pem,
            attestationJson = entity.attestationJson,
            lastSuccessTime = entity.lastSuccessTime,
        )
    }

    fun mapToEntity(domain: Device): DeviceEntity {
        return DeviceEntity(
            id = domain.id,
            name = domain.name,
            base64Pem = domain.base64Pem,
            attestationJson = domain.attestationJson,
            lastSuccessTime = domain.lastSuccessTime
        )
    }
}