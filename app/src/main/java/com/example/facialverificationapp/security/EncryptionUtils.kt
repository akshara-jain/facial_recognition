package com.example.facialverificationapp.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionUtils {
    private const val KEY_ALIAS = "FacialVerificationKey"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // Standard IV size for GCM is 12 bytes
    private const val TAG_SIZE = 128 // GCM Tag size in bits

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(data: FloatArray): ByteArray {
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv

        // Convert FloatArray to ByteArray
        val byteBuffer = ByteBuffer.allocate(data.size * 4)
        byteBuffer.asFloatBuffer().put(data)
        val plainBytes = byteBuffer.array()

        val cipherBytes = cipher.doFinal(plainBytes)

        // Combine IV and Ciphertext: [IV (12 bytes) | Ciphertext]
        val combinedBytes = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combinedBytes, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combinedBytes, iv.size, cipherBytes.size)
        return combinedBytes
    }

    fun decrypt(combinedBytes: ByteArray): FloatArray {
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val iv = ByteArray(IV_SIZE)
        System.arraycopy(combinedBytes, 0, iv, 0, IV_SIZE)

        val cipherBytes = ByteArray(combinedBytes.size - IV_SIZE)
        System.arraycopy(combinedBytes, IV_SIZE, cipherBytes, 0, cipherBytes.size)

        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(cipherBytes)

        // Convert ByteArray back to FloatArray
        val floatBuffer = FloatBuffer.allocate(decryptedBytes.size / 4)
        val byteBuffer = ByteBuffer.wrap(decryptedBytes)
        floatBuffer.put(byteBuffer.asFloatBuffer())
        return floatBuffer.array()
    }
}
