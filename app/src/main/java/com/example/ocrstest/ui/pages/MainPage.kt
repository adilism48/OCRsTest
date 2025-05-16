package com.example.ocrstest.ui.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.ocrstest.utils.createImageFile
import com.example.ocrstest.viewmodel.MainViewModel

@Composable
fun MainPage(navController: NavController, viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) viewModel.onImageSelected(viewModel.imageUri)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onImageSelected(uri)
    }

    val navigateEvent by viewModel.navigateToResult.collectAsState()

    LaunchedEffect(navigateEvent) {
        navigateEvent?.let { route ->
            navController.navigate("result/$route")
            viewModel.onNavigated()
        }
    }

    Column(
        modifier = Modifier.safeDrawingPadding().fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("OCRs Test", fontSize = 36.sp, modifier = Modifier.padding(bottom = 32.dp))

        DropdownWithField(listOf("ML Kit", "Tesseract"), viewModel.selectedEngine) {
            viewModel.selectedEngine = it
        }

        Button(onClick = { navController.navigate("history") }) {
            Text("View History")
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                val file = createImageFile(context)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                viewModel.imageUri = uri
                cameraLauncher.launch(uri)
            }) {
                Text("Scan from camera")
            }
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Scan from gallery")
            }
        }

        if (viewModel.isLoading) CircularProgressIndicator(Modifier.padding(16.dp))
        Text(viewModel.recognizingStatus)
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