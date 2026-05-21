package com.chemecador.tennistracker.core.di

import com.chemecador.tennistracker.core.auth.AuthRepository
import com.chemecador.tennistracker.core.auth.AuthViewModel
import com.chemecador.tennistracker.core.data.match.FirestoreMatchRepository
import com.chemecador.tennistracker.core.data.match.MatchRepository
import com.chemecador.tennistracker.core.match.MatchSessionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { AuthRepository(get()) }
    single<MatchRepository> { FirestoreMatchRepository(get()) }

    viewModelOf(::AuthViewModel)
    viewModel {
        val auth: AuthRepository = get()
        MatchSessionViewModel(
            matchRepository = get(),
            currentUid = { auth.currentUser?.uid },
        )
    }
}
