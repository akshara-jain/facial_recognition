package com.example.facialverificationapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faces")
data class FaceEntity(
    @PrimaryKey
    val userId: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    // The encrypted embedding FloatArray stored as ByteArray (BLOB)
    @ColumnInfo(name = "encrypted_embedding")
    val encryptedEmbedding: ByteArray,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEntity

        if (userId != other.userId) return false
        if (name != other.name) return false
        if (!encryptedEmbedding.contentEquals(other.encryptedEmbedding)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + encryptedEmbedding.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
