package com.nefrit.common

import java.util.*

data class Patient(
    val id: Int,
    val firstPhotoDate: Date,
    val woundType: WoundType,
    val photoData: PhotoData
)

data class NewPatient(
    val id: Int,
    val photoData: PhotoData
)

data class PatientDetails(
    val standart: List<Pair<Double, Double>>,
    val byClass: List<Pair<Double, Double>>,
    val dynamics: List<Pair<Double, Double>>,
    val photoData: PhotoData
)