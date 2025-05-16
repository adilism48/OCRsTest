package com.example.ocrstest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ocrstest.model.OcrResult

@Database(entities = [OcrResult::class], version = 1)
abstract class OcrDatabase : RoomDatabase() {
    abstract fun ocrResultDao(): OcrResultDao

    companion object {
        @Volatile
        private var INSTANCE: OcrDatabase? = null

        fun getDatabase(context: Context): OcrDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OcrDatabase::class.java,
                    "ocr_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}