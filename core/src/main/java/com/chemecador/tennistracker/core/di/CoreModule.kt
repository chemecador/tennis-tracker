package com.chemecador.tennistracker.core.di

import com.chemecador.tennistracker.core.auth.AuthRepository
import com.chemecador.tennistracker.core.auth.AuthViewModel
import com.chemecador.tennistracker.core.match.MatchSessionViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    single { FirebaseAuth.getInstance() }
    single { AuthRepository(get()) }

    viewModelOf(::AuthViewModel)
    viewModelOf(::MatchSessionViewModel)
}
