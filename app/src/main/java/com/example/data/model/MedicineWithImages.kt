package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MedicineWithImages(
    @Embedded val medicine: Medicine,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicineId"
    )
    val images: List<MedicineImage> = emptyList()
) {
    val frontImage: MedicineImage?
        get() = images.firstOrNull { it.imageType == "FRONT" } ?: images.firstOrNull()

    val backImage: MedicineImage?
        get() = images.firstOrNull { it.imageType == "BACK" }

    val additionalImages: List<MedicineImage>
        get() = images.filter { it.imageType != "FRONT" && it.imageType != "BACK" }
}
