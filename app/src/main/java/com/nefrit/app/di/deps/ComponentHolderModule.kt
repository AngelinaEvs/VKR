package com.nefrit.app.di.deps

import com.nefrit.app.app.App
import com.nefrit.core_db.di.DbApi
import com.nefrit.core_db.di.DbHolder
import com.nefrit.common.di.FeatureApiHolder
import com.nefrit.common.di.FeatureContainer
import com.nefrit.common.di.scope.ApplicationScope
import com.nefrit.feature_user_api.di.PatientFeatureApi
import com.nefrit.splash.di.SplashFeatureApi
import com.nefrit.splash.di.SplashFeatureHolder
import com.nefrit.users.di.UserFeatureHolder
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
interface ComponentHolderModule {

    @ApplicationScope
    @Binds
    fun provideFeatureContainer(application: App): FeatureContainer

    @ApplicationScope
    @Binds
    @ClassKey(SplashFeatureApi::class)
    @IntoMap
    fun provideSplashFeatureHolder(splashFeatureHolder: SplashFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(PatientFeatureApi::class)
    @IntoMap
    fun provideUserFeatureHolder(userFeatureHolder: UserFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(DbApi::class)
    @IntoMap
    fun provideDbFeature(dbHolder: DbHolder): FeatureApiHolder
}