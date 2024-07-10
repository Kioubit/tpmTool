package eu.gload.tpmtool

import android.app.Application

class App : Application() {
    companion object {
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