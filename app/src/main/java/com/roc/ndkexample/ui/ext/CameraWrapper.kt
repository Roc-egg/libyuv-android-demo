package com.roc.ndkexample.ui.ext

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.roc.ndkexample.ui.page.singleThreadPool
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 摄像头扩展类
 * @Author Roc
 * @Date   2024/7/2
 * @Name   CameraWrapper
 */

/**
 * 创建视频捕获用例
 * @receiver Context
 * @param lifecycleOwner LifecycleOwner
 * @param cameraSelector CameraSelector
 * @param previewView PreviewView
 * @return ImageAnalysis
 */
suspend fun Context.createImageAnalysisUseCase(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewView: PreviewView
): ImageAnalysis {
    val preview = Preview.Builder()
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

//    val qualitySelector = QualitySelector.from(
//        Quality.FHD,
//        FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
//    )
//    val recorder = Recorder.Builder()
//        .setExecutor(mainExecutor)
//        .setQualitySelector(qualitySelector)
//        .build()
//    val videoCapture = VideoCapture.withOutput(recorder)

    val mImageAnalysis =
        ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)//设置输出YUV_420_888格式
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        Size(720, 1080),// 图片的建议尺寸
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                ).build()
            )
            .setOutputImageRotationEnabled(true) // 是否旋转分析器中得到的图片
            .setTargetRotation(Surface.ROTATION_0) // 允许旋转后 得到图片的旋转设置
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

    val cameraProvider = getCameraProvider()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        mImageAnalysis
    )

    return mImageAnalysis
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            { continuation.resume(future.get()) },
            singleThreadPool
        )
    }
}

@SuppressLint("MissingPermission")
fun startRecordingVideo(
    context: Context,
    filenameFormat: String,
    videoCapture: VideoCapture<Recorder>,
    outputDirectory: File,
    executor: Executor,
    audioEnabled: Boolean,
    consumer: Consumer<VideoRecordEvent>
): Recording {
    val videoFile = File(
        outputDirectory,
        SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".mp4"
    )

    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .apply { if (audioEnabled) withAudioEnabled() }
        .start(executor, consumer)
}