package com.example.ocrstest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LazyColumn(modifier = Modifier.padding(16.dp).safeDrawingPadding()) {
        items(results) { item ->
            Column(modifier = Modifier.padding(8.dp)) {
                Text("OCR: ${item.ocrEngine}", fontSize = 16.sp)
                Text("Time: ${Date(item.timestamp)}")
                Text("Duration: ${item.durationMillis} ms")
                Text("Text: ${item.recognizedText.take(100)}")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
