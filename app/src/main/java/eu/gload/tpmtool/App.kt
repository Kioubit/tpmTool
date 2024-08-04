package eu.gload.tpmtool

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class App : Application() {

    companion object {
        val applicationScope = CoroutineScope(SupervisorJob())

        private lateinit var mContext : App
        @JvmStatic fun getMContext(): App {
            return mContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        mContext = this
    }
}