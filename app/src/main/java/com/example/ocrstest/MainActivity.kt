package com.example.ocrstest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ocrstest.ui.theme.OCRsTestTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OCRsTestTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "main") {
                    composable("main") {
                        MainPage(navController)
                    }
                    composable(
                        "result/{ocrEngine}/{timeMillis}",
                        arguments = listOf(
                            navArgument("ocrEngine") { type = NavType.StringType },
                            navArgument("timeMillis") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val ocrEngine =
                            backStackEntry.arguments?.getString("ocrEngine") ?: "Unknown"
                        val timeMillis = backStackEntry.arguments?.getLong("timeMillis") ?: 0L
                        ResultPage(navController, ocrEngine, timeMillis)
                    }
                    composable("history") {
                        HistoryScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun MainPage(navController: NavController) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("Scanned text will appear here..") }
    var timeTaken by remember { mutableLongStateOf(0) }
    var recognizingStatus by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val ocrEngines = listOf("ML Kit", "Tesseract")
    var selectedEngine by remember { mutableStateOf(ocrEngines[0]) }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val tessBaseAPI = remember {
        val tessDir = prepareTesseractData(context)
        TessBaseAPI().apply {
            init(tessDir.absolutePath, "eng")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            textRecognizer.close()
            tessBaseAPI.recycle()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri = tempCameraUri
            } else {
                recognizingStatus = "Photo capture cancelled or failed"
                isLoading = false
            }
        }
    )

    val selectImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                imageUri = uri
            } else {
                recognizingStatus = "Image selection cancelled"
                isLoading = false
            }
        }

    LaunchedEffect(imageUri, selectedEngine) {
        val currentUri = imageUri
        val db = OcrDatabase.getDatabase(context)
        val dao = db.ocrResultDao()

        if (currentUri != null) {
            isLoading = true
            recognizingStatus = "Processing with $selectedEngine..."

            try {
                if (selectedEngine == "ML Kit") {
                    val image = InputImage.fromFilePath(context, currentUri)
                    val startTime = System.currentTimeMillis()

                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val recognized = visionText.text
                            val timeTaken = System.currentTimeMillis() - startTime

                            OcrResultStore.imageUri = currentUri
                            OcrResultStore.recognizedText = recognized

                            val result = OcrResult(
                                ocrEngine = selectedEngine,
                                recognizedText = recognized,
                                timestamp = System.currentTimeMillis(),
                                durationMillis = timeTaken
                            )

                            CoroutineScope(Dispatchers.IO).launch {
                                dao.insert(result)
                            }

                            navController.navigate("result/MLKit/$timeTaken")
                        }
                } else {
                    val bitmap = uriToBitmap(context, currentUri)
                    val startTime = System.currentTimeMillis()

                    withContext(Dispatchers.IO) {
                        if (bitmap != null) {
                            tessBaseAPI.setImage(bitmap)
                            recognizedText = tessBaseAPI.utF8Text ?: "No text found"

                            timeTaken = System.currentTimeMillis() - startTime

                            val result = OcrResult(
                                ocrEngine = selectedEngine,
                                recognizedText = recognizedText,
                                timestamp = System.currentTimeMillis(),
                                durationMillis = System.currentTimeMillis() - startTime
                            )

                            dao.insert(result)

                            OcrResultStore.imageUri = imageUri
                            OcrResultStore.recognizedText = recognizedText

                            withContext(Dispatchers.Main) {
                                navController.navigate("result/Tesseract/$timeTaken")
                            }
                        } else {
                            recognizingStatus = "Error loading image for Tesseract"
                        }
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                recognizingStatus = "Error: ${e.message}"
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "OCRs Test",
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
                .padding(bottom = 32.dp),
            fontSize = 36.sp
        )

        DropdownWithField(ocrEngines, selectedEngine) {
            selectedEngine = it
        }

        Button(onClick = { navController.navigate("history") }) {
            Text("View History")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                val file = try {
                    createImageFile(context)
                } catch (ex: IOException) {
                    recognizingStatus = "Error creating file: ${ex.message}"
                    isLoading = false
                    return@Button
                }

                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            }) {
                Text("Scan from camera")
            }
            Button(onClick = {
                isLoading = true
                recognizingStatus = "Processing..."
                selectImageLauncher.launch("image/*")
            }) {
                Text("Scan from gallery")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        }

        Text(
            modifier = Modifier
                .padding(8.dp)
                .padding(8.dp),
            text = recognizingStatus
        )
    }
}

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

private fun createImageFile(context: Context): File {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir: File? = context.getExternalFilesDir(null)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: IOException) {
        Log.e("OCRCompose", "Error converting URI to Bitmap", e)
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownWithField(items: List<String>, selected: String, onSelected: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded }
        ) {
            TextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }
            )

            ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                items.forEach { text ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelected(text)
                            isExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}


