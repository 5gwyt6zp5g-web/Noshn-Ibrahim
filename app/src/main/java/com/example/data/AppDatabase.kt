package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        CourseEntity::class,
        NoteEntity::class,
        QuizEntity::class,
        PurchaseEntity::class,
        ReviewEntity::class,
        AIChatMessageEntity::class,
        CategoryEntity::class,
        EducationalResourceEntity::class,
        WishlistItemEntity::class,
        ResourceProgressEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class EduHubDatabase : RoomDatabase() {
    abstract val dao: EduHubDao

    companion object {
        @Volatile
        private var INSTANCE: EduHubDatabase? = null

        fun getInstance(context: Context): EduHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduHubDatabase::class.java,
                    "eduhub_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
