package com.example.facialverificationapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.facialverificationapp.ai.LivenessDetector
import com.example.facialverificationapp.ai.YoloDetector
import com.example.facialverificationapp.ui.camera.BoundingBoxOverlay
import com.example.facialverificationapp.ui.camera.CameraPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
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

    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    val isProcessing by viewModel.isProcessing.collectAsState()
    val activeDetections by viewModel.activeDetections.collectAsState()

    // Initialize detectors locally for camera binding
    val yoloDetector = remember { YoloDetector(context) }
    val mockLivenessDetector = remember {
        LivenessDetector(context, object : com.example.facialverificationapp.ai.LivenessListener {
            override fun onChallengeChanged(newChallenge: com.example.facialverificationapp.ai.LivenessChallenge) {}
            override fun onLivenessPassed() {}
            override fun onLandmarksDetected(landmarks: List<com.google.mediapipe.tasks.vision.facelandmarker.NormalizedLandmark>) {}
            override fun onError(error: String) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enroll Face", fontWeight = FontWeight.Bold, color = Color.White) },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Live camera preview box with glowing neon borders
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2979FF), Color(0xFF00E676))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(Color.Black)
                ) {
                    CameraPreview(
                        yoloDetector = yoloDetector,
                        livenessDetector = mockLivenessDetector,
                        onFaceDetections = { viewModel.updateDetections(it) },
                        onFaceCropReady = { viewModel.updateFaceCrop(it) }
                    )

                    BoundingBoxOverlay(
                        detections = activeDetections,
                        isFrontCamera = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Registration details Form
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "User Registration Details",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = userId,
                            onValueChange = { userId = it },
                            label = { Text("User ID") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color(0xFF00E676),
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color(0xFF00E676),
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isProcessing) {
                            CircularProgressIndicator(color = Color(0xFF00E676), modifier = Modifier.size(36.dp))
                        } else {
                            Button(
                                onClick = {
                                    viewModel.registerUser(
                                        userId = userId,
                                        name = userName,
                                        onSuccess = {
                                            Toast.makeText(context, "Registration Succeeded!", Toast.LENGTH_SHORT).show()
                                            onBackClick()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E676),
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Enroll Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required to enroll user faces.", color = Color.White)
            }
        }
    }
}
