package com.roc.ndkexample.ui.page

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.roc.libyuv.YuvUtils
import com.roc.ndkexample.R
import com.roc.ndkexample.ui.ext.createImageAnalysisUseCase
import com.roc.ndkexample.utils.CameraUtils
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


var time: Long = 0L

/**开启单例线程池*/
val singleThreadPool = Executors.newSingleThreadExecutor()

/**
 * 摄像头捕获数据页面
 * @Author Roc
 * @Date   2024/7/2
 * @Name   CameraCaptureScreen
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCaptureScreen(
    navController: NavController
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    //权限状态控制
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )
    //camera预览视图
    val previewView: PreviewView = remember { PreviewView(context) }
    //摄像头图像分析器
    val imageAnalysis: MutableState<ImageAnalysis?> = remember { mutableStateOf(null) }
    val cameraSelector: MutableState<CameraSelector> =
        remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    val imageAnalysisStarted: MutableState<Boolean> = remember { mutableStateOf(false) }
    //获取一帧 实际工程中不要这么做
    val mTakeOneYuv: MutableState<Boolean> = remember { mutableStateOf(true) }

    val imageBitmap: MutableState<Bitmap?> = remember { mutableStateOf(null) }

//    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        //触发申请权限
        permissionState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(previewView) {
        imageAnalysis.value = context.createImageAnalysisUseCase(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = cameraSelector.value,
            previewView = previewView
        )
    }

    if (permissionState.allPermissionsGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            //摄像头预览视图
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            IconButton(
                onClick = {
                    if (!imageAnalysisStarted.value) {
                        imageAnalysis.value?.let { imageAnalysis ->
                            imageAnalysisStarted.value = true
                            imageAnalysis.setAnalyzer(singleThreadPool) { imageProxy ->
                                if (mTakeOneYuv.value) {
                                    when (imageProxy.format) {
                                        ImageFormat.YUV_420_888 -> {

                                            imageProxy.image?.apply {
                                                //用来计算消耗时间
                                                time = System.currentTimeMillis()

                                                //创建用来接收处理后的ARGB数据
                                                val outData = ByteArray(width * height * 4)
                                                //android420做变化（转为I420，做镜像，转为ABGR(对应android的ARGB)）
                                                YuvUtils.android420Rotate(
                                                    width = width,
                                                    height = height,
                                                    yPlane = planes[0].buffer,
                                                    yRowStride = planes[0].rowStride,
                                                    uPlane = planes[1].buffer,
                                                    uRowStride = planes[1].rowStride,
                                                    vPlane = planes[2].buffer,
                                                    vRowStride = planes[2].rowStride,
                                                    uPixelStride = planes[1].pixelStride,
                                                    outData = outData,
                                                    90
                                                )
                                                Log.e(
                                                    "消耗时间",
                                                    "${Thread.currentThread()}  android420Rotate - time = ${System.currentTimeMillis() - time}"
                                                )
                                                time = System.currentTimeMillis()
                                                //将转换后的ARGB数据转换成Bitmap
                                                val bitmapImageFromYUV =
                                                    CameraUtils.getBitmapImageFromYUV2(
                                                        data = outData,
                                                        width = width,
                                                        height = height
                                                    )
                                                Log.e(
                                                    "消耗时间",
                                                    "${Thread.currentThread()}  getBitmapImageFromYUV2 - time = ${System.currentTimeMillis() - time}"
                                                )
                                                //更新Image刷新数据
                                                imageBitmap.value = bitmapImageFromYUV
                                            }
                                        }

                                        else -> {//其他格式图像

                                        }
                                    }
                                }
                                imageProxy.close()
                            }
                        }
                    } else {
                        imageAnalysisStarted.value = false
                        mTakeOneYuv.value = true
                        imageAnalysis.value?.clearAnalyzer()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Icon(
                    painter = painterResource(if (imageAnalysisStarted.value) R.drawable.outline_stop_circle_24 else R.drawable.outline_not_started_24),
                    contentDescription = "",
                    modifier = Modifier.size(64.dp)
                )
            }


            if (!imageAnalysisStarted.value) {
                IconButton(
                    onClick = {
                        cameraSelector.value =
                            if (cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                            else CameraSelector.DEFAULT_BACK_CAMERA
                        lifecycleOwner.lifecycleScope.launch {
                            imageAnalysis.value = context.createImageAnalysisUseCase(
                                lifecycleOwner = lifecycleOwner,
                                cameraSelector = cameraSelector.value,
                                previewView = previewView
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_cameraswitch_24),
                        contentDescription = "",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
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

}