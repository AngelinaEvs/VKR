package com.nefrit.users.di

import com.nefrit.common.di.CommonApi
import com.nefrit.common.di.scope.FeatureScope
import com.nefrit.core_db.di.DbApi
import com.nefrit.feature_user_api.di.PatientFeatureApi
import com.nefrit.users.UsersRouter
import com.nefrit.users.presentation.details.di.PatientComponent
import com.nefrit.users.presentation.list.di.PatientsComponent
import dagger.BindsInstance
import dagger.Component

@FeatureScope
@Component(
    dependencies = [
        PatietFeatureDependencies::class
    ],
    modules = [
        PatientFeatureModule::class
    ]
)
interface PatientFeatureComponent : PatientFeatureApi {

    fun patientsComponentFactory(): PatientsComponent.Factory

    fun patientComponentFactory(): PatientComponent.Factory

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun router(usersRouter: UsersRouter): Builder

        fun withDependencies(deps: PatietFeatureDependencies): Builder

        fun build(): PatientFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class
        ]
    )
    interface PatietFeatureDependenciesComponent : PatietFeatureDependencies
}