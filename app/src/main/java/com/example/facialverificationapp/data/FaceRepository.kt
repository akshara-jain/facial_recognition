package com.example.facialverificationapp.data

import com.example.facialverificationapp.ai.EmbeddingExtractor
import com.example.facialverificationapp.data.local.FaceDao
import com.example.facialverificationapp.data.local.FaceEntity
import com.example.facialverificationapp.security.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VerificationResult(
    val isVerified: Boolean,
    val userId: String? = null,
    val userName: String? = null,
    val confidence: Float = 0.0f
)

class FaceRepository(private val faceDao: FaceDao) {

    // Threshold of Cosine Similarity (value ranges from -1.0 to 1.0)
    // 0.80 - 0.85 is a typical robust threshold for local feature descriptors
    private val similarityThreshold = 0.80f

    suspend fun registerFace(userId: String, name: String, embedding: FloatArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val encryptedBytes = EncryptionUtils.encrypt(embedding)
            val entity = FaceEntity(
                userId = userId,
                name = name,
                encryptedEmbedding = encryptedBytes,
                timestamp = System.currentTimeMillis()
            )
            faceDao.insertFace(entity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun verifyFace(liveEmbedding: FloatArray): VerificationResult = withContext(Dispatchers.IO) {
        try {
            val allFaces = faceDao.getAllFaces()
            var bestMatchEntity: FaceEntity? = null
            var highestSimilarity = -1.0f

            for (faceEntity in allFaces) {
                val registeredEmbedding = EncryptionUtils.decrypt(faceEntity.encryptedEmbedding)
                val similarity = EmbeddingExtractor.calculateCosineSimilarity(liveEmbedding, registeredEmbedding)

                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity
                    bestMatchEntity = faceEntity
                }
            }

            if (highestSimilarity >= similarityThreshold && bestMatchEntity != null) {
                // Convert similarity to simple confidence percentage [0.0 - 1.0] -> [0% - 100%]
                // Scaling 0.80..1.00 to 80%..100%
                val confidence = highestSimilarity.coerceIn(0.0f, 1.0f)
                VerificationResult(
                    isVerified = true,
                    userId = bestMatchEntity.userId,
                    userName = bestMatchEntity.name,
                    confidence = confidence
                )
            } else {
                VerificationResult(
                    isVerified = false,
                    confidence = highestSimilarity.coerceIn(0.0f, 1.0f)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            VerificationResult(isVerified = false)
        }
    }

    suspend fun getAllRegisteredUsers(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        faceDao.getAllFaces().map { it.userId to it.name }
    }

    suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        faceDao.deleteFaceById(userId)
    }
}
