package com.nefrit.users.data.network

import com.nefrit.users.data.network.model.UserRemote
import io.reactivex.Single
import javax.inject.Inject

class UserApiImpl @Inject constructor() : UserApi {

    override fun getUsers(): Single<List<UserRemote>> {
        return Single.just(mockUsers())
    }

    override fun getUser(id: Int): Single<UserRemote> {
        return Single.fromCallable {

        }
    }
}