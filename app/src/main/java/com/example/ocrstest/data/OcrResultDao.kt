package com.example.ocrstest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ocrstest.model.OcrResult

@Dao
interface OcrResultDao {
    @Insert
    suspend fun insert(result: OcrResult)

    @Query("SELECT * FROM OcrResult ORDER BY timestamp DESC")
    fun getAllResults(): List<OcrResult>
}