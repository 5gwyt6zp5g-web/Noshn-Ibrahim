package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EduHubRepository(private val dao: EduHubDao) {

    // --- Exposed Flows ---
    val categories: Flow<List<CategoryEntity>> = dao.getCategories()
    val allCourses: Flow<List<CourseEntity>> = dao.getAllCourses()
    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val allQuizzes: Flow<List<QuizEntity>> = dao.getAllQuizzes()
    val allEducationalResources: Flow<List<EducationalResourceEntity>> = dao.getAllEducationalResources()

    fun getCoursesByCategory(category: String): Flow<List<CourseEntity>> = dao.getCoursesByCategory(category)
    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> = dao.getNotesByCategory(category)
    
    fun getCoursesByInstructor(instructorId: String): Flow<List<CourseEntity>> = dao.getCoursesByInstructor(instructorId)
    fun getNotesBySeller(sellerId: String): Flow<List<NoteEntity>> = dao.getNotesBySeller(sellerId)

    fun getQuizzesByType(isExamBank: Boolean): Flow<List<QuizEntity>> = dao.getQuizzesByType(isExamBank)

    fun getChatHistory(userId: String): Flow<List<AIChatMessageEntity>> = dao.getChatHistory(userId)
    fun getAllPurchases(): Flow<List<PurchaseEntity>> = dao.getAllPurchases()
    fun getPurchasesForUser(userId: String): Flow<List<PurchaseEntity>> = dao.getPurchasesForUser(userId)
    fun checkPurchase(userId: String, contentId: String, contentType: String): Flow<Boolean> = dao.checkPurchase(userId, contentId, contentType)
    fun getAllWishlistItems(): Flow<List<WishlistItemEntity>> = dao.getAllWishlistItems()
    fun getWishlistForUser(userId: String): Flow<List<WishlistItemEntity>> = dao.getWishlistForUser(userId)
    fun checkWishlist(userId: String, itemId: String): Flow<Boolean> = dao.checkWishlist(userId, itemId)

    // --- Suspended Operations ---
    suspend fun getUser(id: String): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUser(id)
    }

    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUserByEmail(email)
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.updateUser(user)
    }

    suspend fun createCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
        dao.insertCourse(course)
    }

    suspend fun createNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        dao.insertNote(note)
    }

    suspend fun getCourseById(courseId: String): CourseEntity? = withContext(Dispatchers.IO) {
        dao.getCourseById(courseId)
    }

    suspend fun insertCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
        dao.insertCourse(course)
    }

    suspend fun getNoteById(noteId: String): NoteEntity? = withContext(Dispatchers.IO) {
        dao.getNoteById(noteId)
    }

    suspend fun insertNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        dao.insertNote(note)
    }

    suspend fun createQuiz(quiz: QuizEntity) = withContext(Dispatchers.IO) {
        dao.insertQuiz(quiz)
    }

    suspend fun createEducationalResource(resource: EducationalResourceEntity) = withContext(Dispatchers.IO) {
        dao.insertEducationalResource(resource)
    }

    suspend fun deleteEducationalResource(resourceId: String) = withContext(Dispatchers.IO) {
        dao.deleteEducationalResourceById(resourceId)
    }

    suspend fun getEducationalResourceById(resourceId: String): EducationalResourceEntity? = withContext(Dispatchers.IO) {
        dao.getEducationalResourceById(resourceId)
    }

    suspend fun insertEducationalResources(resources: List<EducationalResourceEntity>) = withContext(Dispatchers.IO) {
        dao.insertEducationalResources(resources)
    }

    suspend fun toggleWishlist(userId: String, itemId: String, itemType: String) = withContext(Dispatchers.IO) {
        val exists = dao.checkWishlist(userId, itemId).first()
        if (exists) {
            dao.deleteWishlistItem(userId, itemId)
        } else {
            dao.insertWishlistItem(WishlistItemEntity(userId, itemId, itemType))
        }
    }

    suspend fun sendChatMessage(userId: String, message: String, isUser: Boolean) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(AIChatMessageEntity(userId = userId, message = message, isUser = isUser))
    }

    suspend fun clearChatHistory(userId: String) = withContext(Dispatchers.IO) {
        dao.clearChatHistory(userId)
    }

    // --- Resource Progress Operations ---
    fun getProgressForUser(userId: String): Flow<List<ResourceProgressEntity>> = dao.getProgressForUser(userId)

    suspend fun getProgressForResource(userId: String, resourceId: String): ResourceProgressEntity? = withContext(Dispatchers.IO) {
        dao.getProgressForResource(userId, resourceId)
    }

    suspend fun saveResourceProgress(userId: String, resourceId: String, completedPagesCsv: String) = withContext(Dispatchers.IO) {
        val progress = ResourceProgressEntity(
            userId = userId,
            resourceId = resourceId,
            completedPagesCsv = completedPagesCsv,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertProgress(progress)
    }

    fun getReviews(contentId: String, contentType: String): Flow<List<ReviewEntity>> {
        return dao.getReviews(contentId, contentType)
    }

    suspend fun insertReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        dao.insertReview(review)
    }

    // --- Monetization and Purchases Flow ---
    /**
     * Handles student purchasing courses or notes.
     * Implements the requested monetization model:
     * Seller receives 70% of the sale, and EduHub tracks the 30% commission.
     */
    suspend fun buyContent(userId: String, contentId: String, contentType: String, price: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            // Register purchase record
            val purchaseId = "${userId}_${contentId}_${System.currentTimeMillis()}"
            val purchase = PurchaseEntity(
                id = purchaseId,
                userId = userId,
                contentId = contentId,
                contentType = contentType,
                pricePaid = price
            )
            dao.insertPurchase(purchase)

            // Calculate Instructor's earnings (70% of the price)
            val sellerPayout = price * 0.70

            // Find seller/instructor id
            val sellerId = when (contentType) {
                "COURSE" -> dao.getCourseById(contentId)?.instructorId
                "NOTE" -> dao.getNoteById(contentId)?.sellerId
                "RESOURCE" -> dao.getEducationalResourceById(contentId)?.authorId
                else -> null
            }

            if (sellerId != null) {
                val seller = dao.getUser(sellerId)
                if (seller != null) {
                    val updatedSeller = seller.copy(earnings = seller.earnings + sellerPayout)
                    dao.insertUser(updatedSeller)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Default Seeding ---
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        // Only seed if categories list is empty
        val existingCategories = dao.getCategories().first()
        if (existingCategories.isEmpty()) {
            // 1. Seed Categories
            val defaultCategories = listOf(
                CategoryEntity("cs", "Computer Science", "Computer"),
                CategoryEntity("eng", "Engineering", "Build"),
                CategoryEntity("bus", "Business", "MonetizationOn"),
                CategoryEntity("med", "Medicine", "MedicalServices"),
                CategoryEntity("mat", "Mathematics", "Calculate")
            )
            dao.insertCategories(defaultCategories)

            // 2. Seed Users
            val systemUsers = listOf(
                UserEntity("user_student", "Alex Johnson", "alex@u.edu", "STUDENT", premiumUser = false),
                UserEntity("user_instructor1", "Dr. Robert Thorne", "thorne@u.edu", "INSTRUCTOR", earnings = 245.50),
                UserEntity("user_instructor2", "Sarah Jenkins, CFA", "jenkins@u.edu", "INSTRUCTOR", earnings = 520.00),
                UserEntity("user_admin", "System Administrator", "admin@eduhub.com", "ADMIN")
            )
            for (u in systemUsers) {
                dao.insertUser(u)
            }

            // 3. Seed Courses
            val course1 = CourseEntity(
                id = "course_compose",
                title = "Jetpack Compose: Modern UI Architectures",
                description = "Master Compose with state flows, lifecycle safety, clean Material Design 3, custom canvas drawing, and high-performance list structures. Designed specifically for engineering students looking into native Android app development.",
                instructorId = "user_instructor1",
                instructorName = "Dr. Robert Thorne",
                price = 39.99,
                category = "Computer Science",
                lessonsJson = """
                    [
                        {"title": "1. Jetpack Compose Core Elements", "duration": "14:20"},
                        {"title": "2. Working with State Managers & ViewModels", "duration": "18:45"},
                        {"title": "3. Material Design 3 Styling & Customization", "duration": "22:10"},
                        {"title": "4. Multi-screen Routing & Safe Arguments", "duration": "11:55"},
                        {"title": "5. Local Storage integration with Room", "duration": "25:30"}
                    ]
                """.trimIndent(),
                rating = 4.8f,
                reviewsCount = 42
            )
            val course2 = CourseEntity(
                id = "course_thermo",
                title = "Engineering Thermodynamics I",
                description = "Comprehensive university syllabus on thermodynamics. Covering first and second law of thermodynamics, heat engines, absolute temperature scales, pure substances properties, cycles, and physical reaction systems.",
                instructorId = "user_instructor1",
                instructorName = "Dr. Robert Thorne",
                price = 29.99,
                category = "Engineering",
                lessonsJson = """
                    [
                        {"title": "1. Basic System Concepts & Units", "duration": "18:10"},
                        {"title": "2. Kinetic Energy, Potential Energy & First Law", "duration": "24:35"},
                        {"title": "3. Pure Substances & Phase Diagrams", "duration": "29:50"},
                        {"title": "4. Carnot Heat Engine Principles", "duration": "32:45"},
                        {"title": "5. Entropy and Practical Power Cycles", "duration": "41:15"}
                    ]
                """.trimIndent(),
                rating = 4.4f,
                reviewsCount = 18
            )
            val course3 = CourseEntity(
                id = "course_finance",
                title = "Corporate Finance & Capital markets",
                description = "Introduction to capital budgeting rules, valuation of assets and equities, cash flow estimation, weighted average cost of capital (WACC), debt structures, risk assessment, and financial derivatives.",
                instructorId = "user_instructor2",
                instructorName = "Sarah Jenkins, CFA",
                price = 49.99,
                category = "Business",
                lessonsJson = """
                    [
                        {"title": "1. Corporate Structure and Financial Reporting", "duration": "22:15"},
                        {"title": "2. Time Value of Money & Discounted Cash Flows", "duration": "19:40"},
                        {"title": "3. Capital Budgeting Rules: NPV vs IRR", "duration": "30:20"},
                        {"title": "4. Valuation of Stocks and Corporate Bonds", "duration": "28:50"},
                        {"title": "5. Risk, Return, and the CAPM Formula", "duration": "35:10"}
                    ]
                """.trimIndent(),
                rating = 4.9f,
                reviewsCount = 37
            )
            dao.insertCourse(course1)
            dao.insertCourse(course2)
            dao.insertCourse(course3)

            // 4. Seed Notes (PDFs)
            val note1 = NoteEntity(
                id = "note_ds",
                title = "Data Structures & Trees Study Note",
                description = "An absolute lifesaver summary sheet covering binary search trees, red-black trees, AVL trees, heap structures, Big O worst/average complexities, and complete traversal pseudo-codes.",
                price = 4.99,
                category = "Computer Science",
                previewText = """
                    DATA STRUCTURES REVISION GUIDE
                    
                    1. BIG-O COMPLEXITY REFERENCE
                    ----------------------------------------------
                    * Array Access: O(1)
                    * Stack Push/Pop: O(1)
                    * Binary Search Tree Search: O(log n) average, O(n) worst-case.
                    * Hash Table insertion: O(1) average.
                    
                    2. BINARY TREES TRAVERSAL METHODS
                    * Pre-Order: Root -> Left -> Right
                    * In-Order: Left -> Root -> Right (returns sorted items for BST)
                    * Post-Order: Left -> Right -> Root
                    
                    3. BALANCING OPERATIONS (AVL & RED-BLACK)
                    In an AVL, height difference between subtrees can be at most 1...
                """.trimIndent(),
                downloadUrl = "https://eduhub.download/notes/ds_trees_revision.pdf",
                sellerId = "user_instructor1",
                sellerName = "Dr. Robert Thorne",
                rating = 4.9f,
                reviewsCount = 54
            )
            val note2 = NoteEntity(
                id = "note_ecg",
                title = "12-Lead ECG Interpretation Summary",
                description = "Handwritten color-coded medical study summary on understanding electrical axes, heart blocks, bundle branch blocks, acute myocardial infarctions, electrolyte abnormalities, and step-by-step reading schemas.",
                price = 7.99,
                category = "Medicine",
                previewText = """
                    12-LEAD ECG QUICK CLINICAL REVIEW
                    
                    1. AXIS DETERMINATION
                    * Normal Axis: Lead I is (+), aVF is (+) (-30 to +90 degrees)
                    * Left Axis Deviation: Lead I is (+), aVF is (-) (Check Lead II: if negative, LAD is confirmed)
                    * Right Axis Deviation: Lead I is (-), aVF is (+)
                    
                    2. SEGMENT STRETCHING (INFARCTION LOGIC)
                    * Anterior Wall: ST elevation in V1-V4 (LAD artery)
                    * Lateral Wall: ST elevation in I, aVL, V5-V6 (Circumflex artery)
                    * Inferior Wall: ST elevation in II, III, aVF (Right Coronary artery)
                    
                    3. AV BLOCKS DIAGNOSIS
                    * First degree: PR interval is constant and > 0.20 seconds...
                """.trimIndent(),
                downloadUrl = "https://eduhub.download/notes/ecg_interpretation_12_lead.pdf",
                sellerId = "user_instructor2",
                sellerName = "Sarah Jenkins, CFA",
                rating = 4.7f,
                reviewsCount = 29
            )
            val note3 = NoteEntity(
                id = "note_calc",
                title = "Calculus II Integration Techniques Cheat Sheet",
                description = "Easy to follow reference notes detailing integration by parts, trigonometric substitutions, partial fraction decomposition, improper integrations, and Taylor series conversions.",
                price = 3.99,
                category = "Mathematics",
                previewText = """
                    CALCULUS II: INTEGRATION AND SERIES
                    
                    1. INTEGRATION BY PARTS FORMULA
                    ∫ u dv = u v - ∫ v du
                    Pro Tip (LIATE rule for picking u):
                    L: Logarithmic, I: Inverse Trig, A: Algebraic, T: Trigonometric, E: Exponential.
                    
                    2. TRIGONOMETRIC SUBSTITUTIONS
                    * For √(a² - x²): Substitute x = a sin(θ), dx = a cos(θ) dθ. Uses: 1 - sin²θ = cos²θ.
                    * For √(a² + x²): Substitute x = a tan(θ), dx = a sec²(θ) dθ. Uses: 1 + tan²θ = sec²θ.
                    * For √(x² - a²): Substitute x = a sec(θ), dx = a sec(θ)tan(θ) dθ. Uses: sec²θ - 1 = tan²θ.
                    
                    3. TAYLOR SERIES EXPANSION
                    f(x) = ∑ [ f^(n)(c) / n! ] * (x - c)^n...
                """.trimIndent(),
                downloadUrl = "https://eduhub.download/notes/calculus2_integration_cheatsheet.pdf",
                sellerId = "user_instructor1",
                sellerName = "Dr. Robert Thorne",
                rating = 4.6f,
                reviewsCount = 19
            )
            dao.insertNote(note1)
            dao.insertNote(note2)
            dao.insertNote(note3)

            // 5. Seed Quizzes
            val quiz1 = QuizEntity(
                id = "quiz_os",
                subject = "Computer Science",
                title = "Operating Systems Past Papers (Spring 2025)",
                questionsJson = """
                    [
                        {
                            "id": "os_q1",
                            "question": "Which scheduling algorithm is non-preemptive and guarantees that no starvation occurs?",
                            "options": [
                                "Shortest Job First (SJF)",
                                "Round Robin (RR)",
                                "First Come, First Served (FCFS)",
                                "Priority Scheduling"
                            ],
                            "correctIndex": 2,
                            "explanation": "First Come First Served (FCFS) is strictly non-preemptive and since every arriving process is eventually appended to the queue, it guarantees no starvation (unlike SJF or Priority)."
                        },
                        {
                            "id": "os_q2",
                            "question": "What is thrashing in an Operating System?",
                            "options": [
                                "High disk activity where the system spends more time page-swapping than executing instructions",
                                "A process terminates abnormally due to memory leakage",
                                "Severe fragmentation in the dynamic stack",
                                "Multiple threads locking each other because of resource dependency"
                            ],
                            "correctIndex": 0,
                            "explanation": "Thrashing occurs when the virtual memory subsystem is in constant state of paging, leading to extremely high disk traffic and total deterioration of performance."
                        },
                        {
                            "id": "os_q3",
                            "question": "A deadlock can be completely avoided if we prevent any of the four Coffman conditions.",
                            "options": [
                                "True",
                                "False"
                            ],
                            "correctIndex": 0,
                            "explanation": "True. The four Coffman conditions (Mutual exclusion, Hold & wait, No preemption, Circular wait) are mutually necessary for deadlock. Eliminating any of them breaks deadlock feasibility."
                        }
                    ]
                """.trimIndent(),
                isExamBank = true
            )
            val quiz2 = QuizEntity(
                id = "quiz_macro",
                subject = "Business",
                title = "Macroeconomics Midterm Practice Exam",
                questionsJson = """
                    [
                        {
                            "id": "macro_q1",
                            "question": "If the central bank increases the reserve requirement ratio, what happens to the money multiplier?",
                            "options": [
                                "It increases",
                                "It decreases",
                                "It remains unchanged",
                                "It turns negative"
                            ],
                            "correctIndex": 1,
                            "explanation": "The money multiplier formula is 1/r. If the reserve ratio (r) increases, the multiplier 1/r decreases, thereby reducing bank lending capacity."
                        },
                        {
                            "id": "macro_q2",
                            "question": "Which of the following describes stagflation?",
                            "options": [
                                "Low unemployment combined with rapid economic growth",
                                "High levels of budget deficits coupled with capital flows",
                                "High inflation occurring together with high unemployment or slow growth",
                                "Persistent deflation combined with structural market surplus"
                            ],
                            "correctIndex": 2,
                            "explanation": "Stagflation is characterized by stagnant economic growth, high unemployment, accompanied by high rates of inflation."
                        },
                        {
                            "id": "macro_q3",
                            "question": "The GDP deflator includes only products and services manufactured domestically.",
                            "options": [
                                "True",
                                "False"
                            ],
                            "correctIndex": 0,
                            "explanation": "True. The GDP deflator measures prices of all goods produced domestically, unlike the CPI which measures a market basket which can include imported consumer items."
                        }
                    ]
                """.trimIndent(),
                isExamBank = false
            )
            dao.insertQuiz(quiz1)
            dao.insertQuiz(quiz2)

            // 6. Prepopulate reviews
            val reviews = listOf(
                ReviewEntity("rev1", "course_compose", "COURSE", "user_student", "Alex Johnson", 5, "Remarkable masterclass, explains everything with actual code instead of slides!"),
                ReviewEntity("rev2", "note_ds", "NOTE", "user_student", "Alex Johnson", 5, "I passed my DS exam because of this! Worth every cent.")
            )
            for (r in reviews) {
                dao.insertReview(r)
            }

            // 7. Initialize Chat greeting from tutor
            dao.insertChatMessage(
                AIChatMessageEntity(
                    userId = "user_student",
                    message = "Hello Alex! I am your AI Virtual Tutor. I can help summarize your documents, explain complex topics (such as computer science or calculus), and generate practice quizzes. Feel free to ask me anything!",
                    isUser = false
                )
            )

            // 8. Prepopulate educational resources (Real world models)
            val resources = listOf(
                EducationalResourceEntity(
                    id = "res_quantum",
                    title = "Quantum Mechanics for Computing Notes",
                    description = "Comprehensive syllabus on Hilbert spaces, bra-ket notation, superposition, entanglement, and simple quantum gates like Hadamard and CNOT. Essential background for quantum programming.",
                    price = 12.99,
                    category = "Computer Science",
                    authorId = "user_instructor1",
                    authorName = "Dr. Robert Thorne"
                ),
                EducationalResourceEntity(
                    id = "res_literature",
                    title = "Introduction to English Literature & Poetry",
                    description = "Analytical syllabus detailing narrative structures, romanticism, modernist prose, and poetic meter. Includes quick-reference outlines for classic Victorian and Elizabethan plays.",
                    price = 8.50,
                    category = "Literature",
                    authorId = "user_instructor2",
                    authorName = "Sarah Jenkins, CFA"
                ),
                EducationalResourceEntity(
                    id = "res_biology",
                    title = "Molecular Biology & Genetic Transcription Guides",
                    description = "Syllabus on RNA synthesis, translation mechanics, chromosomal replication mechanisms, gene expression regulatory loops, and standard recombinant DNA lab techniques.",
                    price = 15.00,
                    category = "Science",
                    authorId = "user_instructor2",
                    authorName = "Sarah Jenkins, CFA"
                ),
                EducationalResourceEntity(
                    id = "res_linear_alg",
                    title = "Linear Algebra Cheat Sheet for Machine Learning",
                    description = "Two-page summary explaining eigenvalues, eigenvectors, singular value decomposition (SVD), principal component analysis (PCA), projection matrices, and high-dimensional transformations.",
                    price = 4.00,
                    category = "Mathematics",
                    authorId = "user_instructor1",
                    authorName = "Dr. Robert Thorne"
                )
            )
            dao.insertEducationalResources(resources)
        }
    }
}
