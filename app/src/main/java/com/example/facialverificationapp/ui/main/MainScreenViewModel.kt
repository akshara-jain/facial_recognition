package com.example.facialverificationapp.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facialverificationapp.ai.EmbeddingExtractor
import com.example.facialverificationapp.ai.FaceDetection
import com.example.facialverificationapp.ai.LivenessChallenge
import com.example.facialverificationapp.data.FaceRepository
import com.example.facialverificationapp.data.VerificationResult
import com.example.facialverificationapp.security.SecurityCheckUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SecurityState {
    data object Checking : SecurityState
    data object Passed : SecurityState
    data class Failed(val isRooted: Boolean, val isEmulator: Boolean) : SecurityState
}

class MainScreenViewModel(private val repository: FaceRepository) : ViewModel() {

    private val _securityState = MutableStateFlow<SecurityState>(SecurityState.Checking)
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

    private val _registeredUsers = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val registeredUsers: StateFlow<List<Pair<String, String>>> = _registeredUsers.asStateFlow()

    // Face detection overlay state
    private val _activeDetections = MutableStateFlow<List<FaceDetection>>(emptyList())
    val activeDetections: StateFlow<List<FaceDetection>> = _activeDetections.asStateFlow()

    // Liveness Challenge Flow
    private val _currentChallenge = MutableStateFlow(LivenessChallenge.NONE)
    val currentChallenge: StateFlow<LivenessChallenge> = _currentChallenge.asStateFlow()

    private val _livenessPassed = MutableStateFlow(false)
    val livenessPassed: StateFlow<Boolean> = _livenessPassed.asStateFlow()

    // Verification Flow
    private val _verificationResult = MutableStateFlow<VerificationResult?>(null)
    val verificationResult: StateFlow<VerificationResult?> = _verificationResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Active face crop bitmap for registration/verification
    private var lastFaceCrop: Bitmap? = null

    init {
        performSecurityChecks()
        loadRegisteredUsers()
    }

    private fun performSecurityChecks() {
        viewModelScope.launch {
            val rooted = SecurityCheckUtils.isDeviceRooted()
            val emulator = SecurityCheckUtils.isEmulator()
            if (rooted || emulator) {
                _securityState.value = SecurityState.Failed(isRooted = rooted, isEmulator = emulator)
            } else {
                _securityState.value = SecurityState.Passed
            }
        }
    }

    fun loadRegisteredUsers() {
        viewModelScope.launch {
            _registeredUsers.value = repository.getAllRegisteredUsers()
        }
    }

    fun updateDetections(detections: List<FaceDetection>) {
        _activeDetections.value = detections
    }

    fun updateFaceCrop(bitmap: Bitmap) {
        lastFaceCrop = bitmap
    }

    fun updateLivenessChallenge(challenge: LivenessChallenge) {
        _currentChallenge.value = challenge
    }

    fun onLivenessPassed() {
        _livenessPassed.value = true
        // Automatically trigger verification on the last captured face crop once liveness passes
        val crop = lastFaceCrop
        if (crop != null) {
            verifyFaceCrop(crop)
        } else {
            _verificationResult.value = VerificationResult(
                isVerified = false,
                confidence = 0.0f
            )
        }
    }

    fun verifyFaceCrop(bitmap: Bitmap) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        viewModelScope.launch {
            val embedding = EmbeddingExtractor.extractEmbedding(bitmap)
            val result = repository.verifyFace(embedding)
            _verificationResult.value = result
            _isProcessing.value = false
        }
    }

    fun registerUser(
        userId: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val crop = lastFaceCrop
        if (crop == null) {
            onError("No face detected. Please look at the camera.")
            return
        }

        if (userId.isBlank() || name.isBlank()) {
            onError("User ID and Name cannot be empty.")
            return
        }

        _isProcessing.value = true
        viewModelScope.launch {
            val embedding = EmbeddingExtractor.extractEmbedding(crop)
            val success = repository.registerFace(userId, name, embedding)
            _isProcessing.value = false
            if (success) {
                loadRegisteredUsers()
                onSuccess()
            } else {
                onError("Failed to save face registration to database.")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            loadRegisteredUsers()
        }
    }

    fun resetLivenessAndVerification() {
        _currentChallenge.value = LivenessChallenge.NONE
        _livenessPassed.value = false
        _verificationResult.value = null
        _activeDetections.value = emptyList()
        lastFaceCrop = null
    }
}
