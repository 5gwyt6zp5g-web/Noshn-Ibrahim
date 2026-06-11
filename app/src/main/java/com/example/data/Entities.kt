package com.example.data

import androidx.room.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String, // "STUDENT", "INSTRUCTOR", "ADMIN"
    val earnings: Double = 0.0,
    val premiumUser: Boolean = false
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val instructorId: String,
    val instructorName: String,
    val price: Double,
    val category: String,
    val lessonsJson: String, // JSON array of lessons: [{"title":"Introduction","duration":"5:20","videoUrl":"video1"},{"title":"Core Concepts","duration":"12:45","videoUrl":"video2"}]
    val rating: Float = 4.5f,
    val reviewsCount: Int = 0
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val previewText: String, // First page/abstract of the summary
    val downloadUrl: String, // Secure reference
    val sellerId: String,
    val sellerName: String,
    val rating: Float = 4.8f,
    val reviewsCount: Int = 0
)

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val title: String,
    val questionsJson: String, // JSON array: [{"id":"q1","question":"Which algorithm is O(n log n)?","options":["Bubble Sort","Quick Sort","Selection Sort","Insertion Sort"],"correctIndex":1,"explanation":"Quick Sort has an average time complexity of O(n log n)."}]
    val isExamBank: Boolean = false // false: self-practice quiz, true: university past exam question bank
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val contentId: String,
    val contentType: String, // "COURSE", "NOTE"
    val pricePaid: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val contentType: String, // "COURSE", "NOTE"
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_chats")
data class AIChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String // Label mapping to Material Icon symbols
)

@Entity(tableName = "educational_resources")
data class EducationalResourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val authorId: String,
    val authorName: String = "Author",
    val timestamp: Long = System.currentTimeMillis(),
    val averageRating: Double = 0.0,
    val reviewsCount: Int = 0,
    val isReported: Boolean = false,
    val reportReason: String = "",
    val tags: String = ""
)

@Entity(tableName = "wishlist", primaryKeys = ["userId", "itemId"])
data class WishlistItemEntity(
    val userId: String,
    val itemId: String,
    val itemType: String, // "COURSE" or "NOTE"
    val timestamp: Long = System.currentTimeMillis()
)

data class ResourceComment(
    val id: String = "",
    val resourceId: String = "",
    val userId: String = "",
    val userName: String = "",
    val commentText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "resource_progress", primaryKeys = ["userId", "resourceId"])
data class ResourceProgressEntity(
    val userId: String,
    val resourceId: String,
    val completedPagesCsv: String = "", // comma separated list of completed indices/sections: e.g., "0,1,3"
    val lastUpdated: Long = System.currentTimeMillis()
)

