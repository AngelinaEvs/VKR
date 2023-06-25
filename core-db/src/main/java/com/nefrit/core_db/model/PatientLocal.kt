package com.nefrit.core_db.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nefrit.common.PhotoData

@Entity(tableName = "patients")
data class PatientLocal(
    @PrimaryKey val id: Int,
    val firstPhotoDate: String,
    val woundType: String,
    @Embedded val photoData: PhotoData
)