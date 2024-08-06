package com.roc.ndkexample.ui.page

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

/**
 *
 * @Author Roc
 * @Date   2024/7/9
 * @Name   CameraCaptureScreen2
 */

@Composable
fun CameraCaptureScreen2(navController: NavController) {
    val context = LocalContext.current

    val surfaceView = remember { SurfaceView(context) }
    val imageBitmap: MutableState<Bitmap?> = remember { mutableStateOf(null) }


    Box(modifier = Modifier.fillMaxSize()) {
        //摄像头预览视图
        AndroidView(factory = {
            surfaceView.apply {
                val camera = Camera.open()
                val params = camera.parameters
                params.previewFormat = ImageFormat.NV21
                params.setPreviewSize(720, 1280)
                camera.parameters = params
                camera.setPreviewDisplay(surfaceView.holder)
                camera.setPreviewCallback { data, camera ->
                    Log.e("长度", "data = $data")
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    imageBitmap.value = bitmap
                }
                camera.startPreview()
            }
        }, modifier = Modifier.fillMaxSize())

        imageBitmap.value?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black.copy(alpha = 0.4f))
            )
        }
    }
}