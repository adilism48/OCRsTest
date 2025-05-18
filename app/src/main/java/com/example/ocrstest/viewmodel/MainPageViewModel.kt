package com.example.ocrstest.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrstest.data.OcrResultDao
import com.example.ocrstest.data.OcrResultStore
import com.example.ocrstest.model.OcrResult
import com.example.ocrstest.utils.prepareTesseractData
import com.example.ocrstest.utils.uriToBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appContext: Application,
    private val dao: OcrResultDao
) : AndroidViewModel(appContext) {

    var imageUri by mutableStateOf<Uri?>(null)
    var recognizedText by mutableStateOf("")
    var recognizingStatus by mutableStateOf("")
    var selectedEngine by mutableStateOf("ML Kit")
    var selectedEngineSize by mutableStateOf("~20-30MB")
    var isLoading by mutableStateOf(false)
    private val _navigateToResult = MutableStateFlow<String?>(null)
    val navigateToResult: StateFlow<String?> = _navigateToResult

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val tessBaseAPI: TessBaseAPI by lazy {
        val tessDir = prepareTesseractData(appContext)
        TessBaseAPI().apply { init(tessDir.absolutePath, "eng") }
    }

    fun onImageSelected(uri: Uri?) {
        imageUri = uri
        uri?.let { processImage(it) }
    }

    private fun processImage(uri: Uri) {
        viewModelScope.launch {
            isLoading = true
            recognizingStatus = "Обработка с $selectedEngine..."

            try {
                if (selectedEngine == "ML Kit") {
                    val image = InputImage.fromFilePath(appContext, uri)
                    val startTime = System.currentTimeMillis()
                    val result = textRecognizer.process(image).await()
                    val recognized = result.text
                    val timeTaken = System.currentTimeMillis() - startTime
                    selectedEngineSize = "~20-30MB"

                    saveResult(uri, recognized, selectedEngine, timeTaken, selectedEngineSize)
                } else {
                    val bitmap = withContext(Dispatchers.IO) {
                        uriToBitmap(appContext, uri)
                    }

                    if (bitmap != null) {
                        val startTime = System.currentTimeMillis()

                        val recognized = withContext(Dispatchers.IO) {
                            tessBaseAPI.setImage(bitmap)
                            tessBaseAPI.utF8Text ?: "No text found"
                        }

                        val timeTaken = System.currentTimeMillis() - startTime
                        selectedEngineSize = "~22MB"
                        saveResult(uri, recognized, selectedEngine, timeTaken, selectedEngineSize)
                    } else {
                        recognizingStatus = "Error loading image"
                    }
                }
            } catch (e: Exception) {
                recognizingStatus = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun saveResult(uri: Uri, text: String, engine: String, time: Long, engineSize: String) {
        recognizedText = text
        OcrResultStore.imageUri = uri
        OcrResultStore.recognizedText = text
        recognizingStatus = "Завершено"

        viewModelScope.launch(Dispatchers.IO) {
            val result = OcrResult(
                ocrEngine = engine,
                recognizedText = text,
                timestamp = System.currentTimeMillis(),
                durationMillis = time,
                engineSize = engineSize
            )
            dao.insert(result)
            _navigateToResult.value = "$selectedEngine/$time/$engineSize"
        }
    }

    fun onNavigated() {
        _navigateToResult.value = null
        recognizingStatus = ""
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
        tessBaseAPI.recycle()
    }
}