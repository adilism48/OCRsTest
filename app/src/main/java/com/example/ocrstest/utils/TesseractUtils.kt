package com.example.ocrstest.utils

import android.content.Context
import java.io.File

fun prepareTesseractData(context: Context): File {
    val tessDataDir = File(context.filesDir, "tessdata")
    if (!tessDataDir.exists()) tessDataDir.mkdirs()

    val trainedDataFile = File(tessDataDir, "eng.traineddata")
    if (!trainedDataFile.exists()) {
        context.assets.open("tessdata/eng.traineddata").use { input ->
            trainedDataFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    return context.filesDir
}