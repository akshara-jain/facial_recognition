package com.example.facialverificationapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FaceEntity::class], version = 1, exportSchema = false)
abstract class FaceDatabase : RoomDatabase() {
    abstract fun faceDao(): FaceDao

    companion object {
        @Volatile
        private var INSTANCE: FaceDatabase? = null

        fun getDatabase(context: Context): FaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDatabase::class.java,
                    "face_verification_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
