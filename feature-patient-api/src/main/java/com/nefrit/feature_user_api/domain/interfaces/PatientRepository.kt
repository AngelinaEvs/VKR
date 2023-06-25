package com.nefrit.feature_user_api.domain.interfaces

import io.reactivex.Observable
import io.reactivex.Single

interface PatientRepository {

    fun getPatient(id: Int): Observable<Patient>?

    fun getPatients(): Observable<List<Patient>>

    fun getPatientsByClass(woundType: String): Single<List<Patient>>

    fun savePatient(patient: NewPatient)
}