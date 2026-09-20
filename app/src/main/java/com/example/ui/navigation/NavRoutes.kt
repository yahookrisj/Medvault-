package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Today")
    object Medicines : Screen("medicines", "Medicines")
    object Scan : Screen("scan", "Visual Scan")
    object History : Screen("history", "History")
    object Settings : Screen("settings", "Settings")

    object MedicineDetail : Screen("medicine_detail/{medicineId}", "Medicine Details") {
        fun createRoute(medicineId: Long) = "medicine_detail/$medicineId"
    }

    object AddMedicine : Screen("add_medicine", "Add Medicine")

    object EditMedicine : Screen("edit_medicine/{medicineId}", "Edit Medicine") {
        fun createRoute(medicineId: Long) = "edit_medicine/$medicineId"
    }
}
