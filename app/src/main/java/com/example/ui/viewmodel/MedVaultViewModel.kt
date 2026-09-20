package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedVaultApplication
import com.example.data.model.DoseHistory
import com.example.data.model.Medicine
import com.example.data.model.MedicineImage
import com.example.data.model.MedicineWithImages
import com.example.data.model.Reminder
import com.example.data.model.TodayDoseItem
import com.example.data.model.VisualMatchResult
import com.example.util.AlarmScheduler
import com.example.util.FeatureExtractor
import com.example.util.ImageStorageHelper
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

enum class MedicineFilter {
    ALL,
    ACTIVE,
    LOW_STOCK,
    EXPIRED
}

class MedVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MedVaultApplication).repository

    // Date navigation for dashboard
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayDoses: StateFlow<List<TodayDoseItem>> = _selectedCalendar
        .flatMapLatest { cal -> repository.getTodayDoseItems(cal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedicinesWithImages: StateFlow<List<MedicineWithImages>> = repository.allMedicinesWithImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<Reminder>> = repository.activeReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDoseHistory: StateFlow<List<DoseHistory>> = repository.allDoseHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and filters for medicine library
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(MedicineFilter.ALL)
    val selectedFilter: StateFlow<MedicineFilter> = _selectedFilter.asStateFlow()

    val filteredMedicines: StateFlow<List<MedicineWithImages>> = combine(
        allMedicinesWithImages,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        val queryTrimmed = query.trim().lowercase()
        val now = System.currentTimeMillis()

        list.filter { item ->
            val med = item.medicine
            val matchesQuery = queryTrimmed.isEmpty() ||
                med.brandName.lowercase().contains(queryTrimmed) ||
                med.genericName.lowercase().contains(queryTrimmed) ||
                med.dosageForm.lowercase().contains(queryTrimmed) ||
                med.indication.lowercase().contains(queryTrimmed)

            val matchesFilter = when (filter) {
                MedicineFilter.ALL -> true
                MedicineFilter.ACTIVE -> med.expiryDate > now && med.stockQuantity > 0
                MedicineFilter.LOW_STOCK -> med.stockQuantity <= med.lowStockThreshold
                MedicineFilter.EXPIRED -> med.expiryDate <= now
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Visual Identification scan state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResults = MutableStateFlow<List<VisualMatchResult>>(emptyList())
    val scanResults: StateFlow<List<VisualMatchResult>> = _scanResults.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // History filter
    private val _historyFilter = MutableStateFlow("ALL") // ALL, TAKEN, MISSED, SKIPPED
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    // Settings
    private val _lowStockThreshold = MutableStateFlow(5)
    val lowStockThreshold: StateFlow<Int> = _lowStockThreshold.asStateFlow()

    // Dashboard actions
    fun changeDateOffset(offsetDays: Int) {
        val newCal = (_selectedCalendar.value.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        _selectedCalendar.value = newCal
    }

    fun resetToToday() {
        _selectedCalendar.value = Calendar.getInstance()
    }

    fun markDoseTaken(item: TodayDoseItem) {
        viewModelScope.launch {
            repository.recordDoseStatus(
                reminderId = item.reminderId,
                medicineId = item.medicineId,
                scheduledTime = item.scheduledTimeMillis,
                status = "TAKEN"
            )
        }
    }

    fun markDoseSkipped(item: TodayDoseItem) {
        viewModelScope.launch {
            repository.recordDoseStatus(
                reminderId = item.reminderId,
                medicineId = item.medicineId,
                scheduledTime = item.scheduledTimeMillis,
                status = "SKIPPED"
            )
        }
    }

    fun snoozeDose(item: TodayDoseItem) {
        AlarmScheduler.scheduleSnooze(
            context = getApplication(),
            reminderId = item.reminderId,
            medicineId = item.medicineId,
            medicineName = item.medicineBrandName,
            dose = item.dose,
            beforeFood = item.beforeFood,
            snoozeDurationMillis = 10 * 60 * 1000L
        )
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: MedicineFilter) {
        _selectedFilter.value = filter
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun updateStock(medicineId: Long, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(medicineId, newStock.coerceAtLeast(0))
        }
    }

    // Medicine CRUD
    fun saveMedicineWithImages(
        medicine: Medicine,
        frontImageFile: File?,
        backImageFile: File?,
        additionalImageFiles: List<File>,
        reminders: List<Reminder>,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val medicineId = if (medicine.id == 0L) {
                repository.insertMedicine(medicine)
            } else {
                repository.updateMedicine(medicine)
                medicine.id
            }

            // Save front image
            frontImageFile?.let { file ->
                val bitmap = ImageStorageHelper.loadBitmapFromFile(file.absolutePath, 512)
                val embedding = bitmap?.let { FeatureExtractor.extractEmbedding(it) }
                val featureStr = embedding?.let { FeatureExtractor.serializeVector(it) }

                repository.insertImage(
                    MedicineImage(
                        medicineId = medicineId,
                        imagePath = file.absolutePath,
                        imageType = "FRONT",
                        featureVector = featureStr
                    )
                )
            }

            // Save back image
            backImageFile?.let { file ->
                val bitmap = ImageStorageHelper.loadBitmapFromFile(file.absolutePath, 512)
                val embedding = bitmap?.let { FeatureExtractor.extractEmbedding(it) }
                val featureStr = embedding?.let { FeatureExtractor.serializeVector(it) }

                repository.insertImage(
                    MedicineImage(
                        medicineId = medicineId,
                        imagePath = file.absolutePath,
                        imageType = "BACK",
                        featureVector = featureStr
                    )
                )
            }

            // Additional images
            additionalImageFiles.forEach { file ->
                val bitmap = ImageStorageHelper.loadBitmapFromFile(file.absolutePath, 512)
                val embedding = bitmap?.let { FeatureExtractor.extractEmbedding(it) }
                val featureStr = embedding?.let { FeatureExtractor.serializeVector(it) }

                repository.insertImage(
                    MedicineImage(
                        medicineId = medicineId,
                        imagePath = file.absolutePath,
                        imageType = "ADDITIONAL",
                        featureVector = featureStr
                    )
                )
            }

            // Save reminders & schedule alarms
            val savedMedicine = repository.getMedicineById(medicineId) ?: medicine.copy(id = medicineId)
            for (reminder in reminders) {
                val reminderToSave = reminder.copy(medicineId = medicineId)
                val reminderId = repository.insertReminder(reminderToSave)
                AlarmScheduler.scheduleReminder(getApplication(), reminderToSave.copy(id = reminderId), savedMedicine)
            }

            withContext(Dispatchers.Main) {
                onSuccess(medicineId)
            }
        }
    }

    fun deleteMedicine(medicineWithImages: MedicineWithImages) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel alarms for any reminders
            val reminders = repository.getRemindersForMedicine(medicineWithImages.medicine.id).first()
            for (r in reminders) {
                AlarmScheduler.cancelReminder(getApplication(), r.id)
            }
            repository.deleteMedicine(medicineWithImages)
        }
    }

    fun deleteImage(image: MedicineImage) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteImage(image)
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertReminder(reminder)
            val medicine = repository.getMedicineById(reminder.medicineId)
            if (medicine != null) {
                AlarmScheduler.scheduleReminder(getApplication(), reminder.copy(id = id), medicine)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelReminder(getApplication(), reminder.id)
            repository.deleteReminder(reminder)
        }
    }

    // Visual identification workflow
    fun identifyMedicineFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _isScanning.value = true
            _scanError.value = null
            try {
                val allStoredImages = repository.getAllImages()
                if (allStoredImages.isEmpty()) {
                    _scanError.value = "No medicine photos saved yet. Add photos to your medicines in the Library first!"
                    _scanResults.value = emptyList()
                    _isScanning.value = false
                    return@launch
                }

                val allMeds = allMedicinesWithImages.value.map { it.medicine }.associateBy { it.id }

                val matches = FeatureExtractor.findTopMatches(
                    queryBitmap = bitmap,
                    storedImages = allStoredImages,
                    medicinesMap = allMeds,
                    limit = 3
                )

                _scanResults.value = matches
                if (matches.isEmpty()) {
                    _scanError.value = "No close match found in your saved medicines."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _scanError.value = "Error comparing photo: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanResults() {
        _scanResults.value = emptyList()
        _scanError.value = null
    }

    // PDF Export
    suspend fun exportPdfReport(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val medicines = allMedicinesWithImages.value
            val reminders = activeReminders.value
            val history = allDoseHistory.value
            val file = PdfReportGenerator.generateMedicationReport(
                context = getApplication(),
                medicines = medicines,
                reminders = reminders,
                history = history
            )
            Result.success(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
