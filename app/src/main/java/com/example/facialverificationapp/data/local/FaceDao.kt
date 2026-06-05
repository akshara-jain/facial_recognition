package com.example.facialverificationapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FaceDao {
    @Query("SELECT * FROM faces")
    suspend fun getAllFaces(): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE userId = :userId LIMIT 1")
    suspend fun getFaceById(userId: String): FaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFace(face: FaceEntity): Long

    @Delete
    suspend fun deleteFace(face: FaceEntity): Int

    @Query("DELETE FROM faces WHERE userId = :userId")
    suspend fun deleteFaceById(userId: String): Int

    @Query("DELETE FROM faces")
    suspend fun deleteAllFaces(): Int
}
