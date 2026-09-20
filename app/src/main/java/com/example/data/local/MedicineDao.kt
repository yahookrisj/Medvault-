package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Medicine
import com.example.data.model.MedicineImage
import com.example.data.model.MedicineWithImages
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Query("SELECT * FROM medicines ORDER BY brandName ASC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Transaction
    @Query("SELECT * FROM medicines ORDER BY brandName ASC")
    fun getAllMedicinesWithImages(): Flow<List<MedicineWithImages>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE id = :id")
    fun getMedicineWithImages(id: Long): Flow<MedicineWithImages?>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): Medicine?

    @Query("SELECT * FROM medicines WHERE brandName LIKE '%' || :query || '%' OR genericName LIKE '%' || :query || '%' OR dosageForm LIKE '%' || :query || '%'")
    fun searchMedicines(query: String): Flow<List<Medicine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("UPDATE medicines SET stockQuantity = CASE WHEN stockQuantity >= :amount THEN stockQuantity - :amount ELSE 0 END WHERE id = :medicineId")
    suspend fun decrementStock(medicineId: Long, amount: Int = 1)

    @Query("UPDATE medicines SET stockQuantity = :newStock WHERE id = :medicineId")
    suspend fun updateStock(medicineId: Long, newStock: Int)

    // Image operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: MedicineImage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<MedicineImage>)

    @Query("SELECT * FROM medicine_images WHERE medicineId = :medicineId")
    suspend fun getImagesForMedicine(medicineId: Long): List<MedicineImage>

    @Query("SELECT * FROM medicine_images")
    suspend fun getAllImages(): List<MedicineImage>

    @Query("UPDATE medicine_images SET featureVector = :vector WHERE id = :imageId")
    suspend fun updateImageFeatureVector(imageId: Long, vector: String)

    @Delete
    suspend fun deleteImage(image: MedicineImage)

    @Query("DELETE FROM medicine_images WHERE medicineId = :medicineId")
    suspend fun deleteImagesForMedicine(medicineId: Long)
}
