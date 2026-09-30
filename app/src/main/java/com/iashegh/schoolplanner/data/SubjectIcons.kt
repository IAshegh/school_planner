package com.iashegh.schoolplanner.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

object SubjectIcons {
    val all: Map<String, ImageVector> = linkedMapOf(
        "star" to Icons.Filled.Star,
        "book" to Icons.Filled.MenuBook,
        "math" to Icons.Filled.Calculate,
        "science" to Icons.Filled.Science,
        "art" to Icons.Filled.Brush,
        "music" to Icons.Filled.MusicNote,
        "sport" to Icons.Filled.SportsSoccer,
        "language" to Icons.Filled.Language,
        "world" to Icons.Filled.Public,
        "computer" to Icons.Filled.Computer,
    )

    fun get(key: String): ImageVector = all[key] ?: Icons.Filled.Star

    val palette = listOf(
        0xFFEF5350.toInt(), 0xFFFF7043.toInt(), 0xFFFFCA28.toInt(), 0xFF66BB6A.toInt(),
        0xFF26C6DA.toInt(), 0xFF42A5F5.toInt(), 0xFF7E57C2.toInt(), 0xFFEC407A.toInt(),
    )
}
