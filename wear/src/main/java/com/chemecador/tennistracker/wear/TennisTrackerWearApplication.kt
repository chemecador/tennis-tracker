package com.chemecador.tennistracker.wear

import android.app.Application
import com.chemecador.tennistracker.core.di.coreModule
import com.chemecador.tennistracker.wear.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TennisTrackerWearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TennisTrackerWearApplication)
            modules(coreModule, appModule)
        }
    }
}
