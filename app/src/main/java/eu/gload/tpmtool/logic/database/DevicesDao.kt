package eu.gload.tpmtool.logic.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DevicesDao {
    @get:Query("Select * from device")
    val deviceList: List<Device>

    @Insert
    fun insertDevice(device: Device)

    @Update
    fun updateDevice(device: Device)

    @Delete
    fun deleteDevice(device: Device)
}

