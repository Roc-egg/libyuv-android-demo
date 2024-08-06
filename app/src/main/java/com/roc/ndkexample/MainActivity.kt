package com.roc.ndkexample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roc.libyuv.YuvUtils
import com.roc.ndkexample.ui.page.CameraCaptureScreen
import com.roc.ndkexample.ui.page.CameraCaptureScreen2
import com.roc.ndkexample.ui.theme.NdkExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NdkExampleTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Route.CAMERA,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        composable(Route.CAMERA) {
                            CameraCaptureScreen(navController = navController)
                        }

                    }
                }
            }
        }
        Log.d("测试", "libyuv版本号 = ${YuvUtils.libYuvVersion()}")
    }
}

object Route {
    const val CAMERA = "CAMERA"
}