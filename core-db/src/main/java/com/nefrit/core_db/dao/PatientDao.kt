package com.nefrit.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nefrit.common.WoundType
import com.nefrit.core_db.model.PatientLocal
import io.reactivex.Single

@Dao
abstract class PatientDao {

    @Query("select * from patients")
    abstract fun getPatients(): Single<List<PatientLocal>>

    @Query("select * from patients where id = :id")
    abstract fun getPatient(id: Int): Single<PatientLocal>

    @Query("select * from patients where woundType = :woundType")
    abstract fun getPatientsByClass(woundType: String): Single<List<PatientLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(patients: List<PatientLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(patient: PatientLocal): Long
}