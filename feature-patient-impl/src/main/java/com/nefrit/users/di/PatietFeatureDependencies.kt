package com.nefrit.users.di

import com.nefrit.common.data.network.NetworkApiCreator
import com.nefrit.core_db.AppDatabase

interface PatietFeatureDependencies {

    fun networkApiCreator(): NetworkApiCreator

    fun db(): AppDatabase
}