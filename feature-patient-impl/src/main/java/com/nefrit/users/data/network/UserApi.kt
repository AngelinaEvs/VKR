package com.nefrit.users.data.network

import com.nefrit.common.utils.getFile
import com.nefrit.users.data.network.model.UserRemote
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import java.io.File


interface UserApi {

    fun getUsers(): Single<List<UserRemote>>

    fun getUser(id: Int): Single<UserRemote>

    @Multipart
    @POST("/add_message")
    fun checkWoundType(
        @Part image: okhttp3.MultipartBody.Part?
    ): WoundResponse

}

data class WoundResponse(
    val result: String
)