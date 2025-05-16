package com.example.ocrstest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ocrstest.ui.pages.HistoryScreen
import com.example.ocrstest.ui.pages.MainPage
import com.example.ocrstest.ui.pages.ResultPage
import com.example.ocrstest.ui.theme.OCRsTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OCRsTestTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "main") {
                    composable("main") { MainPage(navController) }
                    composable(
                        "result/{ocrEngine}/{timeMillis}",
                        arguments = listOf(
                            navArgument("ocrEngine") { type = NavType.StringType },
                            navArgument("timeMillis") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val ocrEngine = backStackEntry.arguments?.getString("ocrEngine") ?: "Unknown"
                        val timeMillis = backStackEntry.arguments?.getLong("timeMillis") ?: 0L
                        ResultPage(navController, ocrEngine, timeMillis)
                    }
                    composable("history") { HistoryScreen() }
                }
            }
        }
    }
}


