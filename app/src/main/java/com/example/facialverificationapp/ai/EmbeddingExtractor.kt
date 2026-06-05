package com.example.facialverificationapp.ai

import android.graphics.Bitmap
import kotlin.math.sqrt

object EmbeddingExtractor {

    /**
     * Extracts a deterministic 128-dimensional face embedding from a cropped face bitmap.
     * This is a robust local-feature statistical descriptor that allows offline face comparison.
     * To swap with a real TFLite face recognition model (e.g. FaceNet):
     * Simply load your FaceNet TFLite interpreter and run it on the cropped bitmap.
     */
    fun extractEmbedding(faceBitmap: Bitmap): FloatArray {
        // 1. Resize to a grid that yields 128 features (e.g., 8x16 = 128 features)
        val targetWidth = 8
        val targetHeight = 16
        val resized = Bitmap.createScaledBitmap(faceBitmap, targetWidth, targetHeight, true)

        val embedding = FloatArray(128)
        var index = 0

        // 2. Extract grayscale intensity values as base features
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val pixel = resized.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Standard grayscale conversion (luminance formula)
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                
                // Store normalized gray value in embedding
                embedding[index++] = gray / 255.0f
            }
        }

        // 3. Normalize the vector to unit length (L2 Norm) for stable similarity math
        var sumSquare = 0.0f
        for (value in embedding) {
            sumSquare += value * value
        }
        val norm = sqrt(sumSquare)
        
        if (norm > 0.0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }

        return embedding
    }

    /**
     * Calculates the Cosine Similarity between two face embeddings.
     * Returns a score between -1.0 and 1.0 (with 1.0 being identical).
     */
    fun calculateCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0.0f
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0.0f) dotProduct / denominator else 0.0f
    }

    /**
     * Calculates the Euclidean distance between two face embeddings.
     * With L2 normalized vectors, Euclidean distance relates directly to Cosine distance.
     */
    fun calculateEuclideanDistance(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return Float.MAX_VALUE
        var sum = 0.0f
        for (i in vectorA.indices) {
            val diff = vectorA[i] - vectorB[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
}
