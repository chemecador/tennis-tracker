package com.chemecador.tennistracker.wear.di

import com.chemecador.tennistracker.wear.auth.GoogleCredentialClient
import com.chemecador.tennistracker.wear.auth.WearGoogleAuth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { GoogleCredentialClient(androidContext()) }
    single { WearGoogleAuth(androidContext()) }
}
