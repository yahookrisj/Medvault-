package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DoseHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseHistoryDao {

    @Query("SELECT * FROM dose_history ORDER BY scheduledTime DESC")
    fun getAllHistory(): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE medicineId = :medicineId ORDER BY scheduledTime DESC")
    fun getHistoryForMedicine(medicineId: Long): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY scheduledTime ASC")
    fun getHistoryBetween(startTime: Long, endTime: Long): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime")
    suspend fun getHistoryListBetween(startTime: Long, endTime: Long): List<DoseHistory>

    @Query("SELECT * FROM dose_history WHERE reminderId = :reminderId AND scheduledTime = :scheduledTime LIMIT 1")
    suspend fun getHistoryByReminderAndSchedule(reminderId: Long, scheduledTime: Long): DoseHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseHistory(doseHistory: DoseHistory): Long

    @Query("DELETE FROM dose_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("SELECT COUNT(*) FROM dose_history WHERE status = 'TAKEN'")
    fun getTakenCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dose_history WHERE status = 'MISSED'")
    fun getMissedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dose_history WHERE status = 'SKIPPED'")
    fun getSkippedCount(): Flow<Int>
}
