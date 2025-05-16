package com.example.ocrstest.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OcrResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ocrEngine: String,
    val recognizedText: String,
    val timestamp: Long,
    val durationMillis: Long
)