package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicine_images",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicineId")]
)
data class MedicineImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val imagePath: String,
    val imageType: String, // FRONT, BACK, ADDITIONAL
    val featureVector: String? = null // Comma-separated normalized embedding floats for quick visual matching
)
