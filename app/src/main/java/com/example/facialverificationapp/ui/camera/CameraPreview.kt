package com.example.facialverificationapp.ui.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.facialverificationapp.ai.FaceDetection
import com.example.facialverificationapp.ai.LivenessDetector
import com.example.facialverificationapp.ai.YoloDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    yoloDetector: YoloDetector,
    livenessDetector: LivenessDetector,
    onFaceDetections: (List<FaceDetection>) -> Unit,
    onFaceCropReady: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Setup camera preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Setup image analysis (YOLO and MediaPipe stream)
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    // Convert ImageProxy to Bitmap using CameraX native extension
                    val bitmap = imageProxy.toBitmap()

                    // Rotate the bitmap based on camera hardware orientation
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val rotatedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else {
                        bitmap
                    }

                    // 1. Run YOLO Face Detection
                    val detections = yoloDetector.detect(rotatedBitmap)
                    onFaceDetections(detections)

                    // 2. Extract cropped face if a face is detected
                    if (detections.isNotEmpty()) {
                        val primaryDetection = detections.first()
                        val box = primaryDetection.boundingBox
                        
                        // Map normalized bounding box coordinates to rotated bitmap pixel dimensions
                        val width = rotatedBitmap.width
                        val height = rotatedBitmap.height
                        
                        val left = (box.left * width).toInt().coerceIn(0, width - 1)
                        val top = (box.top * height).toInt().coerceIn(0, height - 1)
                        val right = (box.right * width).toInt().coerceIn(0, width)
                        val bottom = (box.bottom * height).toInt().coerceIn(0, height)
                        
                        val cropW = right - left
                        val cropH = bottom - top

                        if (cropW > 10 && cropH > 10) {
                            val cropBitmap = Bitmap.createBitmap(rotatedBitmap, left, top, cropW, cropH)
                            onFaceCropReady(cropBitmap)
                        }
                    }

                    // 3. Feed frame into MediaPipe Liveness Detector
                    livenessDetector.detect(rotatedBitmap)

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}
