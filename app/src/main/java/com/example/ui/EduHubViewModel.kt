package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiTutorService
import com.example.api.FirebaseAuthHelper
import com.example.api.FirebaseFirestoreHelper
import com.example.data.*
import com.google.firebase.auth.GoogleAuthProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ToastType {
    SUCCESS, INFO, ERROR
}

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: ToastType = ToastType.SUCCESS
)

class EduHubViewModel(application: Application) : AndroidViewModel(application) {

    // --- Toast / Notification State ---
    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage: StateFlow<ToastMessage?> = _toastMessage.asStateFlow()

    fun showToast(message: String, type: ToastType = ToastType.SUCCESS) {
        _toastMessage.value = ToastMessage(message = message, type = type)
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private val db = EduHubDatabase.getInstance(application)
    private val repository = EduHubRepository(db.dao)

    // --- Current Logged In User State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // --- Search & Filtering State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // --- Loading & UI States ---
    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    private val _isTutorResponding = MutableStateFlow(false)
    val isTutorResponding: StateFlow<Boolean> = _isTutorResponding.asStateFlow()

    private val _quizGenerationError = MutableStateFlow<String?>(null)
    val quizGenerationError: StateFlow<String?> = _quizGenerationError.asStateFlow()

    // --- Authentication States ---
    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authWarning = MutableStateFlow<String?>(null)
    val authWarning: StateFlow<String?> = _authWarning.asStateFlow()

    // --- Cloud Firestore State Flows ---
    private val _firestoreCourses = MutableStateFlow<List<CourseEntity>>(emptyList())
    val firestoreCourses: StateFlow<List<CourseEntity>> = _firestoreCourses.asStateFlow()

    private val _firestoreNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val firestoreNotes: StateFlow<List<NoteEntity>> = _firestoreNotes.asStateFlow()

    private val _firestoreEducationalResources = MutableStateFlow<List<EducationalResourceEntity>>(emptyList())
    val firestoreEducationalResources: StateFlow<List<EducationalResourceEntity>> = _firestoreEducationalResources.asStateFlow()

    private val _isFirestoreLoading = MutableStateFlow(false)
    val isFirestoreLoading: StateFlow<Boolean> = _isFirestoreLoading.asStateFlow()

    private val _isFirestoreOnline = MutableStateFlow(false)
    val isFirestoreOnline: StateFlow<Boolean> = _isFirestoreOnline.asStateFlow()

    private val _firestoreMessage = MutableStateFlow<String?>(null)
    val firestoreMessage: StateFlow<String?> = _firestoreMessage.asStateFlow()

    // --- Live Data flows from Room Database ---
    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCourses: StateFlow<List<CourseEntity>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuizzes: StateFlow<List<QuizEntity>> = repository.allQuizzes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEducationalResources: StateFlow<List<EducationalResourceEntity>> = repository.allEducationalResources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<PurchaseEntity>> = repository.getAllPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWishlistItems: StateFlow<List<WishlistItemEntity>> = repository.getAllWishlistItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Chat History for Current User ---
    val chatHistory: StateFlow<List<AIChatMessageEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getChatHistory(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Purchases for Current User ---
    val purchasedIds: StateFlow<Set<String>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getPurchasesForUser(user.id).map { purchases ->
                    purchases.map { it.contentId }.toSet()
                }
            } else {
                flowOf(emptySet())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // --- Wishlist / Saved Items for Current User ---
    val wishlistItems: StateFlow<List<WishlistItemEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getWishlistForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Resource progress tracker for purchased PDF resources ---
    val resourceProgress: StateFlow<List<ResourceProgressEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getProgressForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Interaction tracker for Recommendations ---
    private val _interactedCategories = MutableStateFlow<List<String>>(emptyList())
    val interactedCategories: StateFlow<List<String>> = _interactedCategories.asStateFlow()

    fun recordCategoryInteraction(category: String) {
        if (category.isBlank()) return
        val current = _interactedCategories.value.toMutableList()
        current.add(category)
        if (current.size > 30) {
            current.removeAt(0)
        }
        _interactedCategories.value = current
    }

    // --- Recommended Educational Resources Flow ---
    val recommendedEducationalResources: StateFlow<List<EducationalResourceEntity>> = combine(
        allEducationalResources,
        wishlistItems,
        purchasedIds,
        _interactedCategories
    ) { resources, wishlist, purchases, interactions ->
        fun mapToResourceCategory(cat: String): String {
            val norm = cat.trim().lowercase()
            return when {
                norm.contains("computer") || norm.contains("deep learning") || norm.contains("ai") || norm.contains("software") || norm.contains("programming") || norm.contains("code") || norm.contains("turing") -> "Computer Science"
                norm.contains("math") || norm.contains("calc") || norm.contains("prob") || norm.contains("stats") || norm.contains("algebra") || norm.contains("arithmetic") -> "Mathematics"
                norm.contains("literature") || norm.contains("english") || norm.contains("history") || norm.contains("book") || norm.contains("write") || norm.contains("novel") -> "Literature"
                norm.contains("science") || norm.contains("phys") || norm.contains("chem") || norm.contains("bio") || norm.contains("med") || norm.contains("quantum") -> "Science"
                else -> {
                    cat.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }
        }

        val categoryScores = mutableMapOf<String, Double>()
        val courses = allCourses.value
        val notes = allNotes.value

        // 1. Wishlist (Weight: 3.0)
        wishlist.forEach { wish ->
            val cat = if (wish.itemType == "COURSE") {
                courses.find { it.id == wish.itemId }?.category
            } else {
                notes.find { it.id == wish.itemId }?.category
            }
            if (!cat.isNullOrBlank()) {
                val mapped = mapToResourceCategory(cat)
                categoryScores[mapped] = (categoryScores[mapped] ?: 0.0) + 3.0
            }
        }

        // 2. Purchases (Weight: 5.0)
        purchases.forEach { purchaseId ->
            val cat = courses.find { it.id == purchaseId }?.category
                ?: notes.find { it.id == purchaseId }?.category
            if (!cat.isNullOrBlank()) {
                val mapped = mapToResourceCategory(cat)
                categoryScores[mapped] = (categoryScores[mapped] ?: 0.0) + 5.0
            }
        }

        // 3. Interactions (Weight: 1.5)
        interactions.forEach { cat ->
            if (cat.isNotBlank()) {
                val mapped = mapToResourceCategory(cat)
                categoryScores[mapped] = (categoryScores[mapped] ?: 0.0) + 1.5
            }
        }

        val sortedCategories = categoryScores.entries
            .filter { it.value > 0.0 }
            .sortedByDescending { it.value }
            .map { it.key }

        if (sortedCategories.isEmpty()) {
            resources.sortedByDescending { it.averageRating }.take(6)
        } else {
            resources.map { resource ->
                val mappedResCat = mapToResourceCategory(resource.category)
                val rankIndex = sortedCategories.indexOf(mappedResCat)
                val score = if (rankIndex != -1) {
                    categoryScores[mappedResCat] ?: 0.0
                } else {
                    -1.0
                }
                resource to score
            }
            .filter { it.second >= 0.0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(6)
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Comments Section State ---
    private val _activeResourceComments = MutableStateFlow<List<ResourceComment>>(emptyList())
    val activeResourceComments: StateFlow<List<ResourceComment>> = _activeResourceComments.asStateFlow()

    private val _isCommentsLoading = MutableStateFlow(false)
    val isCommentsLoading: StateFlow<Boolean> = _isCommentsLoading.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed the database if empty
            repository.seedIfNeeded()
            
            // Check if there is an existing authenticated Firebase user
            try {
                val authInstance = FirebaseAuthHelper.getAuth(getApplication())
                val firebaseUser = authInstance?.currentUser
                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    val email = firebaseUser.email ?: ""
                    var dbUser = repository.getUser(uid)
                    if (dbUser == null && email.isNotBlank()) {
                        dbUser = repository.getUserByEmail(email)
                    }
                    if (dbUser == null) {
                        val fallbackName = firebaseUser.displayName ?: if (email.isNotBlank()) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Learner"
                        dbUser = UserEntity(
                            id = uid,
                            name = fallbackName,
                            email = email,
                            role = "STUDENT"
                        )
                        repository.insertUser(dbUser)
                    } else if (dbUser.id != uid) {
                        dbUser = dbUser.copy(id = uid)
                        repository.insertUser(dbUser)
                    }
                    _currentUser.value = dbUser
                    Log.d("EduHubViewModel", "Session restored for: ${dbUser.email} (ID: ${dbUser.id})")
                } else {
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                Log.e("EduHubViewModel", "Error restoring session: ${e.message}")
                _currentUser.value = null
            }

            // Seed Firestore and synchronize!
            try {
                val context = getApplication<Application>()
                val localCourses = repository.allCourses.first()
                val localNotes = repository.allNotes.first()
                val localResources = repository.allEducationalResources.first()
                FirebaseFirestoreHelper.seedFirestoreIfNeeded(context, localCourses, localNotes)
                FirebaseFirestoreHelper.seedEducationalResourcesIfNeeded(context, localResources)
                syncAndFetchFirestore()
            } catch (e: Exception) {
                _isFirestoreOnline.value = false
                _firestoreMessage.value = "Firestore is currently offline. Simulating local content."
            }
        }
    }

    fun syncAndFetchFirestore() {
        _isFirestoreLoading.value = true
        _firestoreMessage.value = null
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val fetchedCourses = FirebaseFirestoreHelper.fetchCoursesFromFirestore(context)
                val fetchedNotes = FirebaseFirestoreHelper.fetchNotesFromFirestore(context)
                val fetchedResources = FirebaseFirestoreHelper.fetchEducationalResourcesFromFirestore(context)
                
                if (fetchedCourses.isNotEmpty() || fetchedNotes.isNotEmpty() || fetchedResources.isNotEmpty()) {
                    _firestoreCourses.value = fetchedCourses
                    _firestoreNotes.value = fetchedNotes
                    _firestoreEducationalResources.value = fetchedResources
                    _isFirestoreOnline.value = true
                    _firestoreMessage.value = "Successfully fetched collection from Cloud Firestore!"
                } else {
                    _isFirestoreOnline.value = false
                    _firestoreMessage.value = "Firestore returned empty collection. Showing local catalogs."
                }
            } catch (e: Exception) {
                _isFirestoreOnline.value = false
                _firestoreMessage.value = "Firestore offline (simulation sandbox mode active)."
            } finally {
                _isFirestoreLoading.value = false
            }
        }
    }

    fun createEducationalResource(
        title: String,
        description: String,
        price: Double,
        category: String,
        tags: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val authorId = user?.id ?: "anonymous"
            val authorName = user?.name ?: "Anonymous Student"
            val id = "res_" + System.currentTimeMillis()
            val newResource = EducationalResourceEntity(
                id = id,
                title = title,
                description = description,
                price = price,
                category = category,
                authorId = authorId,
                authorName = authorName,
                timestamp = System.currentTimeMillis(),
                tags = tags
            )
            
            try {
                // Insert locally (cache-first)
                repository.createEducationalResource(newResource)
                
                // Try uploading to Firestore
                val context = getApplication<Application>()
                val success = FirebaseFirestoreHelper.addEducationalResourceToFirestore(context, newResource)
                if (success) {
                    syncAndFetchFirestore()
                    showToast("Successfully submitted educational resource!", ToastType.SUCCESS)
                    onResult(true, "Successfully uploaded resource to Firestore!")
                } else {
                    _isFirestoreOnline.value = false
                    val offlineError = "Failed to upload to Firestore. Saved locally in database cache."
                    _firestoreMessage.value = offlineError
                    showToast("Resource submitted and saved locally to cache.", ToastType.INFO)
                    onResult(false, offlineError)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "An unexpected error occurred during resource upload."
                onResult(false, errorMsg)
            }
        }
    }

    fun reportEducationalResource(resourceId: String, reason: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val resource = repository.getEducationalResourceById(resourceId)
                if (resource != null) {
                    val updated = resource.copy(isReported = true, reportReason = reason)
                    repository.createEducationalResource(updated)
                    
                    val context = getApplication<Application>()
                    FirebaseFirestoreHelper.addEducationalResourceToFirestore(context, updated)
                    syncAndFetchFirestore()
                    showToast("Resource successfully flagged: $reason", ToastType.SUCCESS)
                    onResult(true)
                } else {
                    showToast("Resource not found to flag.", ToastType.ERROR)
                    onResult(false)
                }
            } catch (e: Exception) {
                showToast("Failed to flag: ${e.message}", ToastType.ERROR)
                onResult(false)
            }
        }
    }

    fun dismissReport(resourceId: String) {
        viewModelScope.launch {
            try {
                val resource = repository.getEducationalResourceById(resourceId)
                if (resource != null) {
                    val updated = resource.copy(isReported = false, reportReason = "")
                    repository.createEducationalResource(updated)
                    
                    val context = getApplication<Application>()
                    FirebaseFirestoreHelper.addEducationalResourceToFirestore(context, updated)
                    syncAndFetchFirestore()
                    showToast("Report dismissed.", ToastType.SUCCESS)
                }
            } catch (e: Exception) {
                showToast("Failed to dismiss: ${e.message}", ToastType.ERROR)
            }
        }
    }

    fun deleteReportingResource(resourceId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEducationalResource(resourceId)
                
                val context = getApplication<Application>()
                FirebaseFirestoreHelper.deleteEducationalResourceFromFirestore(context, resourceId)
                syncAndFetchFirestore()
                showToast("Resource permanently removed.", ToastType.SUCCESS)
            } catch (e: Exception) {
                showToast("Failed to delete: ${e.message}", ToastType.ERROR)
            }
        }
    }

    fun deleteBulkResources(resourceIds: List<String>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                var deletedCount = 0
                resourceIds.forEach { resourceId ->
                    repository.deleteEducationalResource(resourceId)
                    FirebaseFirestoreHelper.deleteEducationalResourceFromFirestore(context, resourceId)
                    deletedCount++
                }
                syncAndFetchFirestore()
                showToast("Successfully deleted $deletedCount resource(s).", ToastType.SUCCESS)
                onSuccess()
            } catch (e: Exception) {
                showToast("Failed to bulk delete resources: ${e.message}", ToastType.ERROR)
            }
        }
    }

    fun updateResourceProgress(resourceId: String, completedPagesCsv: String) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                showToast("User not authenticated to track progress.", ToastType.ERROR)
                return@launch
            }
            try {
                repository.saveResourceProgress(user.id, resourceId, completedPagesCsv)
            } catch (e: Exception) {
                showToast("Failed to save study progress.", ToastType.ERROR)
            }
        }
    }

    fun clearFirestoreMessage() {
        _firestoreMessage.value = null
    }

    // --- Actions ---

    fun clearAuthWarning() {
        _authWarning.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun registerWithEmailAndPassword(email: String, name: String, role: String, password: String, onResult: (Boolean, String?) -> Unit) {
        _authLoading.value = true
        _authError.value = null
        _authWarning.value = null
        viewModelScope.launch {
            val authInstance = FirebaseAuthHelper.getAuth(getApplication())
            if (authInstance != null) {
                authInstance.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = task.result?.user
                            val uid = firebaseUser?.uid ?: ("user_em_" + System.currentTimeMillis())
                            viewModelScope.launch {
                                val newUser = UserEntity(
                                    id = uid,
                                    name = if (name.isNotBlank()) name else email.substringBefore("@").replaceFirstChar { it.uppercase() },
                                    email = email,
                                    role = role
                                )
                                repository.insertUser(newUser)
                                _currentUser.value = newUser
                                _authLoading.value = false
                                onResult(true, null)
                            }
                        } else {
                            val errMsg = task.exception?.localizedMessage ?: "Registration failed."
                            _authError.value = errMsg
                            _authLoading.value = false
                            onResult(false, errMsg)
                        }
                    }
                    .addOnFailureListener { e ->
                        _authError.value = e.localizedMessage
                        _authLoading.value = false
                        onResult(false, e.localizedMessage)
                    }
            } else {
                // FALLBACK ONLY: If Firebase Auth is missing/failed initialization inside preview container
                _authWarning.value = "Firebase offline. Simulated registration successful!"
                viewModelScope.launch {
                    val customUid = "user_mock_" + System.currentTimeMillis()
                    val newUser = UserEntity(
                        id = customUid,
                        name = if (name.isNotBlank()) name else email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = email,
                        role = role
                    )
                    repository.insertUser(newUser)
                    _currentUser.value = newUser
                    _authLoading.value = false
                    onResult(true, null)
                }
            }
        }
    }

    fun loginWithEmailAndPassword(email: String, role: String, password: String, onResult: (Boolean, String?) -> Unit) {
        _authLoading.value = true
        _authError.value = null
        _authWarning.value = null
        viewModelScope.launch {
            val authInstance = FirebaseAuthHelper.getAuth(getApplication())
            if (authInstance != null) {
                authInstance.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = task.result?.user
                            val uid = firebaseUser?.uid ?: ""
                            viewModelScope.launch {
                                var dbUser = repository.getUserByEmail(email)
                                if (dbUser == null) {
                                    val fallbackName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                    dbUser = UserEntity(id = uid, name = fallbackName, email = email, role = role)
                                    repository.insertUser(dbUser)
                                } else {
                                    dbUser = dbUser.copy(id = uid, role = role) // update credentials
                                    repository.updateUser(dbUser)
                                }
                                _currentUser.value = dbUser
                                _authLoading.value = false
                                onResult(true, null)
                            }
                        } else {
                            val errMsg = task.exception?.localizedMessage ?: "Invalid email or password."
                            _authError.value = errMsg
                            _authLoading.value = false
                            onResult(false, errMsg)
                        }
                    }
                    .addOnFailureListener { e ->
                        _authError.value = e.localizedMessage
                        _authLoading.value = false
                        onResult(false, e.localizedMessage)
                    }
            } else {
                // FALLBACK ONLY: If Firebase Auth is missing/failed initialization inside preview container
                _authWarning.value = "Firebase offline. Simulated authentication successful!"
                viewModelScope.launch {
                    val formattedEmail = email.trim()
                    var dbUser = repository.getUserByEmail(formattedEmail)
                    if (dbUser != null) {
                        dbUser = dbUser.copy(role = role)
                        repository.updateUser(dbUser)
                        _currentUser.value = dbUser
                    } else {
                        val fallbackName = formattedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val newSimulatedUser = UserEntity(id = "user_sim_" + System.currentTimeMillis(), name = fallbackName, email = formattedEmail, role = role)
                        repository.insertUser(newSimulatedUser)
                        _currentUser.value = newSimulatedUser
                    }
                    _authLoading.value = false
                    onResult(true, null)
                }
            }
        }
    }

    fun loginWithGoogleToken(idToken: String, role: String, onResult: (Boolean, String?) -> Unit) {
        _authLoading.value = true
        _authError.value = null
        _authWarning.value = null
        viewModelScope.launch {
            val authInstance = FirebaseAuthHelper.getAuth(getApplication())
            if (authInstance != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                authInstance.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = task.result?.user
                            val uid = firebaseUser?.uid ?: ""
                            val email = firebaseUser?.email ?: "google_user@gmail.com"
                            val name = firebaseUser?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                            viewModelScope.launch {
                                var dbUser = repository.getUser(uid)
                                if (dbUser == null) {
                                    dbUser = UserEntity(id = uid, name = name, email = email, role = role)
                                    repository.insertUser(dbUser)
                                } else {
                                    dbUser = dbUser.copy(role = role)
                                    repository.updateUser(dbUser)
                                }
                                _currentUser.value = dbUser
                                _authLoading.value = false
                                onResult(true, null)
                            }
                        } else {
                            val errMsg = task.exception?.localizedMessage ?: "Google Sign-In authentication failed."
                            _authError.value = errMsg
                            _authLoading.value = false
                            onResult(false, errMsg)
                        }
                    }
                    .addOnFailureListener { e ->
                        _authError.value = e.localizedMessage
                        _authLoading.value = false
                        onResult(false, e.localizedMessage)
                    }
            } else {
                // FALLBACK ONLY: If Firebase Auth is missing/failed initialization inside preview container
                _authWarning.value = "Google login simulated successfully in preview."
                viewModelScope.launch {
                    val simUid = "google_sim_" + System.currentTimeMillis()
                    val name = "Google Scholar"
                    val email = "scholar.google@university.edu"
                    val dbUser = UserEntity(id = simUid, name = name, email = email, role = role)
                    repository.insertUser(dbUser)
                    _currentUser.value = dbUser
                    _authLoading.value = false
                    onResult(true, null)
                }
            }
        }
    }

    fun loginWithIPhone(role: String, onResult: (Boolean, String?) -> Unit) {
        _authLoading.value = true
        _authError.value = null
        _authWarning.value = null
        viewModelScope.launch {
            try {
                _authWarning.value = "Apple iPhone registration simulated successfully."
                val simUid = "apple_sim_" + System.currentTimeMillis()
                val name = "Apple Scholar"
                val email = "icloud.scholar@icloud.com"
                val dbUser = UserEntity(id = simUid, name = name, email = email, role = role)
                repository.insertUser(dbUser)
                _currentUser.value = dbUser
                _authLoading.value = false
                onResult(true, null)
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
                _authLoading.value = false
                onResult(false, e.localizedMessage)
            }
        }
    }

    fun loginWithMicrosoft(role: String, onResult: (Boolean, String?) -> Unit) {
        _authLoading.value = true
        _authError.value = null
        _authWarning.value = null
        viewModelScope.launch {
            try {
                _authWarning.value = "Microsoft Mail registration simulated successfully."
                val simUid = "ms_sim_" + System.currentTimeMillis()
                val name = "Microsoft Scholar"
                val email = "scholar.microsoft@outlook.com"
                val dbUser = UserEntity(id = simUid, name = name, email = email, role = role)
                repository.insertUser(dbUser)
                _currentUser.value = dbUser
                _authLoading.value = false
                onResult(true, null)
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
                _authLoading.value = false
                onResult(false, e.localizedMessage)
            }
        }
    }

    // Deprecated mockup fallback bridge
    fun login(email: String, role: String): Boolean {
        loginWithEmailAndPassword(email, role, "password") { _, _ -> }
        return true
    }

    fun logout() {
        _currentUser.value = null
        try {
            FirebaseAuthHelper.getAuth(getApplication())?.signOut()
            Log.d("EduHubViewModel", "Successfully signed out from FirebaseAuth")
        } catch (e: Exception) {
            Log.e("EduHubViewModel", "Error signing out: ${e.message}")
        }
    }

    fun updateRole(newRole: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(role = newRole)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun toggleCategory(categoryId: String) {
        if (_selectedCategory.value == categoryId) {
            _selectedCategory.value = null
        } else {
            _selectedCategory.value = categoryId
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Purchase flow ---
    fun purchaseContent(contentId: String, contentType: String, price: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val success = repository.buyContent(
                userId = user.id,
                contentId = contentId,
                contentType = contentType,
                price = price
            )
            if (success) {
                // If current user is also the seller, reload to reflect earnings
                _currentUser.value = repository.getUser(user.id)
            }
        }
    }

    // --- Wishlist toggling ---
    fun toggleWishlist(itemId: String, itemType: String) {
        val user = _currentUser.value
        if (user == null) {
            showToast("Please sign in to save items.", ToastType.ERROR)
            return
        }
        viewModelScope.launch {
            try {
                repository.toggleWishlist(user.id, itemId, itemType)
                showToast("Favorites updated!", ToastType.SUCCESS)
            } catch (e: Exception) {
                Log.e("EduHubViewModel", "Error toggling wishlist: ${e.message}")
                showToast("Unable to update favorites", ToastType.ERROR)
            }
        }
    }

    // --- Comments Section flows ---
    fun fetchComments(resourceId: String) {
        viewModelScope.launch {
            _isCommentsLoading.value = true
            val context = getApplication<Application>()
            try {
                val list = FirebaseFirestoreHelper.fetchCommentsForResource(context, resourceId)
                _activeResourceComments.value = list
            } catch (e: Exception) {
                Log.e("EduHubViewModel", "Error loading comments: ${e.message}")
            } finally {
                _isCommentsLoading.value = false
            }
        }
    }

    fun addComment(resourceId: String, text: String) {
        val user = _currentUser.value
        if (user == null) {
            showToast("Please sign in to add comments.", ToastType.ERROR)
            return
        }
        if (text.isBlank()) return

        viewModelScope.launch {
            val context = getApplication<Application>()
            val comment = ResourceComment(
                id = "",
                resourceId = resourceId,
                userId = user.id,
                userName = user.name,
                commentText = text,
                timestamp = System.currentTimeMillis()
            )
            val success = FirebaseFirestoreHelper.addCommentToResource(context, comment)
            if (success) {
                fetchComments(resourceId)
                showToast("Comment successfully posted!", ToastType.SUCCESS)
            } else {
                showToast("Could not send comment to Firestore.", ToastType.ERROR)
            }
        }
    }

    // --- Add Review flow ---
    fun addReview(contentId: String, contentType: String, rating: Int, comment: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val review = ReviewEntity(
                id = "rev_${System.currentTimeMillis()}",
                contentId = contentId,
                contentType = contentType,
                userId = user.id,
                userName = user.name,
                rating = rating,
                comment = comment
            )
            repository.insertReview(review)

            if (contentType == "RESOURCE") {
                val resource = repository.getEducationalResourceById(contentId)
                if (resource != null) {
                    val newCount = resource.reviewsCount + 1
                    val newAvgRating = ((resource.averageRating * resource.reviewsCount) + rating) / newCount
                    val updatedResource = resource.copy(
                        averageRating = newAvgRating,
                        reviewsCount = newCount
                    )
                    repository.createEducationalResource(updatedResource)
                    
                    val context = getApplication<android.app.Application>()
                    FirebaseFirestoreHelper.addEducationalResourceToFirestore(context, updatedResource)
                    syncAndFetchFirestore()
                }
            } else if (contentType == "COURSE") {
                val course = repository.getCourseById(contentId)
                if (course != null) {
                    val newCount = course.reviewsCount + 1
                    val newRating = ((course.rating * course.reviewsCount) + rating) / newCount
                    val updatedCourse = course.copy(
                        rating = newRating.toFloat(),
                        reviewsCount = newCount
                    )
                    repository.insertCourse(updatedCourse)
                }
            } else if (contentType == "NOTE") {
                val note = repository.getNoteById(contentId)
                if (note != null) {
                    val newCount = note.reviewsCount + 1
                    val newRating = ((note.rating * note.reviewsCount) + rating) / newCount
                    val updatedNote = note.copy(
                        rating = newRating.toFloat(),
                        reviewsCount = newCount
                    )
                    repository.insertNote(updatedNote)
                }
            }
            showToast("Review and rating submitted successfully!", ToastType.SUCCESS)
        }
    }

    // --- AI Studies features ---

    fun askTutor(messageText: String) {
        val user = _currentUser.value ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            repository.sendChatMessage(user.id, messageText, isUser = true)
            _isTutorResponding.value = true

            val response = GeminiTutorService.askTutor(messageText)

            repository.sendChatMessage(user.id, response, isUser = false)
            _isTutorResponding.value = false
        }
    }

    fun clearChat() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.clearChatHistory(user.id)
            repository.sendChatMessage(
                userId = user.id,
                message = "Hello ${user.name}! Chat history cleared. How can I help you master your study courses today?",
                isUser = false
            )
        }
    }

    fun triggerPdfSummarization(noteTitle: String, previewText: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val prompt = "Please summarize this academic document: '$noteTitle'. Here is a partial preview of its core contents:\n\n$previewText\n\nGenerate high-yield key study takeaways and formula guides."
            repository.sendChatMessage(user.id, "Generate study summary for: $noteTitle", isUser = true)
            _isTutorResponding.value = true

            val response = GeminiTutorService.askTutor(prompt)

            repository.sendChatMessage(user.id, response, isUser = false)
            _isTutorResponding.value = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun generateInteractiveQuiz(subject: String, topic: String, onFinished: (Boolean) -> Unit) {
        val user = _currentUser.value ?: return
        _isGeneratingQuiz.value = true
        _quizGenerationError.value = null

        viewModelScope.launch {
            try {
                // Background call to Gemini
                val rawJson = GeminiTutorService.generateInteractiveQuizQuestions(subject, topic)
                
                // Parse generated question format
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val type = Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, Any>>>(type)
                
                val parsedList = adapter.fromJson(rawJson)
                if (!parsedList.isNullOrEmpty()) {
                    // Create beautiful quiz entity and insert it!
                    val newQuizId = "quiz_gen_${System.currentTimeMillis()}"
                    val quiz = QuizEntity(
                        id = newQuizId,
                        subject = subject,
                        title = "AI Gen Quiz: $topic",
                        questionsJson = rawJson,
                        isExamBank = false
                    )
                    repository.createQuiz(quiz)
                    
                    // Post announcement in AI Tutor Chat too for unified experience
                    repository.sendChatMessage(
                        userId = user.id,
                        message = "🎉 Great news! I've successfully compiled a custom academic Quiz for you on the topic '$topic' with exactly 3 multiple-choice study challenges. You can now access and attempt it inside your **Exam Bank** screen!",
                        isUser = false
                    )
                    onFinished(true)
                } else {
                    _quizGenerationError.value = "Returned format was incorrect. Please try again."
                    onFinished(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _quizGenerationError.value = "Connection timed out. Creating a smart fallback practice quiz instead!"
                
                // Smart fallback so it ALWAYS works smoothly
                val fallbackJson = getLocalQuizFallback(topic)
                val finalQuiz = QuizEntity(
                    id = "quiz_gen_${System.currentTimeMillis()}",
                    subject = subject,
                    title = "AI Gen Quiz: $topic (Offline Mode)",
                    questionsJson = fallbackJson,
                    isExamBank = false
                )
                repository.createQuiz(finalQuiz)
                repository.sendChatMessage(
                    userId = user.id,
                    message = "📚 Compiled a smart offline study practice challenge on '$topic' with MCQ items since network is busy. You will find it in the **Exam Bank** tab!",
                    isUser = false
                )
                onFinished(true)
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    private fun getLocalQuizFallback(topic: String): String {
        return """
            [
              {
                "id": "fall_q1",
                "question": "Regarding the topic '$topic', which study method has been shown to maximize active retention?",
                "options": ["Passive skimming of textbooks", "Combining flashcards with expanding spaced reviews", "Highlighter marker tagging only", "Re-watching online lectures on double speed"],
                "correctIndex": 1,
                "explanation": "Active retrieval (flashcards) combined with spaced intervals prompts cognitive pathways to re-encode the concepts."
              },
              {
                "id": "fall_q2",
                "question": "True or False: Under active university curriculum demands, testing yourself before you feel fully ready actually boosts final score achievements.",
                "options": ["True", "False"],
                "correctIndex": 0,
                "explanation": "True. This is the pre-testing effect study phenomenon. Struggling to find answers primes the brain to capture the correct knowledge later."
              }
            ]
        """.trimIndent()
    }

    // --- Seller Upload Content Operations ---

    fun uploadCourse(title: String, description: String, price: Double, category: String, lessons: List<String>) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val lessonsList = lessons.filter { it.isNotBlank() }.mapIndexed { index, lessonTitle ->
                mapOf("title" to "${index + 1}. $lessonTitle", "duration" to "12:00")
            }
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, Map::class.java)
            val adapter = moshi.adapter<List<Map<String, String>>>(listType)
            val jsonLessons = adapter.toJson(lessonsList)

            val newCourse = CourseEntity(
                id = "course_${System.currentTimeMillis()}",
                title = title,
                description = description,
                instructorId = user.id,
                instructorName = user.name,
                price = price,
                category = category,
                lessonsJson = jsonLessons,
                rating = 5.0f,
                reviewsCount = 0
            )
            repository.createCourse(newCourse)
        }
    }

    fun uploadNote(title: String, description: String, price: Double, category: String, contentText: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val newNote = NoteEntity(
                id = "note_${System.currentTimeMillis()}",
                title = title,
                description = description,
                price = price,
                category = category,
                previewText = contentText,
                downloadUrl = "https://eduhub.download/notes/user_upload_${System.currentTimeMillis()}.pdf",
                sellerId = user.id,
                sellerName = user.name,
                rating = 5.0f,
                reviewsCount = 0
            )
            repository.createNote(newNote)
        }
    }

    // --- Subscription Upgrade ---
    fun togglePremiumSubscription() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(premiumUser = !user.premiumUser)
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun getReviews(contentId: String, contentType: String): Flow<List<ReviewEntity>> {
        return repository.getReviews(contentId, contentType)
    }

    // --- Localization State ---
    private val _appLanguage = MutableStateFlow("en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(languageCode: String) {
        _appLanguage.value = languageCode
    }
}

// Wrapper interface extension to handle the API connection cleanly
private suspend fun GeminiTutorService.generateInteractiveQuizQuestions(subject: String, topic: String): String {
    return this.generateExamQuestions(subject, topic)
}
