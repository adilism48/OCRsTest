package com.example.ocrstest.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocrstest.data.OcrDatabase
import com.example.ocrstest.model.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var results by remember { mutableStateOf<List<OcrResult>>(emptyList()) }

    LaunchedEffect(true) {
        val dao = OcrDatabase.getDatabase(context).ocrResultDao()
        val data = withContext(Dispatchers.IO) {
            dao.getAllResults()
        }
        results = data
    }

    LazyColumn(modifier = Modifier.safeDrawingPadding()) {
        items(results) { item ->
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Модель: ${item.ocrEngine}", fontSize = 16.sp)
                    Text("Размер: ${item.engineSize}", fontSize = 16.sp)
                    Text("Сохранен: ${Date(item.timestamp)}")
                    Text("Время: ${item.durationMillis} ms")
                    Text("Текст: ${item.recognizedText.take(100)}")
                }
            }
        }
    }
}
