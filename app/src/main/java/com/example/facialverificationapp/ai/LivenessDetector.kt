package com.example.facialverificationapp.ai

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

enum class LivenessChallenge {
    NONE,
    BLINK_ONCE,
    TURN_HEAD_LEFT,
    TURN_HEAD_RIGHT,
    PASSED
}

interface LivenessListener {
    fun onChallengeChanged(newChallenge: LivenessChallenge)
    fun onLivenessPassed()
    fun onLandmarksDetected(landmarks: List<NormalizedLandmark>)
    fun onError(error: String)
}

class LivenessDetector(
    private val context: Context,
    private val listener: LivenessListener
) {
    private var faceLandmarker: FaceLandmarker? = null
    
    // Liveness state machine
    var currentChallenge = LivenessChallenge.NONE
        private set
    
    private val challengeSequence = listOf(
        LivenessChallenge.BLINK_ONCE,
        LivenessChallenge.TURN_HEAD_LEFT,
        LivenessChallenge.TURN_HEAD_RIGHT
    )
    private var sequenceIndex = 0

    // Blink detection helper states
    private var isEyeClosed = false
    private var eyeClosedTimestamp = 0L

    init {
        setupFaceLandmarker()
    }

    private fun setupFaceLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, inputImage ->
                    processFaceLandmarks(result)
                }
                .setErrorListener { error ->
                    listener.onError(error.message ?: "Unknown MediaPipe error")
                }
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            listener.onError("Failed to initialize FaceLandmarker: ${e.message}")
        }
    }

    fun startChallenge() {
        sequenceIndex = 0
        currentChallenge = challengeSequence[sequenceIndex]
        listener.onChallengeChanged(currentChallenge)
        isEyeClosed = false
    }

    fun reset() {
        sequenceIndex = 0
        currentChallenge = LivenessChallenge.NONE
        isEyeClosed = false
    }

    fun detect(bitmap: Bitmap) {
        val landmarker = faceLandmarker ?: return
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val timestamp = SystemClock.uptimeMillis()
            landmarker.detectAsync(mpImage, timestamp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processFaceLandmarks(result: FaceLandmarkerResult) {
        val landmarks = result.faceLandmarks().firstOrNull()
        if (landmarks == null) {
            // No face detected for liveness
            return
        }

        listener.onLandmarksDetected(landmarks)

        if (currentChallenge == LivenessChallenge.PASSED || currentChallenge == LivenessChallenge.NONE) {
            return
        }

        // --- Challenge: Blink Detection ---
        if (currentChallenge == LivenessChallenge.BLINK_ONCE) {
            // Right Eye points: 159 (top), 145 (bottom), 33 (outer), 133 (inner)
            val rightEar = calculateEAR(landmarks[159], landmarks[145], landmarks[33], landmarks[133])
            
            // Left Eye points: 386 (top), 374 (bottom), 263 (outer), 362 (inner)
            val leftEar = calculateEAR(landmarks[386], landmarks[374], landmarks[263], landmarks[362])
            
            val avgEar = (rightEar + leftEar) / 2.0f

            if (!isEyeClosed && avgEar < 0.20f) {
                isEyeClosed = true
                eyeClosedTimestamp = SystemClock.elapsedRealtime()
            } else if (isEyeClosed && avgEar > 0.26f) {
                val duration = SystemClock.elapsedRealtime() - eyeClosedTimestamp
                // Valid human blink usually takes between 100ms and 500ms
                if (duration in 80..600) {
                    advanceChallenge()
                } else {
                    isEyeClosed = false // Reset if too slow/fast (possible spoofing)
                }
            }
        }

        // --- Challenge: Head Turn Detection ---
        if (currentChallenge == LivenessChallenge.TURN_HEAD_LEFT || currentChallenge == LivenessChallenge.TURN_HEAD_RIGHT) {
            // Nose tip: 4
            // Left Cheek contour: 234
            // Right Cheek contour: 454
            val nose = landmarks[4]
            val leftCheek = landmarks[234]
            val rightCheek = landmarks[454]

            val distToLeft = calculateDistance3D(nose, leftCheek)
            val distToRight = calculateDistance3D(nose, rightCheek)

            if (distToRight > 0f) {
                val ratio = distToLeft / distToRight
                if (currentChallenge == LivenessChallenge.TURN_HEAD_LEFT && ratio < 0.45f) {
                    advanceChallenge()
                } else if (currentChallenge == LivenessChallenge.TURN_HEAD_RIGHT && ratio > 2.2f) {
                    advanceChallenge()
                }
            }
        }
    }

    private fun advanceChallenge() {
        sequenceIndex++
        isEyeClosed = false
        if (sequenceIndex < challengeSequence.size) {
            currentChallenge = challengeSequence[sequenceIndex]
            listener.onChallengeChanged(currentChallenge)
        } else {
            currentChallenge = LivenessChallenge.PASSED
            listener.onChallengeChanged(currentChallenge)
            listener.onLivenessPassed()
        }
    }

    private fun calculateEAR(top: NormalizedLandmark, bottom: NormalizedLandmark, outer: NormalizedLandmark, inner: NormalizedLandmark): Float {
        val vertDist = calculateDistance2D(top, bottom)
        val horizDist = calculateDistance2D(outer, inner)
        return if (horizDist > 0f) vertDist / horizDist else 0f
    }

    private fun calculateDistance2D(
        p1: NormalizedLandmark,
        p2: NormalizedLandmark
    ): Float {

        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()

        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateDistance3D(
        p1: NormalizedLandmark,
        p2: NormalizedLandmark
    ): Float {

        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        val dz = p1.z() - p2.z()

        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
    }
}
