package com.example.ocrstest.di

import com.example.ocrstest.data.OcrResultDao
import android.content.Context
import androidx.room.Room
import com.example.ocrstest.data.OcrDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOcrResultDao(appDatabase: OcrDatabase): OcrResultDao {
        return appDatabase.ocrResultDao()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OcrDatabase {
        return Room.databaseBuilder(
            context,
            OcrDatabase::class.java,
            "ocr_database"
        ).build()
    }
}