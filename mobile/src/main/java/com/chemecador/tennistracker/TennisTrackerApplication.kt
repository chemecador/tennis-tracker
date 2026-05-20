package com.chemecador.tennistracker

import android.app.Application
import com.chemecador.tennistracker.core.di.coreModule
import com.chemecador.tennistracker.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TennisTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TennisTrackerApplication)
            modules(coreModule, appModule)
        }
    }
}
