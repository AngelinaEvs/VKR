package com.nefrit.users.di

import com.nefrit.common.di.FeatureApiHolder
import com.nefrit.common.di.FeatureContainer
import com.nefrit.common.di.scope.ApplicationScope
import com.nefrit.core_db.di.DbApi
import com.nefrit.users.UsersRouter
import javax.inject.Inject

@ApplicationScope
class UserFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val usersRouter: UsersRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val userFeatureDependencies = DaggerUserFeatureComponent_PatietFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .build()
        return DaggerPatientFeatureComponent.builder()
            .withDependencies(userFeatureDependencies)
            .router(usersRouter)
            .build()
    }
}