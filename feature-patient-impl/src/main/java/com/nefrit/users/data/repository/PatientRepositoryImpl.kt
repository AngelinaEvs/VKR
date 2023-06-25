package com.nefrit.users.data.repository

import com.nefrit.common.NewPatient
import com.nefrit.core_db.AppDatabase
import com.nefrit.feature_user_api.domain.interfaces.PatientRepository
import com.nefrit.users.data.mappers.mapPatientLocalToPatient
import com.nefrit.users.data.mappers.mapPatientRemoteToPatient
import com.nefrit.users.data.mappers.mapPatientToPatientLocal
import com.nefrit.users.data.network.UserApi
import com.nefrit.common.Patient
import com.nefrit.common.WoundType
import com.nefrit.common.utils.getFile
import com.nefrit.core_db.model.PatientLocal
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PatientRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val db: AppDatabase
) : PatientRepository {

    override fun getPatient(id: Int): Observable<Patient> {
        return Single.concat(getLocalUser(id))
            .toObservable()
    }

    private fun getLocalUser(id: Int): Single<Patient> {
        return db.patientDao().getPatient(id)
            .map { mapPatientLocalToPatient(it) }
    }

    override fun getPatients(): Observable<List<Patient>> {
        return Single.concat(getLocalUsers(), getRemoteUsers())
            .toObservable()
    }

    override fun getPatientsByClass(woundType: String): Observable<List<Patient>> {
        return Single.concat(db.patientDao().getPatientsByClass(woundType))
            .toObservable()
    }

    override fun savePatient(patient: NewPatient) {
        db.patientDao().insert(
            PatientLocal(
                patient.id,
                patient.photoData.date,
                woundType = checkWoundType(uri),
                photoData = patient.photoData
            )
        )
    }

    private suspend fun checkWoundType(uri: String): String {
        return withContext(Dispatchers.IO) {
            var file = getFile(uri)

            var body =
                okhttp3.MultipartBody.Part.createFormData("image", file.name, file)

            return WoundType.valueOf(
                api.checkWoundType(
                    body
                ).result
            )
        }
    }

    private fun getLocalUsers(): Single<List<Patient>> {
        return db.patientDao().getPatients()
            .map { it.map { mapPatientLocalToPatient(it) } }
    }

    private fun getRemoteUsers(): Single<List<Patient>> {
        return api.getUsers()
            .map { it.map { mapPatientRemoteToPatient(it) } }
            .doOnSuccess { db.patientDao().insert(it.map { mapPatientToPatientLocal(it) }) }
    }
}