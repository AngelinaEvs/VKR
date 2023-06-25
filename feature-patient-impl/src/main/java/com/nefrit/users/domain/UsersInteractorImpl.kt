package com.nefrit.users.domain

import com.nefrit.common.NewPatient
import com.nefrit.common.Patient
import com.nefrit.common.PatientDetails
import com.nefrit.common.PhotoData
import com.nefrit.feature_user_api.domain.interfaces.UserInteractor
import com.nefrit.feature_user_api.domain.interfaces.PatientRepository
import io.reactivex.Observable
import javax.inject.Inject

class UsersInteractorImpl @Inject constructor(
    private val patientRepository: PatientRepository
) : UserInteractor {

    override fun getPatients(): Observable<List<Patient>> {
        return patientRepository.getPatients()
    }

    override fun getPatient(id: Int): Observable<PatientDetails> {
        return patientRepository.getPatient(id)?.map {
            val l = patientRepository.getPatientsByClass(it.woundType.toString()).map { it }
            val s = it.photoData[0].squareValue.toDouble()
            val standart = mutableListOf(0.0 to s)
            getStandart(s, standart)
            val byClassList = mutableListOf<Pair<Double, Double>>()
            byClass(l, byClassList)
            PatientDetails(
                standart = standart,
                byClass = byClassList,
                dynamics = dynamic(it.photoData),
                photoData = it.photoData,
            )
        }
    }

    private fun getStandart(s: Double, list: MutableList<Pair<Double, Double>>) {
        var newS = s - 0.0086 * 24
        if (newS < 0.0) return
        list.add(list.size.toDouble() to newS)
        if (newS > 0.0) getStandart(newS, list)
    }

    private fun byClass(patients: List<Patient>, resList: MutableList<Pair<Double, Double>>) {
        val list = patients
            .sortedBy { it.photoData.dayNumber }
        val f = list[1].photoData.dayNumber.toInt()
        val l = list.last().photoData.dayNumber.toInt()
        for (i in f..l) {
            var average = mutableListOf<Double>()
            list.filter { it.photoData.dayNumber.toInt() == i }
                .forEach {
                    average.add(it.photoData.squareValue.toDouble())
                }
            var sum = 0.0
            sum = sum + average.map { it }
            val res = sum / average.size
            resList.add(i.toDouble() to res)
        }
    }

    private fun dynamic(list: List<PhotoData>) =
        list.map { it.dayNumber.toDouble() to it.squareValue.toDouble() }

    override fun savePatient(patient: NewPatient) {
        patientRepository.insertPatient(patient)
    }
}