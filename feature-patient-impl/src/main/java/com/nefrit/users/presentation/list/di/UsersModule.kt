package com.nefrit.users.presentation.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.nefrit.common.di.viewmodel.ViewModelKey
import com.nefrit.common.di.viewmodel.ViewModelModule
import com.nefrit.feature_user_api.domain.interfaces.UserInteractor
import com.nefrit.users.UsersRouter
import com.nefrit.users.presentation.list.PatientsViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module(
    includes = [
        ViewModelModule::class
    ]
)
class UsersModule {

    @Provides
    fun provideMainViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): PatientsViewModel {
        return ViewModelProviders.of(fragment, factory).get(PatientsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(PatientsViewModel::class)
    fun provideSignInViewModel(interactor: UserInteractor, router: UsersRouter): ViewModel {
        return PatientsViewModel(interactor, router)
    }
}