package eu.gload.tpmtool.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity (
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "device_name") var name: String = "",
    @ColumnInfo(name = "base64_pub_pem") var base64Pem: String = "",
    @ColumnInfo(name = "attestation_json") var attestationJson: String = "",
    @ColumnInfo(name = "last_success_time") var lastSuccessTime: Long? = null
)


