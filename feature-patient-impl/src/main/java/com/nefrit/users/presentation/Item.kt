package com.nefrit.users.presentation

import com.nefrit.common.Patient

object Item {

    val list = mutableListOf<Patient>(
        Patient(1, "13131", ""),
        Patient(2, "9292982", ""),
        Patient(3, "9292987", ""),
        Patient(4, "21987", ""),
        Patient(5, "3113", "")
    )

    fun addUser(title: String) {
        list.add(0, Patient(0, title, ""))
    }
}