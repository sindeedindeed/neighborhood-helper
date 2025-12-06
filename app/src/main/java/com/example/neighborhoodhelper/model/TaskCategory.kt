package com.example.neighborhoodhelper.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TaskCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    ALL("All", Icons.Default.List, Color(0xFF9E9E9E)),
    GROCERIES("Groceries", Icons.Default.ShoppingCart, Color(0xFF4CAF50)),
    MOVING("Moving", Icons.Default.LocalShipping, Color(0xFF2196F3)),
    REPAIRS("Repairs", Icons.Default.Build, Color(0xFFFF9800)),
    PET_CARE("Pet Care", Icons.Default.Pets, Color(0xFFE91E63)),
    ELDERLY_CARE("Elderly Care", Icons.Default.Favorite, Color(0xFF9C27B0)),
    CHILDCARE("Childcare", Icons.Default.ChildCare, Color(0xFFFFEB3B)),
    CLEANING("Cleaning", Icons.Default.CleaningServices, Color(0xFF00BCD4)),
    GARDENING("Gardening", Icons.Default.Yard, Color(0xFF8BC34A)),
    TRANSPORTATION("Ride Share", Icons.Default.DirectionsCar, Color(0xFF3F51B5)),
    TECH_HELP("Tech Help", Icons.Default.Computer, Color(0xFF607D8B)),
    COOKING("Cooking", Icons.Default.Restaurant, Color(0xFFFF5722)),
    OTHER("Other", Icons.Default.MoreHoriz, Color(0xFF795548));

    companion object {
        fun fromString(value: String): TaskCategory {
            return entries.find { it.name == value } ?: OTHER
        }
    }
}
