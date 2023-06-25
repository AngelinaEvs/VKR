package com.nefrit.users.presentation.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nefrit.common.NewPatient
import com.nefrit.common.Patient
import com.nefrit.common.PhotoData
import com.nefrit.common.base.BaseViewModel
import com.nefrit.feature_user_api.domain.interfaces.UserInteractor
import com.nefrit.users.UsersRouter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class PatientsViewModel(
    private val interactor: UserInteractor,
    private val router: UsersRouter
) : BaseViewModel() {

    private val _usersLiveData = MutableLiveData<List<Patient>>()
    val usersLiveData: LiveData<List<Patient>> = _usersLiveData

    fun userClicked(patient: Patient) {
        router.openUser(patient.id)
    }

    fun getUsers() {
        disposables.add(
            interactor.getPatients()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread(), true)
                .subscribe({
                    _usersLiveData.value = it
                }, {
                    it.printStackTrace()
                })
        )
    }

    fun addNew(id: String, uri: String, squareValue: Double) {
        val currentDate = Date().time
        interactor.savePatient(NewPatient(
            id, PhotoData(uri = uri, "0", squareValue.toString(), currentDate))
        )
        _usersLiveData.value = usersLiveData.value
    }
}