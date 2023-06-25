package com.nefrit.feature_user_api.domain.interfaces

import com.nefrit.common.Patient
import io.reactivex.Observable

interface UserInteractor {

    fun getPatient(id: Int): Observable<Patient>

    fun getPatients(): Observable<List<Patient>>

    fun savePatient(patient: NewPatient)
}