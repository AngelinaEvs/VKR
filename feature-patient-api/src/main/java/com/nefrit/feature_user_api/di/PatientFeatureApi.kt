package com.nefrit.feature_user_api.di

import com.nefrit.feature_user_api.domain.interfaces.UserInteractor
import com.nefrit.feature_user_api.domain.interfaces.PatientRepository

interface PatientFeatureApi {

    fun provideUserRepository(): PatientRepository

    fun provideUserInteractor(): UserInteractor
}