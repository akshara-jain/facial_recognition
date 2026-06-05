package com.example.facialverificationapp.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.facialverificationapp.ai.FaceDetection
import java.util.Locale

@Composable
fun BoundingBoxOverlay(
    detections: List<FaceDetection>,
    isFrontCamera: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (detection in detections) {
            val box = detection.boundingBox
            
            // Map normalized coordinates [0, 1] to screen dimensions.
            // If using the front camera, the preview is mirrored, so we mirror the X coordinates.
            val left = if (isFrontCamera) (1.0f - box.right) * width else box.left * width
            val right = if (isFrontCamera) (1.0f - box.left) * width else box.right * width
            val top = box.top * height
            val bottom = box.bottom * height

            val rectWidth = right - left
            val rectHeight = bottom - top

            // Draw bounding box
            drawRect(
                color = Color(0xFF00E676), // Vibrant Neon Green
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
