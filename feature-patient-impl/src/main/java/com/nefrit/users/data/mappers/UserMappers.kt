package com.nefrit.users.data.mappers

import com.nefrit.core_db.model.PatientLocal
import com.nefrit.common.WoundType
import com.nefrit.users.data.network.model.UserRemote
import com.nefrit.common.Patient
import java.util.*

fun mapPatientToPatientLocal(patient: Patient): PatientLocal {
    return with(patient) {
        PatientLocal(
            id,
            firstPhotoDate.toString(),
            woundType.toString(),
            photoData
        )
    }
}

fun mapPatientLocalToPatient(patient: PatientLocal): Patient {
    return with(patient) {
        Patient(
            id,
            Date(firstPhotoDate),
            WoundType.valueOf(woundType),
            photoData
        )
    }
}

fun mapPatientRemoteToPatient(user: UserRemote): Patient {
    return with(user) {
        Patient(id, firstName, lastName)
    }
}