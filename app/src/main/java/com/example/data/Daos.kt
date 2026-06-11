package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EduHubDao {
    // --- User Queries ---
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- Category Queries ---
    @Query("SELECT * FROM categories")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // --- Course Queries ---
    @Query("SELECT * FROM courses ORDER BY rating DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE category = :categoryName")
    fun getCoursesByCategory(categoryName: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Query("SELECT * FROM courses WHERE instructorId = :instructorId")
    fun getCoursesByInstructor(instructorId: String): Flow<List<CourseEntity>>

    // --- Notes Queries ---
    @Query("SELECT * FROM notes ORDER BY rating DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE category = :categoryName")
    fun getNotesByCategory(categoryName: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE sellerId = :sellerId")
    fun getNotesBySeller(sellerId: String): Flow<List<NoteEntity>>

    // --- Quiz/Exam Queries ---
    @Query("SELECT * FROM quizzes")
    fun getAllQuizzes(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE isExamBank = :isExamBank")
    fun getQuizzesByType(isExamBank: Boolean): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    suspend fun getQuizById(quizId: String): QuizEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    // --- Purchase Queries ---
    @Query("SELECT * FROM purchases")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE userId = :userId")
    fun getPurchasesForUser(userId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM purchases WHERE userId = :userId AND contentId = :contentId AND contentType = :contentType)")
    fun checkPurchase(userId: String, contentId: String, contentType: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    // --- Review Queries ---
    @Query("SELECT * FROM reviews WHERE contentId = :contentId AND contentType = :contentType ORDER BY timestamp DESC")
    fun getReviews(contentId: String, contentType: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    // --- AI Chat Queries ---
    @Query("SELECT * FROM ai_chats WHERE userId = :userId ORDER BY timestamp ASC")
    fun getChatHistory(userId: String): Flow<List<AIChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: AIChatMessageEntity)

    @Query("DELETE FROM ai_chats WHERE userId = :userId")
    suspend fun clearChatHistory(userId: String)

    // --- Educational Resource Queries ---
    @Query("SELECT * FROM educational_resources ORDER BY timestamp DESC")
    fun getAllEducationalResources(): Flow<List<EducationalResourceEntity>>

    @Query("SELECT * FROM educational_resources WHERE id = :resourceId")
    suspend fun getEducationalResourceById(resourceId: String): EducationalResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducationalResource(resource: EducationalResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducationalResources(resources: List<EducationalResourceEntity>)

    @Query("DELETE FROM educational_resources WHERE id = :resourceId")
    suspend fun deleteEducationalResourceById(resourceId: String)

    // --- Wishlist Queries ---
    @Query("SELECT * FROM wishlist")
    fun getAllWishlistItems(): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWishlistForUser(userId: String): Flow<List<WishlistItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE userId = :userId AND itemId = :itemId)")
    fun checkWishlist(userId: String, itemId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItemEntity)

    @Query("DELETE FROM wishlist WHERE userId = :userId AND itemId = :itemId")
    suspend fun deleteWishlistItem(userId: String, itemId: String)

    // --- Resource Progress Queries ---
    @Query("SELECT * FROM resource_progress WHERE userId = :userId")
    fun getProgressForUser(userId: String): Flow<List<ResourceProgressEntity>>

    @Query("SELECT * FROM resource_progress WHERE userId = :userId AND resourceId = :resourceId LIMIT 1")
    suspend fun getProgressForResource(userId: String, resourceId: String): ResourceProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ResourceProgressEntity)
}
