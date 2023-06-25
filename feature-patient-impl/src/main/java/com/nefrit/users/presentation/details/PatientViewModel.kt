package com.nefrit.users.presentation.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nefrit.common.PatientDetails
import com.nefrit.common.base.BaseViewModel
import com.nefrit.feature_user_api.domain.interfaces.UserInteractor
import com.nefrit.users.UsersRouter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class PatientViewModel(
    private val interactor: UserInteractor,
    private val id: Int,
    private val router: UsersRouter
) : BaseViewModel() {

    private val _patientLiveData = MutableLiveData<PatientDetails>()
    val patientLiveData: LiveData<PatientDetails> = _patientLiveData

    init {
        disposables.add(
            interactor.getPatient(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread(), true)
                .subscribe({
                    _patientLiveData.value = it
                }, {
                    it.printStackTrace()
                })
        )
    }

    fun backClicked() {
        router.returnToUsers()
    }
}