package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.DoseHistory
import com.example.data.model.MedicineWithImages
import com.example.data.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    suspend fun generateMedicationReport(
        context: Context,
        medicines: List<MedicineWithImages>,
        reminders: List<Reminder>,
        history: List<DoseHistory>
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(13, 148, 136) // Emerald primary
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(2, 132, 199) // Medical blue
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }

        var y = 40f

        // Document Title
        canvas.drawText("MedVault – Personal Medication Record", 36f, y, titlePaint)
        y += 18f
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        canvas.drawText("Generated on: $dateStr • Offline Verified Record", 36f, y, subtitlePaint)
        y += 24f

        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 18f

        // Section: Summary Statistics
        canvas.drawText("1. ADHERENCE SUMMARY", 36f, y, sectionPaint)
        y += 16f

        val totalLogged = history.size
        val takenCount = history.count { it.status == "TAKEN" }
        val missedCount = history.count { it.status == "MISSED" }
        val skippedCount = history.count { it.status == "SKIPPED" }
        val adherenceRate = if (totalLogged > 0) (takenCount * 100 / totalLogged) else 100

        canvas.drawRect(36f, y, 559f, y + 42f, headerBgPaint)
        canvas.drawText("Total Recorded: $totalLogged", 46f, y + 18f, boldTextPaint)
        canvas.drawText("Taken: $takenCount", 180f, y + 18f, textPaint)
        canvas.drawText("Missed: $missedCount", 280f, y + 18f, textPaint)
        canvas.drawText("Skipped: $skippedCount", 380f, y + 18f, textPaint)
        canvas.drawText("Adherence Rate: $adherenceRate%", 46f, y + 34f, boldTextPaint)
        y += 56f

        // Section: Medicine Database
        canvas.drawText("2. REGISTERED MEDICINES (${medicines.size})", 36f, y, sectionPaint)
        y += 16f

        val expiryDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (item in medicines) {
            if (y > 780f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            val med = item.medicine
            val expiryStr = expiryDateFormat.format(Date(med.expiryDate))
            val isExpired = med.expiryDate < System.currentTimeMillis()

            canvas.drawText("• ${med.brandName} (${med.genericName})", 36f, y, boldTextPaint)
            y += 13f

            val detailsLine = "Strength: ${med.strength} | Form: ${med.dosageForm} | Stock: ${med.stockQuantity} units | Expiry: $expiryStr ${if (isExpired) "[EXPIRED]" else ""}"
            canvas.drawText(detailsLine, 46f, y, textPaint)
            y += 13f

            if (med.indication.isNotBlank()) {
                canvas.drawText("Purpose: ${med.indication}", 46f, y, textPaint)
                y += 13f
            }

            if (!med.notes.isNullOrBlank()) {
                canvas.drawText("Notes: ${med.notes}", 46f, y, textPaint)
                y += 13f
            }

            y += 6f
        }

        y += 12f
        if (y > 750f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 40f
        }

        // Section: Active Schedules
        canvas.drawText("3. ACTIVE REMINDER SCHEDULES (${reminders.size})", 36f, y, sectionPaint)
        y += 16f

        val medicineNameMap = medicines.associate { it.medicine.id to it.medicine.brandName }

        for (reminder in reminders) {
            if (y > 780f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            val medName = medicineNameMap[reminder.medicineId] ?: "Unknown Medicine"
            val foodNote = if (reminder.beforeFood) "Before Food" else "After Food"
            canvas.drawText("• $medName – Dose: ${reminder.dose} | Frequency: ${reminder.scheduleType}", 36f, y, boldTextPaint)
            y += 13f
            canvas.drawText("  Time(s): ${reminder.reminderTime} | Guideline: $foodNote", 36f, y, textPaint)
            y += 16f
        }

        document.finishPage(page)

        val reportDir = File(context.filesDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val reportFile = File(reportDir, "MedVault_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(reportFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        reportFile
    }
}
