package com.example.facialverificationapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.facialverificationapp.ai.FaceDetection
import com.example.facialverificationapp.ai.LivenessChallenge
import com.example.facialverificationapp.ai.LivenessDetector
import com.example.facialverificationapp.ai.LivenessListener
import com.example.facialverificationapp.ai.YoloDetector
import com.example.facialverificationapp.ui.camera.BoundingBoxOverlay
import com.example.facialverificationapp.ui.camera.CameraPreview
import com.google.mediapipe.tasks.vision.facelandmarker.NormalizedLandmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    viewModel: MainScreenViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val activeDetections by viewModel.activeDetections.collectAsState()
    val challenge by viewModel.currentChallenge.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val verificationResult by viewModel.verificationResult.collectAsState()
    val livenessPassed by viewModel.livenessPassed.collectAsState()

    // Initialize detectors and coordinate states
    val yoloDetector = remember { YoloDetector(context) }
    val livenessDetector = remember {
        LivenessDetector(context, object : LivenessListener {
            override fun onChallengeChanged(newChallenge: LivenessChallenge) {
                viewModel.updateLivenessChallenge(newChallenge)
            }

            override fun onLivenessPassed() {
                viewModel.onLivenessPassed()
            }

            override fun onLandmarksDetected(landmarks: List<NormalizedLandmark>) {}

            override fun onError(error: String) {}
        })
    }

    // Launch the challenge sequence automatically when screen is loaded
    LaunchedEffect(key1 = hasCameraPermission) {
        if (hasCameraPermission) {
            viewModel.resetLivenessAndVerification()
            livenessDetector.startChallenge()
        }
    }

    DisposableEffect(key1 = true) {
        onDispose {
            livenessDetector.close()
            yoloDetector.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity Verification", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Instruction Banner
                    ChallengeCard(challenge = challenge, isProcessing = isProcessing, livenessPassed = livenessPassed)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Camera view container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = 2.dp,
                                brush = Brush.horizontalGradient(
                                    colors = getGlowColors(challenge, verificationResult != null)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .background(Color.Black)
                    ) {
                        CameraPreview(
                            yoloDetector = yoloDetector,
                            livenessDetector = livenessDetector,
                            onFaceDetections = { viewModel.updateDetections(it) },
                            onFaceCropReady = { viewModel.updateFaceCrop(it) }
                        )

                        BoundingBoxOverlay(
                            detections = activeDetections,
                            isFrontCamera = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Restart / Action triggers
                    Button(
                        onClick = {
                            viewModel.resetLivenessAndVerification()
                            livenessDetector.startChallenge()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Reset Challenges", fontWeight = FontWeight.Bold)
                    }
                }

                // Premium overlay showing verification results
                AnimatedVisibility(
                    visible = verificationResult != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    verificationResult?.let { result ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .border(
                                        width = 2.dp,
                                        color = if (result.isVerified) Color(0xFF00E676) else Color(0xFFFF1744),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (result.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = "Status",
                                        tint = if (result.isVerified) Color(0xFF00E676) else Color(0xFFFF1744),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (result.isVerified) "VERIFIED" else "NOT VERIFIED",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.isVerified) Color(0xFF00E676) else Color(0xFFFF1744)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (result.isVerified) {
                                        Text(
                                            text = "Welcome, ${result.userName}",
                                            fontSize = 18.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ID: ${result.userId}",
                                            fontSize = 14.sp,
                                            color = Color.LightGray
                                        )
                                    } else {
                                        Text(
                                            text = "No matching profile found offline.",
                                            fontSize = 14.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Similarity Confidence: ${String.format(java.util.Locale.US, "%.1f", result.confidence * 100)}%",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.resetLivenessAndVerification()
                                            livenessDetector.startChallenge()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (result.isVerified) Color(0xFF00E676) else Color(0xFFFF1744),
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Acknowledge", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required for face verification.", color = Color.White)
            }
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: LivenessChallenge,
    isProcessing: Boolean,
    livenessPassed: Boolean
) {
    val containerColor = when (challenge) {
        LivenessChallenge.BLINK_ONCE -> Color(0xFF3E2723)
        LivenessChallenge.TURN_HEAD_LEFT -> Color(0xFF0D47A1)
        LivenessChallenge.TURN_HEAD_RIGHT -> Color(0xFF311B92)
        LivenessChallenge.PASSED -> Color(0xFF1B5E20)
        else -> Color(0xFF1E1E1E)
    }

    val glowColor = when (challenge) {
        LivenessChallenge.BLINK_ONCE -> Color(0xFFFFD54F)
        LivenessChallenge.TURN_HEAD_LEFT -> Color(0xFF64B5F6)
        LivenessChallenge.TURN_HEAD_RIGHT -> Color(0xFFB39DDB)
        LivenessChallenge.PASSED -> Color(0xFF81C784)
        else -> Color.DarkGray
    }

    val instruction = when (challenge) {
        LivenessChallenge.BLINK_ONCE -> "CHALLENGE 1: Blink once"
        LivenessChallenge.TURN_HEAD_LEFT -> "CHALLENGE 2: Turn your head left"
        LivenessChallenge.TURN_HEAD_RIGHT -> "CHALLENGE 3: Turn your head right"
        LivenessChallenge.PASSED -> "Liveness checks completed!"
        else -> "Ready, positioning camera..."
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, glowColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Running YOLO model comparison...", color = Color.White, fontWeight = FontWeight.SemiBold)
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(glowColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = instruction,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

fun getGlowColors(challenge: LivenessChallenge, verified: Boolean): List<Color> {
    if (verified) return listOf(Color(0xFF00E676), Color(0xFF00E676))
    return when (challenge) {
        LivenessChallenge.BLINK_ONCE -> listOf(Color(0xFFFFD54F), Color(0xFFFFA000))
        LivenessChallenge.TURN_HEAD_LEFT -> listOf(Color(0xFF2979FF), Color(0xFF00D5FF))
        LivenessChallenge.TURN_HEAD_RIGHT -> listOf(Color(0xFF7C4DFF), Color(0xFFE040FB))
        LivenessChallenge.PASSED -> listOf(Color(0xFF00E676), Color(0xFF00C853))
        else -> listOf(Color(0xFF2196F3), Color(0xFF00E676))
    }
}
