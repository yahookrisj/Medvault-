package com.example.ui.screens.medicines

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Medicine
import com.example.data.model.MedicineWithImages
import com.example.data.model.Reminder
import com.example.ui.viewmodel.MedVaultViewModel
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    viewModel: MedVaultViewModel,
    medicineId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var brandName by remember { mutableStateOf("") }
    var genericName by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf("") }
    var dosageForm by remember { mutableStateOf("Tablet") }
    var manufacturer by remember { mutableStateOf("") }
    var indication by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expiryDate by remember {
        val nextYear = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }
        mutableLongStateOf(nextYear.timeInMillis)
    }
    var batchNumber by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableIntStateOf(30) }
    var lowStockThreshold by remember { mutableIntStateOf(5) }

    var frontImageFile by remember { mutableStateOf<File?>(null) }
    var backImageFile by remember { mutableStateOf<File?>(null) }
    val additionalImageFiles = remember { mutableStateListOf<File>() }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dosage Form Dropdown
    val dosageForms = listOf("Tablet", "Capsule", "Syrup", "Injection", "Ointment", "Drops", "Inhaler", "Powder", "Patch", "Other")
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Load existing medicine if editing
    LaunchedEffect(medicineId) {
        if (medicineId != null && medicineId > 0) {
            val medWithImages = viewModel.allMedicinesWithImages.value.firstOrNull { it.medicine.id == medicineId }
            if (medWithImages != null) {
                val med = medWithImages.medicine
                brandName = med.brandName
                genericName = med.genericName
                strength = med.strength
                dosageForm = med.dosageForm
                manufacturer = med.manufacturer ?: ""
                indication = med.indication
                notes = med.notes ?: ""
                expiryDate = med.expiryDate
                batchNumber = med.batchNumber ?: ""
                stockQuantity = med.stockQuantity
                lowStockThreshold = med.lowStockThreshold

                medWithImages.frontImage?.let { frontImageFile = File(it.imagePath) }
                medWithImages.backImage?.let { backImageFile = File(it.imagePath) }
                additionalImageFiles.clear()
                medWithImages.additionalImages.forEach { additionalImageFiles.add(File(it.imagePath)) }
            }
        }
    }

    // Photo pickers & cameras
    var targetPhotoType by remember { mutableStateOf("FRONT") } // FRONT, BACK, ADDITIONAL

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val path = ImageStorageHelper.saveUriToFile(context, uri)
                if (path != null) {
                    val file = File(path)
                    when (targetPhotoType) {
                        "FRONT" -> frontImageFile = file
                        "BACK" -> backImageFile = file
                        "ADDITIONAL" -> additionalImageFiles.add(file)
                    }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                val path = ImageStorageHelper.saveBitmapToFile(context, bitmap)
                val file = File(path)
                when (targetPhotoType) {
                    "FRONT" -> frontImageFile = file
                    "BACK" -> backImageFile = file
                    "ADDITIONAL" -> additionalImageFiles.add(file)
                }
            }
        }
    }

    // Expiry Date Picker Dialog
    val calendar = Calendar.getInstance().apply { timeInMillis = expiryDate }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val picked = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            expiryDate = picked.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (medicineId != null && medicineId > 0) "Edit Medicine" else "Add New Medicine",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (brandName.isBlank()) {
                                errorMessage = "Please enter Brand Name"
                                return@Button
                            }
                            if (genericName.isBlank()) {
                                errorMessage = "Please enter Generic Name"
                                return@Button
                            }
                            if (strength.isBlank()) {
                                errorMessage = "Please enter Strength (e.g., 500 mg)"
                                return@Button
                            }

                            isSaving = true
                            errorMessage = null

                            val medicine = Medicine(
                                id = medicineId ?: 0L,
                                brandName = brandName.trim(),
                                genericName = genericName.trim(),
                                strength = strength.trim(),
                                dosageForm = dosageForm,
                                manufacturer = manufacturer.trim().ifBlank { null },
                                indication = indication.trim(),
                                notes = notes.trim().ifBlank { null },
                                expiryDate = expiryDate,
                                batchNumber = batchNumber.trim().ifBlank { null },
                                stockQuantity = stockQuantity,
                                lowStockThreshold = lowStockThreshold
                            )

                            viewModel.saveMedicineWithImages(
                                medicine = medicine,
                                frontImageFile = frontImageFile,
                                backImageFile = backImageFile,
                                additionalImageFiles = additionalImageFiles.toList(),
                                reminders = emptyList(),
                                onSuccess = { id ->
                                    isSaving = false
                                    onSaved(id)
                                }
                            )
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_medicine_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Photo Capture Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Medicine Photos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Used for visual identification and quick reference. Front and back photos recommended.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Front Photo Slot
                            PhotoSlot(
                                label = "Front Photo",
                                file = frontImageFile,
                                onCameraClick = {
                                    targetPhotoType = "FRONT"
                                    cameraLauncher.launch(null)
                                },
                                onGalleryClick = {
                                    targetPhotoType = "FRONT"
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemoveClick = { frontImageFile = null },
                                modifier = Modifier.weight(1f)
                            )

                            // Back Photo Slot
                            PhotoSlot(
                                label = "Back Photo",
                                file = backImageFile,
                                onCameraClick = {
                                    targetPhotoType = "BACK"
                                    cameraLauncher.launch(null)
                                },
                                onGalleryClick = {
                                    targetPhotoType = "BACK"
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemoveClick = { backImageFile = null },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Additional Photos Row
                        if (additionalImageFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Additional Photos (${additionalImageFiles.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(additionalImageFiles) { file ->
                                    Box(modifier = Modifier.size(72.dp)) {
                                        AsyncImage(
                                            model = file,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        IconButton(
                                            onClick = { additionalImageFiles.remove(file) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                targetPhotoType = "ADDITIONAL"
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Additional Photo")
                        }
                    }
                }
            }

            // Basic Information Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // Brand Name
                        OutlinedTextField(
                            value = brandName,
                            onValueChange = { brandName = it },
                            label = { Text("Brand Name *") },
                            placeholder = { Text("e.g. Tylenol, Amoxil, Lipitor") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("brand_name_input")
                        )

                        // Generic Name
                        OutlinedTextField(
                            value = genericName,
                            onValueChange = { genericName = it },
                            label = { Text("Generic Name *") },
                            placeholder = { Text("e.g. Paracetamol, Amoxicillin, Atorvastatin") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generic_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Strength
                            OutlinedTextField(
                                value = strength,
                                onValueChange = { strength = it },
                                label = { Text("Strength *") },
                                placeholder = { Text("500 mg, 10 ml") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("strength_input")
                            )

                            // Dosage Form Dropdown
                            ExposedDropdownMenuBox(
                                expanded = isDropdownExpanded,
                                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = dosageForm,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Form") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false }
                                ) {
                                    dosageForms.forEach { form ->
                                        DropdownMenuItem(
                                            text = { Text(form) },
                                            onClick = {
                                                dosageForm = form
                                                isDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Indication / Purpose
                        OutlinedTextField(
                            value = indication,
                            onValueChange = { indication = it },
                            label = { Text("Indication / Purpose") },
                            placeholder = { Text("e.g. Pain relief, Blood pressure, Antibiotic") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Manufacturer
                        OutlinedTextField(
                            value = manufacturer,
                            onValueChange = { manufacturer = it },
                            label = { Text("Manufacturer (Optional)") },
                            placeholder = { Text("e.g. Pfizer, GSK, Novartis") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Inventory & Expiry Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Inventory & Expiry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Quantity in Stock
                            OutlinedTextField(
                                value = stockQuantity.toString(),
                                onValueChange = { stockQuantity = it.toIntOrNull() ?: 0 },
                                label = { Text("Current Stock") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stock_input")
                            )

                            // Low Stock Alert Threshold
                            OutlinedTextField(
                                value = lowStockThreshold.toString(),
                                onValueChange = { lowStockThreshold = it.toIntOrNull() ?: 5 },
                                label = { Text("Alert Threshold") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Expiry Date Selector
                        val expiryFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        OutlinedTextField(
                            value = expiryFormat.format(Date(expiryDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expiry Date") },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                        )

                        // Batch Number
                        OutlinedTextField(
                            value = batchNumber,
                            onValueChange = { batchNumber = it },
                            label = { Text("Batch Number (Optional)") },
                            placeholder = { Text("e.g. B-984210") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Notes
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Special Notes / Instructions") },
                            placeholder = { Text("e.g. Take with water, store in cool place") },
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun PhotoSlot(
    label: String,
    file: File?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.height(140.dp)
    ) {
        if (file != null && file.exists()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = file,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomStart = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Camera / Gallery",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                )
            }
        }
    }
}
