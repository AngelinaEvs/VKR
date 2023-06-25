package com.nefrit.users.presentation.list.di

import androidx.fragment.app.Fragment
import com.nefrit.common.di.scope.ScreenScope
import com.nefrit.users.presentation.list.PatientsFragment
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [
        UsersModule::class
    ]
)
@ScreenScope
interface PatientsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): PatientsComponent
    }

    fun inject(fragment: PatientsFragment)
}