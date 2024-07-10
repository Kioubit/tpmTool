package eu.gload.tpmtool.logic.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase

@Database(entities = [Device::class], exportSchema = false, version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun devicesDao(): DevicesDao

    companion object {
        private const val DB_NAME = "app_db"

        @Volatile
        private var instance: AppDatabase? = null


        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                if (instance == null) {
                    instance = databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    ).fallbackToDestructiveMigration().build()
                }
            }

            return instance!!
        }
    }
}
