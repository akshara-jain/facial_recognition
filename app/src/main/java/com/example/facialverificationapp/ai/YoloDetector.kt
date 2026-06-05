package com.example.facialverificationapp.ai

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

data class FaceDetection(
    val boundingBox: RectF,
    val confidence: Float,
    val label: String
)

class YoloDetector(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()

    private var inputWidth = 320 // Fallback default, will be checked dynamically
    private var inputHeight = 320 // Fallback default
    private var inputChannels = 3

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        try {
            val assetManager = context.assets
            val fileDescriptor: AssetFileDescriptor = assetManager.openFd("model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            val options = Interpreter.Options()
            options.setNumThreads(4)
            val interp = Interpreter(modelBuffer, options)
            
            // Inspect input shape dynamically
            val inputTensor = interp.getInputTensor(0)
            val shape = inputTensor.shape() // E.g., [1, 320, 320, 3] or [1, 3, 320, 320]
            if (shape.size == 4) {
                // Determine whether NCHW or NHWC format
                if (shape[1] == 1 || shape[1] == 3) {
                    // NCHW
                    inputChannels = shape[1]
                    inputHeight = shape[2]
                    inputWidth = shape[3]
                } else {
                    // NHWC
                    inputHeight = shape[1]
                    inputWidth = shape[2]
                    inputChannels = shape[3]
                }
            }
            interpreter = interp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("labels.txt")))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { labels.add(it) }
            }
            reader.close()
        } catch (e: Exception) {
            // Default to "face" if labels fail to load
            labels.add("face")
        }
    }

    fun detect(bitmap: Bitmap): List<FaceDetection> {
        val interp = interpreter ?: return emptyList()

        // 1. Preprocess the bitmap to ByteBuffer
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputHeight * inputWidth * inputChannels * 4) // 4 bytes for float32
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()

        val intValues = IntArray(inputWidth * inputHeight)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        for (pixel in intValues) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        // 2. Setup outputs
        // Dynamic output inspection
        val outputTensor = interp.getOutputTensor(0)
        val outputShape = outputTensor.shape() // E.g., [1, 5, 8400] or [1, 8400, 5]
        
        val outputMap = HashMap<Int, Any>()
        
        // Setup raw buffer according to dimensions
        val rows: Int
        val cols: Int
        val transpose: Boolean
        
        if (outputShape[1] < outputShape[2]) {
            rows = outputShape[1] // E.g. 5 (x, y, w, h, score)
            cols = outputShape[2] // E.g. 8400 anchors
            transpose = true
        } else {
            rows = outputShape[1] // E.g. 8400 anchors
            cols = outputShape[2] // E.g. 5
            transpose = false
        }
        
        val outputBuffer = Array(1) { Array(rows) { FloatArray(cols) } }
        outputMap[0] = outputBuffer

        // 3. Run Inference
        try {
            interp.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        // 4. Parse Outputs
        val detections = mutableListOf<FaceDetection>()
        val confidenceThreshold = 0.5f

        val rawOutput = outputBuffer[0]
        
        if (transpose) {
            // Raw output format: [5][8400] -> [x, y, w, h, confidence][anchor]
            for (c in 0 until cols) {
                val score = rawOutput[4][c]
                if (score > confidenceThreshold) {
                    val xCenter = rawOutput[0][c]
                    val yCenter = rawOutput[1][c]
                    val w = rawOutput[2][c]
                    val h = rawOutput[3][c]

                    val left = max(0.0f, xCenter - w / 2f)
                    val top = max(0.0f, yCenter - h / 2f)
                    val right = min(inputWidth.toFloat(), xCenter + w / 2f)
                    val bottom = min(inputHeight.toFloat(), yCenter + h / 2f)

                    val normalizedRect = RectF(
                        left / inputWidth,
                        top / inputHeight,
                        right / inputWidth,
                        bottom / inputHeight
                    )

                    val label = if (labels.isNotEmpty()) labels[0] else "face"
                    detections.add(FaceDetection(normalizedRect, score, label))
                }
            }
        } else {
            // Raw output format: [8400][5] -> [anchor][x, y, w, h, confidence]
            for (r in 0 until rows) {
                val score = rawOutput[r][4]
                if (score > confidenceThreshold) {
                    val xCenter = rawOutput[r][0]
                    val yCenter = rawOutput[r][1]
                    val w = rawOutput[r][2]
                    val h = rawOutput[r][3]

                    val left = max(0.0f, xCenter - w / 2f)
                    val top = max(0.0f, yCenter - h / 2f)
                    val right = min(inputWidth.toFloat(), xCenter + w / 2f)
                    val bottom = min(inputHeight.toFloat(), yCenter + h / 2f)

                    val normalizedRect = RectF(
                        left / inputWidth,
                        top / inputHeight,
                        right / inputWidth,
                        bottom / inputHeight
                    )

                    val label = if (labels.isNotEmpty()) labels[0] else "face"
                    detections.add(FaceDetection(normalizedRect, score, label))
                }
            }
        }

        // Apply Non-Maximum Suppression
        return applyNMS(detections)
    }

    private fun applyNMS(detections: List<FaceDetection>): List<FaceDetection> {
        val sortedDetections = detections.sortedByDescending { it.confidence }.toMutableList()
        val selectedDetections = mutableListOf<FaceDetection>()
        val iouThreshold = 0.45f

        while (sortedDetections.isNotEmpty()) {
            val first = sortedDetections.removeAt(0)
            selectedDetections.add(first)
            val iterator = sortedDetections.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (calculateIoU(first.boundingBox, next.boundingBox) > iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selectedDetections
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)

        val intersectionArea = max(0f, intersectionRight - intersectionLeft) * max(0f, intersectionBottom - intersectionTop)
        val area1 = (box1.right - box1.left) * (box1.bottom - box1.top)
        val area2 = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = area1 + area2 - intersectionArea

        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
