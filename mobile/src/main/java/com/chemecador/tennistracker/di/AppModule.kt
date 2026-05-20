package com.chemecador.tennistracker.di

import com.chemecador.tennistracker.auth.GoogleCredentialClient
import com.chemecador.tennistracker.data.friends.FriendshipRepository
import com.chemecador.tennistracker.data.profile.UserProfileRepository
import com.chemecador.tennistracker.ui.friends.FriendsViewModel
import com.chemecador.tennistracker.ui.profile.ChooseUsernameViewModel
import com.chemecador.tennistracker.ui.profile.UserProfileViewModel
import com.chemecador.tennistracker.ui.setup.SetupMatchViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { FirebaseFirestore.getInstance() }
    single { GoogleCredentialClient(androidContext()) }
    single { UserProfileRepository(get()) }
    single { FriendshipRepository(get()) }

    viewModelOf(::ChooseUsernameViewModel)
    viewModel { (uid: String) -> UserProfileViewModel(uid, get()) }
    viewModel { (myUid: String) -> FriendsViewModel(myUid, get(), get()) }
    viewModel { (myUid: String) -> SetupMatchViewModel(myUid, get(), get()) }
}
