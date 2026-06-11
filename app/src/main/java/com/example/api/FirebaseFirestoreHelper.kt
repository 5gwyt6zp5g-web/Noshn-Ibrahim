package com.example.api

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import com.example.data.CourseEntity
import com.example.data.NoteEntity
import com.example.data.EducationalResourceEntity
import com.example.data.ResourceComment

object FirebaseFirestoreHelper {
    private const val TAG = "FirebaseFirestoreHelper"

    /**
     * Safely retrieves the FirebaseFirestore instance, ensuring Firebase is initialized first.
     */
    fun getFirestore(context: Context): FirebaseFirestore? {
        FirebaseAuthHelper.initializeFirebase(context.applicationContext)
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore.getInstance() failed: ${e.message}")
            null
        }
    }

    /**
     * Seeds courses and notes up to Firestore if a connection is successfully established.
     * This ensures real Firestore collections contain the same seed data.
     */
    suspend fun seedFirestoreIfNeeded(context: Context, localCourses: List<CourseEntity>, localNotes: List<NoteEntity>) {
        val db = getFirestore(context) ?: return
        try {
            // Check if already seeded in Firestore
            val courseSnap = db.collection("courses").limit(1).get().await()
            if (courseSnap.isEmpty) {
                Log.d(TAG, "Seeding courses into Firestore...")
                for (course in localCourses) {
                    val map = hashMapOf(
                        "id" to course.id,
                        "title" to course.title,
                        "description" to course.description,
                        "instructorId" to course.instructorId,
                        "instructorName" to course.instructorName,
                        "price" to course.price,
                        "category" to course.category,
                        "lessonsJson" to course.lessonsJson,
                        "rating" to course.rating.toDouble(),
                        "reviewsCount" to course.reviewsCount
                    )
                    db.collection("courses").document(course.id).set(map).await()
                }
            }

            val notesSnap = db.collection("notes").limit(1).get().await()
            if (notesSnap.isEmpty) {
                Log.d(TAG, "Seeding notes into Firestore...")
                for (note in localNotes) {
                    val map = hashMapOf(
                        "id" to note.id,
                        "title" to note.title,
                        "description" to note.description,
                        "price" to note.price,
                        "category" to note.category,
                        "previewText" to note.previewText,
                        "downloadUrl" to note.downloadUrl,
                        "sellerId" to note.sellerId,
                        "sellerName" to note.sellerName,
                        "rating" to note.rating.toDouble(),
                        "reviewsCount" to note.reviewsCount
                    )
                    db.collection("notes").document(note.id).set(map).await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding Firestore: ${e.message}")
        }
    }

    /**
     * Fetch courses directly from Firestore collection, optionally filtered by category.
     */
    suspend fun fetchCoursesFromFirestore(context: Context, categoryName: String? = null): List<CourseEntity> {
        val db = getFirestore(context) ?: return emptyList()
        return try {
            var query: Query = db.collection("courses")
            if (!categoryName.isNullOrBlank()) {
                query = query.whereEqualTo("category", categoryName)
            }
            val snap = query.get().await()
            snap.documents.mapNotNull { doc ->
                mapDocToCourse(doc)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching courses from Firestore: ${e.message}")
            throw e // propagate to allow local fallback
        }
    }

    /**
     * Fetch notes directly from Firestore collection, optionally filtered by category.
     */
    suspend fun fetchNotesFromFirestore(context: Context, categoryName: String? = null): List<NoteEntity> {
        val db = getFirestore(context) ?: return emptyList()
        return try {
            var query: Query = db.collection("notes")
            if (!categoryName.isNullOrBlank()) {
                query = query.whereEqualTo("category", categoryName)
            }
            val snap = query.get().await()
            snap.documents.mapNotNull { doc ->
                mapDocToNote(doc)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notes from Firestore: ${e.message}")
            throw e // propagate to allow local fallback
        }
    }

    private fun mapDocToCourse(doc: DocumentSnapshot): CourseEntity? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: ""
            val description = doc.getString("description") ?: ""
            val instructorId = doc.getString("instructorId") ?: ""
            val instructorName = doc.getString("instructorName") ?: ""
            val price = doc.getDouble("price") ?: 0.0
            val category = doc.getString("category") ?: ""
            val lessonsJson = doc.getString("lessonsJson") ?: "[]"
            val rating = doc.getDouble("rating")?.toFloat() ?: 4.5f
            val reviewsCount = doc.getLong("reviewsCount")?.toInt() ?: 0
            
            CourseEntity(id, title, description, instructorId, instructorName, price, category, lessonsJson, rating, reviewsCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping doc ${doc.id} to CourseEntity: ${e.message}")
            null
        }
    }

    private fun mapDocToNote(doc: DocumentSnapshot): NoteEntity? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: ""
            val description = doc.getString("description") ?: ""
            val price = doc.getDouble("price") ?: 0.0
            val category = doc.getString("category") ?: ""
            val previewText = doc.getString("previewText") ?: ""
            val downloadUrl = doc.getString("downloadUrl") ?: ""
            val sellerId = doc.getString("sellerId") ?: ""
            val sellerName = doc.getString("sellerName") ?: ""
            val rating = doc.getDouble("rating")?.toFloat() ?: 4.8f
            val reviewsCount = doc.getLong("reviewsCount")?.toInt() ?: 0

            NoteEntity(id, title, description, price, category, previewText, downloadUrl, sellerId, sellerName, rating, reviewsCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping doc ${doc.id} to NoteEntity: ${e.message}")
            null
        }
    }

    /**
     * Seeds educational resources into Firestore.
     */
    suspend fun seedEducationalResourcesIfNeeded(context: Context, localResources: List<EducationalResourceEntity>) {
        val db = getFirestore(context) ?: return
        try {
            val snap = db.collection("educational_resources").limit(1).get().await()
            if (snap.isEmpty) {
                Log.d(TAG, "Seeding educational resources into Firestore...")
                for (resource in localResources) {
                    addEducationalResourceToFirestore(context, resource)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding educational resources: ${e.message}")
        }
    }

    /**
     * Fetch educational resources from Firestore, optionally filtered.
     */
    suspend fun fetchEducationalResourcesFromFirestore(context: Context, categoryName: String? = null): List<EducationalResourceEntity> {
        val db = getFirestore(context) ?: return emptyList()
        return try {
            var query: Query = db.collection("educational_resources")
            if (!categoryName.isNullOrBlank()) {
                query = query.whereEqualTo("category", categoryName)
            }
            val snap = query.get().await()
            snap.documents.mapNotNull { doc ->
                mapDocToEducationalResource(doc)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching educational resources from Firestore: ${e.message}")
            throw e
        }
    }

    /**
     * Upload an educational resource directly to Firestore collection.
     */
    suspend fun addEducationalResourceToFirestore(context: Context, resource: EducationalResourceEntity): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            val map = hashMapOf(
                "id" to resource.id,
                "title" to resource.title,
                "description" to resource.description,
                "price" to resource.price,
                "category" to resource.category,
                "authorId" to resource.authorId,
                "authorName" to resource.authorName,
                "timestamp" to resource.timestamp,
                "averageRating" to resource.averageRating,
                "reviewsCount" to resource.reviewsCount,
                "isReported" to resource.isReported,
                "reportReason" to resource.reportReason,
                "tags" to resource.tags
            )
            db.collection("educational_resources").document(resource.id).set(map).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading educational resource: ${e.message}")
            false
        }
    }

    /**
     * Delete an educational resource from Firestore collection.
     */
    suspend fun deleteEducationalResourceFromFirestore(context: Context, resourceId: String): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            db.collection("educational_resources").document(resourceId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting educational resource from Firestore: ${e.message}")
            false
        }
    }

    private fun mapDocToEducationalResource(doc: DocumentSnapshot): EducationalResourceEntity? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: ""
            val description = doc.getString("description") ?: ""
            val price = doc.getDouble("price") ?: 0.0
            val category = doc.getString("category") ?: ""
            val authorId = doc.getString("authorId") ?: ""
            val authorName = doc.getString("authorName") ?: "Author"
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val averageRating = doc.getDouble("averageRating") ?: 0.0
            val reviewsCount = doc.getLong("reviewsCount")?.toInt() ?: 0
            val isReported = doc.getBoolean("isReported") ?: false
            val reportReason = doc.getString("reportReason") ?: ""
            val tags = doc.getString("tags") ?: ""

            EducationalResourceEntity(
                id = id,
                title = title,
                description = description,
                price = price,
                category = category,
                authorId = authorId,
                authorName = authorName,
                timestamp = timestamp,
                averageRating = averageRating,
                reviewsCount = reviewsCount,
                isReported = isReported,
                reportReason = reportReason,
                tags = tags
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping doc ${doc.id} to EducationalResourceEntity: ${e.message}")
            null
        }
    }

    /**
     * Fetch comments from a sub-collection of a specific educational resource document in Firestore.
     */
    suspend fun fetchCommentsForResource(context: Context, resourceId: String): List<ResourceComment> {
        val db = getFirestore(context) ?: return emptyList()
        return try {
            val snap = db.collection("educational_resources")
                .document(resourceId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                mapDocToComment(doc, resourceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments for $resourceId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Add a comment to the sub-collection of a specific educational resource document in Firestore.
     */
    suspend fun addCommentToResource(context: Context, comment: ResourceComment): Boolean {
        val db = getFirestore(context) ?: return false
        return try {
            val commentId = comment.id.ifBlank { db.collection("educational_resources").document(comment.resourceId).collection("comments").document().id }
            val map = hashMapOf(
                "id" to commentId,
                "resourceId" to comment.resourceId,
                "userId" to comment.userId,
                "userName" to comment.userName,
                "commentText" to comment.commentText,
                "timestamp" to comment.timestamp
            )
            db.collection("educational_resources")
                .document(comment.resourceId)
                .collection("comments")
                .document(commentId)
                .set(map)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment: ${e.message}")
            false
        }
    }

    private fun mapDocToComment(doc: DocumentSnapshot, resourceId: String): ResourceComment? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val rId = doc.getString("resourceId") ?: resourceId
            val userId = doc.getString("userId") ?: ""
            val userName = doc.getString("userName") ?: "Classmate"
            val commentText = doc.getString("commentText") ?: ""
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

            ResourceComment(id, rId, userId, userName, commentText, timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping doc ${doc.id} to ResourceComment: ${e.message}")
            null
        }
    }
}
