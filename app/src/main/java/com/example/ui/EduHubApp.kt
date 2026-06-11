package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import java.util.Locale
import com.example.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.util.Log
import com.example.api.FirebaseAuthHelper
import com.example.ui.theme.LocalThemeHelper
import com.example.ui.components.FirestoreSearchBar
import com.example.ui.components.EducationalResourceForm
import com.example.ui.components.UserProfile
import com.example.ui.components.getResourceSections
import androidx.compose.ui.graphics.StrokeCap
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Main Route Enums ---
enum class AppState {
    SPLASH,
    AUTHENTICATION,
    MAIN_HUB
}

enum class NavigationTab(@StringRes val labelId: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Default.Home),
    NOTES(R.string.tab_notes, Icons.Default.Book),
    EXAMS(R.string.tab_exams, Icons.Default.School),
    TUTOR(R.string.tab_tutor, Icons.Default.SmartToy),
    STUDIO(R.string.tab_studio, Icons.Default.AddCircle),
    DASHBOARD(R.string.tab_dashboard, Icons.Default.Person),
    PLAYGROUND(R.string.tab_playground, Icons.Default.Code)
}

@Composable
fun LocalProviders(
    localizedContext: android.content.Context,
    layoutDirection: LayoutDirection,
    currentRegistryOwner: androidx.activity.result.ActivityResultRegistryOwner?,
    content: @Composable () -> Unit
) {
    if (currentRegistryOwner != null) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalLayoutDirection provides layoutDirection,
            LocalActivityResultRegistryOwner provides currentRegistryOwner,
            content = content
        )
    } else {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalLayoutDirection provides layoutDirection,
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EduHubApp(
    viewModel: EduHubViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var currentAppState by remember { mutableStateOf(AppState.SPLASH) }
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    // Observers
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val courses by viewModel.allCourses.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val quizzes by viewModel.allQuizzes.collectAsStateWithLifecycle()
    val purchasedIds by viewModel.purchasedIds.collectAsStateWithLifecycle()
    val isFirestoreOnline by viewModel.isFirestoreOnline.collectAsStateWithLifecycle()
    val isFirestoreLoading by viewModel.isFirestoreLoading.collectAsStateWithLifecycle()
    val firestoreMessage by viewModel.firestoreMessage.collectAsStateWithLifecycle()
    val educationalResources by viewModel.allEducationalResources.collectAsStateWithLifecycle()
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val wishlistIds = remember(wishlistItems) { wishlistItems.map { it.itemId }.toSet() }
    val recommendedResources by viewModel.recommendedEducationalResources.collectAsStateWithLifecycle()
    val allPurchases by viewModel.allPurchases.collectAsStateWithLifecycle(emptyList())
    val resourceProgress by viewModel.resourceProgress.collectAsStateWithLifecycle(emptyList())

    // Detail States
    var selectedCourseForDetail by remember { mutableStateOf<CourseEntity?>(null) }
    var selectedNoteForDetail by remember { mutableStateOf<NoteEntity?>(null) }
    var selectedResourceForDetail by remember { mutableStateOf<EducationalResourceEntity?>(null) }
    var activeQuizForTest by remember { mutableStateOf<QuizEntity?>(null) }

    // Splash Timer
    LaunchedEffect(Unit) {
        delay(1800)
        currentAppState = if (currentUser == null) AppState.AUTHENTICATION else AppState.MAIN_HUB
    }

    // Dynamic State Routing
    LaunchedEffect(currentUser) {
        if (currentUser != null && currentAppState == AppState.AUTHENTICATION) {
            currentAppState = AppState.MAIN_HUB
        } else if (currentUser == null && currentAppState == AppState.MAIN_HUB) {
            currentAppState = AppState.AUTHENTICATION
        }
    }

    val currentContext = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val localizedContext = remember(appLanguage) {
        val locale = Locale(appLanguage)
        Locale.setDefault(locale)
        val configuration = android.content.res.Configuration(currentContext.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        currentContext.createConfigurationContext(configuration)
    }

    val layoutDirection = if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    val currentRegistryOwner = LocalActivityResultRegistryOwner.current ?: run {
        var context = currentContext
        while (context is android.content.ContextWrapper) {
            if (context is androidx.activity.result.ActivityResultRegistryOwner) {
                return@run context
            }
            context = context.baseContext
        }
        context as? androidx.activity.result.ActivityResultRegistryOwner
    }

    // Top Level Scaffold with Layout Direction and Context Localization
    LocalProviders(
        localizedContext = localizedContext,
        layoutDirection = layoutDirection,
        currentRegistryOwner = currentRegistryOwner
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
        when (currentAppState) {
            AppState.SPLASH -> SplashScreen()
            AppState.AUTHENTICATION -> AuthenticationScreen(viewModel = viewModel)
            AppState.MAIN_HUB -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                val labelText = stringResource(id = tab.labelId)
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = labelText
                                        )
                                    },
                                    label = { Text(labelText, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "TabTransition"
                        ) { targetTab ->
                             when (targetTab) {
                                NavigationTab.HOME -> HomeScreen(
                                    categories = categories,
                                    courses = if (isFirestoreOnline) viewModel.firestoreCourses.collectAsStateWithLifecycle().value else courses,
                                    notes = if (isFirestoreOnline) viewModel.firestoreNotes.collectAsStateWithLifecycle().value else notes,
                                    educationalResources = if (isFirestoreOnline) viewModel.firestoreEducationalResources.collectAsStateWithLifecycle().value else educationalResources,
                                    recommendedResources = recommendedResources,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    isFirestoreOnline = isFirestoreOnline,
                                    isFirestoreLoading = isFirestoreLoading,
                                    firestoreMessage = firestoreMessage,
                                    purchasedIds = purchasedIds,
                                    wishlistIds = wishlistIds,
                                    onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                                    onCategorySelected = { viewModel.toggleCategory(it) },
                                    onCourseClick = { viewModel.recordCategoryInteraction(it.category); selectedCourseForDetail = it },
                                    onNoteClick = { viewModel.recordCategoryInteraction(it.category); selectedNoteForDetail = it },
                                    onResourceClick = { viewModel.recordCategoryInteraction(it.category); selectedResourceForDetail = it },
                                    onToggleWishlist = { itemId, itemType -> viewModel.toggleWishlist(itemId, itemType) },
                                    onClearFirestoreMessage = { viewModel.clearFirestoreMessage() },
                                    onFirestoreSyncClicked = { viewModel.syncAndFetchFirestore() },
                                    onCreateResourceClicked = { title, desc, price, cat, tags, onResult ->
                                        viewModel.createEducationalResource(title, desc, price, cat, tags, onResult)
                                    },
                                    onReportResourceClicked = { id, reason ->
                                        viewModel.reportEducationalResource(id, reason)
                                    },
                                    allPurchases = allPurchases
                                )
                                NavigationTab.NOTES -> NotesMarketplaceScreen(
                                    notes = notes,
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    purchasedIds = purchasedIds,
                                    wishlistIds = wishlistIds,
                                    onToggleWishlist = { itemId, itemType -> viewModel.toggleWishlist(itemId, itemType) },
                                    onNoteClick = { viewModel.recordCategoryInteraction(it.category); selectedNoteForDetail = it }
                                )
                                NavigationTab.EXAMS -> ExamBankScreen(
                                    quizzes = quizzes,
                                    onStartQuiz = { activeQuizForTest = it }
                                )
                                NavigationTab.TUTOR -> TutorScreen(
                                    viewModel = viewModel
                                )
                                NavigationTab.STUDIO -> StudioScreen(
                                    viewModel = viewModel,
                                    categories = categories
                                )
                                NavigationTab.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    courses = courses,
                                    notes = notes,
                                    purchasedIds = purchasedIds,
                                    onCourseClick = { viewModel.recordCategoryInteraction(it.category); selectedCourseForDetail = it },
                                    onNoteClick = { viewModel.recordCategoryInteraction(it.category); selectedNoteForDetail = it },
                                    onTabChange = { currentTab = it },
                                    onResourceClick = { viewModel.recordCategoryInteraction(it.category); selectedResourceForDetail = it }
                                )
                                NavigationTab.PLAYGROUND -> SwiftPlaygroundScreen()
                            }
                        }
                    }
                }
            }
        }

        // --- Overlays & Interaction Dialogs ---

        selectedCourseForDetail?.let { course ->
            CourseDetailDialog(
                course = course,
                isPurchased = purchasedIds.contains(course.id),
                isInstructor = course.instructorId == currentUser?.id,
                onBuyCourse = {
                    viewModel.purchaseContent(course.id, "COURSE", course.price)
                },
                onDismiss = { selectedCourseForDetail = null },
                onAddReview = { rating, comment ->
                    viewModel.addReview(course.id, "COURSE", rating, comment)
                },
                viewModel = viewModel
            )
        }

        selectedNoteForDetail?.let { note ->
            NoteDetailDialog(
                note = note,
                isPurchased = purchasedIds.contains(note.id),
                isSeller = note.sellerId == currentUser?.id,
                onBuyNote = {
                    viewModel.purchaseContent(note.id, "NOTE", note.price)
                },
                onTriggerAiSummary = {
                    viewModel.triggerPdfSummarization(note.title, note.previewText)
                    currentTab = NavigationTab.TUTOR
                    selectedNoteForDetail = null
                },
                onDismiss = { selectedNoteForDetail = null },
                onAddReview = { rating, comment ->
                    viewModel.addReview(note.id, "NOTE", rating, comment)
                },
                viewModel = viewModel
            )
        }

        selectedResourceForDetail?.let { resource ->
            EducationalResourceDetailDialog(
                resource = resource,
                onAddReview = { rating, comment ->
                    viewModel.addReview(resource.id, "RESOURCE", rating, comment)
                },
                onDismiss = { selectedResourceForDetail = null },
                viewModel = viewModel
            )
        }

        activeQuizForTest?.let { quiz ->
            QuizPlatformDialog(
                quiz = quiz,
                onDismiss = { activeQuizForTest = null }
            )
        }
    }
    }

    // --- Notification Toast System ---
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(3000)
            viewModel.clearToast()
        }
    }

    if (toastMsg != null) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.BottomCenter,
            properties = androidx.compose.ui.window.PopupProperties(
                clippingEnabled = false,
                focusable = false,
                excludeFromSystemGesture = true
            )
        ) {
            val currentToast = toastMsg
            if (currentToast != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (currentToast.type) {
                            ToastType.SUCCESS -> Color(0xFF10B981) // Emerald Green
                            ToastType.ERROR -> MaterialTheme.colorScheme.error
                            ToastType.INFO -> MaterialTheme.colorScheme.secondary
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 96.dp)
                        .testTag("notification_toast")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val icon = when (currentToast.type) {
                            ToastType.SUCCESS -> Icons.Default.CheckCircle
                            ToastType.ERROR -> Icons.Default.Error
                            ToastType.INFO -> Icons.Default.Info
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = currentToast.type.name,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = currentToast.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearToast() },
                            modifier = Modifier.size(24.dp).testTag("close_toast")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Toast",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== SCREEN COMPONENTS ====================

// --- 1. SPLASH SCREEN ---
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "EduHub",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "University Smart Marketplace",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// --- 2. AUTHENTICATION SCREEN ---
@Composable
fun AuthenticationScreen(
    viewModel: EduHubViewModel
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("STUDENT") }
    val roles = listOf("STUDENT", "INSTRUCTOR", "ADMIN")

    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authWarning by viewModel.authWarning.collectAsStateWithLifecycle()

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                viewModel.loginWithGoogleToken(idToken, selectedRole) { _, _ -> }
            } else {
                // If ID Token is null, simulate fallback
                viewModel.loginWithGoogleToken("google_id_token_simulated", selectedRole) { _, _ -> }
            }
        } catch (e: Exception) {
            Log.e("AuthenticationScreen", "Google Sign-In failed, continuing in simulation: ${e.message}")
            viewModel.loginWithGoogleToken("google_id_token_simulated_fallback", selectedRole) { _, _ -> }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .testTag("auth_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isSignUp) "Create Account" else stringResource(id = R.string.auth_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSignUp) "Join EduHub to level up your academics" else stringResource(id = R.string.auth_subtitle),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Error Banner
                if (authError != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = authError ?: "Unknown Error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = { viewModel.clearAuthError() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Info Warning Banner
                if (authWarning != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = authWarning ?: "",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = { viewModel.clearAuthWarning() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Role Selector Tab/Toggle
                Text(
                    text = stringResource(id = R.string.auth_role_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    roles.forEach { role ->
                        val isSelected = selectedRole == role
                        val localizedRole = when (role) {
                            "STUDENT" -> stringResource(id = R.string.auth_role_student)
                            "INSTRUCTOR" -> stringResource(id = R.string.auth_role_instructor)
                            else -> role
                        }
                        Button(
                            onClick = { selectedRole = role },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(localizedRole, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name (SignUp only)
                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.auth_email_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Login/Register Button
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            if (isSignUp) {
                                viewModel.registerWithEmailAndPassword(email, name, selectedRole, password) { _, _ -> }
                            } else {
                                viewModel.loginWithEmailAndPassword(email, selectedRole, password) { _, _ -> }
                            }
                        }
                    },
                    enabled = !authLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (authLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Register" else stringResource(id = R.string.auth_btn_login),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text(
                        text = "OR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Multiple Login Methods (iPhone, Google, Microsoft Mail)
                
                // 1. Google Sign In Button
                Button(
                    onClick = {
                        try {
                            val client = FirebaseAuthHelper.getGoogleSignInClient(context)
                            val intent = client.signInIntent
                            googleSignInLauncher.launch(intent)
                        } catch (e: Exception) {
                            Log.e("AuthenticationScreen", "Launch of Google Sign In failed: ${e.message}")
                            viewModel.loginWithGoogleToken("google_simulated_launch_error", selectedRole) { _, _ -> }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_login_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isSignUp) "Sign Up with Google" else "Continue with Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. iPhone / Apple Sign In Button
                Button(
                    onClick = {
                        viewModel.loginWithIPhone(selectedRole) { _, _ -> }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("iphone_login_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isSignUp) "Sign Up with iPhone" else "Continue with iPhone",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Microsoft Mail Sign In Button
                Button(
                    onClick = {
                        viewModel.loginWithMicrosoft(selectedRole) { _, _ -> }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("microsoft_login_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isSignUp) "Sign Up with Microsoft Mail" else "Continue with Microsoft Mail",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Signup/Signin Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { isSignUp = !isSignUp }
                ) {
                    Text(
                        text = if (isSignUp) "Already have an account? " else "Are you new here? ",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isSignUp) "Sign In" else "Create Account",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Demo Accounts Hint / Fast Logins
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "💡 Test Account Presets (Single-tap Auto Logins):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    email = "alex@u.edu"
                                    password = "password"
                                    selectedRole = "STUDENT"
                                    viewModel.loginWithEmailAndPassword("alex@u.edu", "STUDENT", "password") { _, _ -> }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Student Alex", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    email = "thorne@u.edu"
                                    password = "password"
                                    selectedRole = "INSTRUCTOR"
                                    viewModel.loginWithEmailAndPassword("thorne@u.edu", "INSTRUCTOR", "password") { _, _ -> }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Instructor", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. HOME SCREEN ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categories: List<CategoryEntity>,
    courses: List<CourseEntity>,
    notes: List<NoteEntity>,
    educationalResources: List<EducationalResourceEntity>,
    recommendedResources: List<EducationalResourceEntity> = emptyList(),
    selectedCategory: String?,
    searchQuery: String,
    isFirestoreOnline: Boolean,
    isFirestoreLoading: Boolean,
    firestoreMessage: String?,
    purchasedIds: Set<String>,
    wishlistIds: Set<String>,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onCourseClick: (CourseEntity) -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onResourceClick: (EducationalResourceEntity) -> Unit,
    onToggleWishlist: (String, String) -> Unit,
    onClearFirestoreMessage: () -> Unit,
    onFirestoreSyncClicked: () -> Unit,
    onCreateResourceClicked: (title: String, desc: String, price: Double, category: String, tags: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onReportResourceClicked: (String, String) -> Unit,
    allPurchases: List<PurchaseEntity> = emptyList()
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var reportingResource by remember { mutableStateOf<EducationalResourceEntity?>(null) }
    var isSubmittingResource by remember { mutableStateOf(false) }
    var resourceUploadError by remember { mutableStateOf<String?>(null) }
    var resourceSearchQuery by remember { mutableStateOf("") }
    var selectedResourceCategory by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf("Newest") }
    val sortOptions = listOf("Newest", "Highest Rated", "Price (Low to High)")

    val filteredCourses = courses.filter {
        (selectedCategory == null || it.category.equals(categories.find { cat -> cat.id == selectedCategory }?.name, ignoreCase = true)) &&
        (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
    }

    val filteredNotes = notes.filter {
        (selectedCategory == null || it.category.equals(categories.find { cat -> cat.id == selectedCategory }?.name, ignoreCase = true)) &&
        (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
    }

    val filteredResources = educationalResources.filter {
        (selectedCategory == null || it.category.equals(categories.find { cat -> cat.id == selectedCategory }?.name, ignoreCase = true)) &&
        (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)) &&
        (resourceSearchQuery.isBlank() || 
            it.title.contains(resourceSearchQuery, ignoreCase = true) || 
            it.category.contains(resourceSearchQuery, ignoreCase = true) ||
            it.tags.contains(resourceSearchQuery, ignoreCase = true)) &&
        (selectedResourceCategory == "All" || it.category.equals(selectedResourceCategory, ignoreCase = true))
    }

    val sortedResources = remember(filteredResources, selectedSortOption) {
        when (selectedSortOption) {
            "Newest" -> filteredResources.sortedByDescending { it.timestamp }
            "Highest Rated" -> filteredResources.sortedByDescending { it.averageRating }
            "Price (Low to High)" -> filteredResources.sortedBy { it.price }
            else -> filteredResources
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Welcoming App Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.app_name) + " Campus",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(id = R.string.home_search_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Elegant Dynamic Firestore Search Bar
        FirestoreSearchBar(
            query = searchQuery,
            onQueryChanged = onSearchQueryChanged,
            isFirestoreOnline = isFirestoreOnline,
            isFirestoreLoading = isFirestoreLoading,
            firestoreMessage = firestoreMessage,
            onClearMessage = onClearFirestoreMessage,
            onRefreshClick = onFirestoreSyncClicked
        )

        // Categories Chips List
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(id = R.string.home_categories),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(cat.id) },
                    label = { Text(cat.name, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        val iconVector = when (cat.iconName) {
                            "Computer" -> Icons.Default.Computer
                            "Build" -> Icons.Default.Build
                            "MonetizationOn" -> Icons.Default.MonetizationOn
                            "MedicalServices" -> Icons.Default.MedicalServices
                            else -> Icons.Default.Calculate
                        }
                        Icon(iconVector, null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // --- SECTION: LIVE FIELD COLLECTION ---
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Educational Resources",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFirestoreOnline) "Synced live from Firestore Collection" else "Local sqlite sandbox cache fallback",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Button(
                onClick = {
                    resourceUploadError = null
                    isSubmittingResource = false
                    showCreateDialog = true
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("publish_resource_button")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Publish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live Resource Search Bar
        OutlinedTextField(
            value = resourceSearchQuery,
            onValueChange = { resourceSearchQuery = it },
            placeholder = { Text("Search resources by title or category...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Resources",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = if (resourceSearchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { resourceSearchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("resource_search_bar")
        )

        val resourceCategoriesList = listOf("All", "Science", "Mathematics", "Literature", "Computer Science")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            items(resourceCategoriesList) { catName ->
                val isSelected = selectedResourceCategory == catName
                val icon = when (catName) {
                    "Science" -> Icons.Default.Science
                    "Mathematics" -> Icons.Default.Calculate
                    "Literature" -> Icons.Default.Book
                    "Computer Science" -> Icons.Default.Computer
                    else -> Icons.Default.Category
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedResourceCategory = catName },
                    label = { Text(catName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = catName,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("resource_filter_chip_$catName")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Results sorted by $selectedSortOption (${sortedResources.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Box {
                var expanded by remember { mutableStateOf(false) }

                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("resource_sort_dropdown_trigger")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = selectedSortOption,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Indicator",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.testTag("resource_sort_dropdown_menu")
                ) {
                    sortOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = option,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedSortOption == option) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedSortOption = option
                                expanded = false
                            },
                            leadingIcon = {
                                val optionIcon = when (option) {
                                    "Newest" -> Icons.Default.Book
                                    "Highest Rated" -> Icons.Default.Star
                                    "Price (Low to High)" -> Icons.Default.MonetizationOn
                                    else -> Icons.Default.Sort
                                }
                                Icon(
                                    imageVector = optionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedSortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("sort_option_${option.replace(" ", "_").lowercase()}")
                        )
                    }
                }
            }
        }

        if (isFirestoreLoading) {
            EducationalResourcesSkeletonRow(brush = shimmerBrush())
        } else if (sortedResources.isEmpty()) {
            NoItemsPlaceholder("No live educational resource listings match your criteria.")
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sortedResources) { resource ->
                    EducationalResourceRowItem(
                        resource = resource,
                        onClick = { onResourceClick(resource) },
                        onReportClick = { reportingResource = resource },
                        onTagClick = { tag -> resourceSearchQuery = tag },
                        allResources = educationalResources,
                        allPurchases = allPurchases
                    )
                }
            }
        }

        // --- SECTION: RECOMMENDED FOR YOU ---
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recommended for You",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Interests Match",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Curated resources based on your wishlist and search interactions",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (recommendedResources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Discover more courses to unlock personalized suggestions!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.testTag("recommended_resources_list")
            ) {
                items(recommendedResources) { resource ->
                    EducationalResourceRowItem(
                        resource = resource,
                        onClick = { onResourceClick(resource) },
                        onReportClick = { reportingResource = resource },
                        onTagClick = { tag -> resourceSearchQuery = tag },
                        allResources = educationalResources,
                        allPurchases = allPurchases
                    )
                }
            }
        }

        // --- SECTION: FEATURED COURSES ---
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(id = R.string.home_courses_section),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (stringResource(id = R.string.tab_home) == "الرئيسية") "عرض الكل" else "View All",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { }
            )
        }

        if (filteredCourses.isEmpty()) {
            NoItemsPlaceholder(stringResource(id = R.string.home_no_courses))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredCourses) { course ->
                    CourseRowItem(
                        course = course,
                        isPurchased = purchasedIds.contains(course.id),
                        isWishlisted = wishlistIds.contains(course.id),
                        onWishlistToggle = { onToggleWishlist(course.id, "COURSE") },
                        onClick = { onCourseClick(course) }
                    )
                }
            }
        }

        // --- SECTION: CLASSROOM SUMMARY NOTES ---
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(id = R.string.notes_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (stringResource(id = R.string.tab_home) == "الرئيسية") "عرض الكل" else "View All",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { }
            )
        }

        if (filteredNotes.isEmpty()) {
            NoItemsPlaceholder(stringResource(id = R.string.notes_no_notes))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredNotes) { note ->
                    NoteRowItem(
                        note = note,
                        isPurchased = purchasedIds.contains(note.id),
                        isWishlisted = wishlistIds.contains(note.id),
                        onWishlistToggle = { onToggleWishlist(note.id, "NOTE") },
                        onClick = { onNoteClick(note) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }

    // Dynamic Creation Dialogue
    if (showCreateDialog) {
        Dialog(
            onDismissRequest = { showCreateDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                EducationalResourceForm(
                    categories = categories,
                    onSubmit = { title, desc, price, cat, tags ->
                        isSubmittingResource = true
                        resourceUploadError = null
                        onCreateResourceClicked(title, desc, price, cat, tags) { success, msg ->
                            isSubmittingResource = false
                            if (success) {
                                showCreateDialog = false
                            } else {
                                resourceUploadError = msg
                            }
                        }
                    },
                    onCancel = { showCreateDialog = false },
                    isSubmitting = isSubmittingResource,
                    errorMessage = resourceUploadError
                )
            }
        }
    }

    if (reportingResource != null) {
        ReportResourceDialog(
            resource = reportingResource!!,
            onDismiss = { reportingResource = null },
            onSubmit = { reason ->
                onReportResourceClicked(reportingResource!!.id, reason)
                reportingResource = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportResourceDialog(
    resource: EducationalResourceEntity,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reasonText by remember { mutableStateOf("") }
    val categories = listOf(
        "Spam or misleading",
        "Inappropriate content or language",
        "Harassment or hate speech",
        "Copyright violation",
        "Incorrect or low-quality guidance",
        "Other"
    )
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var otherReasonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Report Resource",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Flag \"${resource.title}\" for review. Please select a reason for reporting:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.testTag("report_radio_$cat")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = cat, fontSize = 13.sp)
                    }
                }

                if (selectedCategory == "Other") {
                    OutlinedTextField(
                        value = otherReasonText,
                        onValueChange = { otherReasonText = it },
                        label = { Text("Describe the issue...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("report_other_reason_input"),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedCategory == "Other") otherReasonText.ifBlank { "Other reason" } else selectedCategory
                    onSubmit(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("submit_report_confirm_btn")
            ) {
                Text("Submit Report", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_report_dialog_btn")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("report_resource_dialog")
    )
}

@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        val transition = rememberInfiniteTransition(label = "shimmer_transition")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_animation"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun EducationalResourceSkeletonItem(brush: Brush) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(brush)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun EducationalResourcesSkeletonRow(brush: Brush) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.testTag("resources_skeleton_loading")
    ) {
        items(3) {
            EducationalResourceSkeletonItem(brush = brush)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EducationalResourceRowItem(
    resource: EducationalResourceEntity,
    onClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onTagClick: (String) -> Unit = {},
    allResources: List<EducationalResourceEntity> = emptyList(),
    allPurchases: List<PurchaseEntity> = emptyList()
) {
    val creatorResources = remember(resource.authorId, allResources) {
        allResources.filter { it.authorId == resource.authorId }
    }
    val creatorUploadedCount = creatorResources.size

    val creatorSales = remember(resource.authorId, creatorResources, allPurchases) {
        if (creatorResources.isEmpty()) 0
        else {
            val resourceIds = creatorResources.map { it.id }.toSet()
            allPurchases.count { it.contentId in resourceIds }
        }
    }

    val creatorAvgRating = remember(creatorResources) {
        val ratedResources = creatorResources.filter { it.reviewsCount > 0 }
        if (ratedResources.isEmpty()) {
            resource.averageRating
        } else {
            val average = ratedResources.map { it.averageRating }.average()
            if (average.isNaN()) resource.averageRating else average
        }
    }
    val totalReviewsCount = remember(creatorResources) {
        creatorResources.sumOf { it.reviewsCount }
    }

    val isTopContributor = creatorSales >= 2 || creatorUploadedCount >= 3
    val isExpert = (creatorAvgRating >= 4.5 && (totalReviewsCount >= 1 || resource.reviewsCount >= 1))

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .height(210.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDark = LocalThemeHelper.current.isDark
                    val categoryName = resource.category.trim()
                    
                    val (badgeBg, badgeText) = remember(categoryName, isDark) {
                        when {
                            categoryName.contains("Science", ignoreCase = true) -> {
                                if (isDark) Pair(Color(0xFF1E3A8A), Color(0xFF93C5FD))
                                else Pair(Color(0xFFEFF6FF), Color(0xFF1D4ED8))
                            }
                            categoryName.contains("Math", ignoreCase = true) || categoryName.contains("Calculus", ignoreCase = true) || categoryName.contains("Arithmetic", ignoreCase = true) -> {
                                if (isDark) Pair(Color(0xFF064E3B), Color(0xFF6EE7B7))
                                else Pair(Color(0xFFECFDF5), Color(0xFF047857))
                            }
                            categoryName.contains("Literature", ignoreCase = true) || categoryName.contains("English", ignoreCase = true) || categoryName.contains("History", ignoreCase = true) -> {
                                if (isDark) Pair(Color(0xFF581C87), Color(0xFFD8B4FE))
                                else Pair(Color(0xFFFAF5FF), Color(0xFF6D28D9))
                            }
                            categoryName.contains("Computer", ignoreCase = true) || categoryName.contains("Tech", ignoreCase = true) || categoryName.contains("Coding", ignoreCase = true) -> {
                                if (isDark) Pair(Color(0xFF164E63), Color(0xFF67E8F9))
                                else Pair(Color(0xFFECFEFF), Color(0xFF0E7490))
                            }
                            categoryName.contains("Design", ignoreCase = true) || categoryName.contains("Art", ignoreCase = true) -> {
                                if (isDark) Pair(Color(0xFF78350F), Color(0xFFFCD34D))
                                else Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
                            }
                            else -> {
                                if (isDark) Pair(Color(0xFF334155), Color(0xFFCBD5E1))
                                else Pair(Color(0xFFF1F5F9), Color(0xFF475569))
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .height(20.dp)
                            .testTag("resource_category_badge_${resource.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resource.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$${String.format("%.2f", resource.price)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val contextForCard = LocalContext.current
                        IconButton(
                            onClick = {
                                val clipboard = contextForCard.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Resource Link", "https://eduhub.app/resource/${resource.id}")
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(contextForCard, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("copy_resource_link_btn_${resource.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Copy Link",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = onReportClick,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("report_resource_btn_${resource.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report Resource",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resource.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${String.format("%.1f", resource.averageRating)} (${resource.reviewsCount})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resource.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                if (resource.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        resource.tags.split(",")
                            .map { it.trim().substringBefore(" ").lowercase() }
                            .filter { it.isNotEmpty() }
                            .take(3)
                            .forEach { tag ->
                                Text(
                                    text = "#$tag",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                        .clickable { onTagClick(tag) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .testTag("tag_chip_${resource.id}_$tag")
                                )
                            }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = resource.authorName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isExpert) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE0E7FF),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("expert_badge_${resource.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Expert",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Expert",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                    if (isTopContributor) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("top_contributor_badge_${resource.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Top Contributor",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Top",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 4. NOTES MARKETPLACE SCREEN ---
@Composable
fun NotesMarketplaceScreen(
    notes: List<NoteEntity>,
    categories: List<CategoryEntity>,
    selectedCategory: String?,
    searchQuery: String,
    purchasedIds: Set<String>,
    wishlistIds: Set<String>,
    onToggleWishlist: (String, String) -> Unit,
    onNoteClick: (NoteEntity) -> Unit
) {
    val filteredNotes = notes.filter {
        (selectedCategory == null || it.category.equals(categories.find { cat -> cat.id == selectedCategory }?.name, ignoreCase = true)) &&
        (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            stringResource(id = R.string.notes_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(id = R.string.notes_subtitle),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredNotes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NoItemsPlaceholder(stringResource(id = R.string.notes_no_notes_long))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotes) { note ->
                    NoteCardItem(
                        note = note,
                        isPurchased = purchasedIds.contains(note.id),
                        isWishlisted = wishlistIds.contains(note.id),
                        onWishlistToggle = { onToggleWishlist(note.id, "NOTE") },
                        onClick = { onNoteClick(note) }
                    )
                }
            }
        }
    }
}

// --- 5. EXAM BANK SCREEN ---
@Composable
fun ExamBankScreen(
    quizzes: List<QuizEntity>,
    onStartQuiz: (QuizEntity) -> Unit
) {
    var viewExamMode by remember { mutableStateOf(true) } // true: Past Exams, false: Practice Quizzes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Text(
                stringResource(id = R.string.exams_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(id = R.string.exams_subtitle),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Mode Switch Tabs
        TabRow(selectedTabIndex = if (viewExamMode) 0 else 1) {
            Tab(
                selected = viewExamMode,
                onClick = { viewExamMode = true },
                text = { Text(stringResource(id = R.string.exams_tab_past)) }
            )
            Tab(
                selected = !viewExamMode,
                onClick = { viewExamMode = false },
                text = { Text(stringResource(id = R.string.exams_tab_practice)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentQuizzes = quizzes.filter { it.isExamBank == viewExamMode }

        if (currentQuizzes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(id = R.string.exams_no_material),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentQuizzes) { quiz ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartQuiz(quiz) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    quiz.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(quiz.subject, fontSize = 10.sp) },
                                        modifier = Modifier.height(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Click to Start Test",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 6. AI STUDY TUTOR SCREEN ---
@Composable
fun TutorScreen(
    viewModel: EduHubViewModel
) {
    val chatMessages by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isResponding by viewModel.isTutorResponding.collectAsStateWithLifecycle()
    var userMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showQuizGenDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(id = R.string.tab_tutor),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    stringResource(id = R.string.tutor_subtitle),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { showQuizGenDialog = true }) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        "Generate Quiz",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(
                        Icons.Default.List,
                        "Clear History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .padding(8.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(id = R.string.tutor_placeholder),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)
                ) {
                    items(chatMessages) { chat ->
                        ChatBubbleItem(chat)
                    }
                    if (isResponding) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (stringResource(id = R.string.tab_home) == "الرئيسية") "مساعد EduHub الذكي يكتب الإجابة..." else "EduHub AI is drafting answer...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SuggestionChip(
                    onClick = { viewModel.askTutor("Explain NPV and IRR formula in finance") },
                    label = { Text("Finance NPV / IRR", fontSize = 11.sp) }
                )
            }
            item {
                SuggestionChip(
                    onClick = { viewModel.askTutor("Explain binary trees traverse (pre-order, in-order, post-order)") },
                    label = { Text("BST Traversals", fontSize = 11.sp) }
                )
            }
            item {
                SuggestionChip(
                    onClick = { viewModel.askTutor("Give me a trick to remember the LIATE rule in Calculus II") },
                    label = { Text("Calculus Tips", fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text(stringResource(id = R.string.tutor_placeholder)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        viewModel.askTutor(userMessage)
                        userMessage = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }

    if (showQuizGenDialog) {
        var subjectInput by remember { mutableStateOf("Computer Science") }
        var topicInput by remember { mutableStateOf("Operating Systems") }
        val isGenLoading by viewModel.isGeneratingQuiz.collectAsStateWithLifecycle()
        val errorText by viewModel.quizGenerationError.collectAsStateWithLifecycle()

        Dialog(
            onDismissRequest = { if (!isGenLoading) showQuizGenDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .padding(2.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "AI Exam Compiler",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Using deep-reasoning AI, we will create a personalized exam based on your course inputs.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("Subject (e.g. Finance, Biology)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("Topic (e.g. Mitosis, Derivatives)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    errorText?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isGenLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "AI drafting questions... (Takes up to 8s)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showQuizGenDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    viewModel.generateInteractiveQuiz(subjectInput, topicInput) { success ->
                                        if (success) {
                                            showQuizGenDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Compile Exam")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(message: AIChatMessageEntity) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "You" else "EduHub AI Tutor",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// --- 7. PUBLISHING / UPLOAD STUDIO SCREEN (SELLER) ---
@Composable
fun StudioScreen(
    viewModel: EduHubViewModel,
    categories: List<CategoryEntity>
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allResources by viewModel.allEducationalResources.collectAsStateWithLifecycle(emptyList())
    val reportedResources = remember(allResources) { allResources.filter { it.isReported } }

    var isAddingCourse by remember { mutableStateOf(true) } // true: Video Course, false: PDF note summary
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Computer Science") }
    var notePreviewContent by remember { mutableStateOf("") }

    // Courses Lesson List entries
    var lesson1 by remember { mutableStateOf("Intro and syllabus") }
    var lesson2 by remember { mutableStateOf("Conceptual deepdive") }
    var lesson3 by remember { mutableStateOf("Final Exam walkthrough") }

    val showCreatorUi = currentUser?.role == "INSTRUCTOR" || currentUser?.role == "ADMIN"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(id = R.string.tab_studio),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(id = R.string.studio_subtitle),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (!showCreatorUi) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        stringResource(id = R.string.studio_upgrade_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(id = R.string.studio_upgrade_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.updateRole("INSTRUCTOR") }
                    ) {
                        Text(stringResource(id = R.string.studio_upgrade_btn))
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(id = R.string.studio_balance), fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(
                            "${String.format("%.2f", currentUser?.earnings ?: 0.0)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer, contentColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(stringResource(id = R.string.studio_cashout), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (currentUser?.role == "ADMIN") {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("admin_moderation_card"),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Moderation Queue",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(
                                    text = "${reportedResources.size} Flagged",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Students have flagged these resources for violating guidelines. Admin intervention required.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (reportedResources.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Queue clear! Student learning environment is safe.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                reportedResources.forEach { resource ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("reported_item_card_${resource.id}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = resource.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Category: ${resource.category} • Author: ${resource.authorName}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ReportProblem,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Violation: ${resource.reportReason}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { viewModel.dismissReport(resource.id) },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.testTag("dismiss_report_btn_${resource.id}").height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Dismiss Flag", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Button(
                                                    onClick = { viewModel.deleteReportingResource(resource.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                    modifier = Modifier.testTag("delete_reported_btn_${resource.id}").height(32.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Remove Listing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = if (isAddingCourse) 0 else 1) {
                Tab(
                    selected = isAddingCourse,
                    onClick = { isAddingCourse = true },
                    text = { Text(stringResource(id = R.string.studio_tab_course)) }
                )
                Tab(
                    selected = !isAddingCourse,
                    onClick = { isAddingCourse = false },
                    text = { Text(stringResource(id = R.string.studio_tab_pdf)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(id = R.string.studio_item_title)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(id = R.string.studio_item_desc)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text(stringResource(id = R.string.studio_item_price)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = categorySelection,
                    onValueChange = { categorySelection = it },
                    label = { Text(stringResource(id = R.string.studio_item_cat)) },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isAddingCourse) {
                Text("Lesson Syllabus Walkthrough", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = lesson1,
                    onValueChange = { lesson1 = it },
                    label = { Text("Video Lecture 1 Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lesson2,
                    onValueChange = { lesson2 = it },
                    label = { Text("Video Lecture 2 Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lesson3,
                    onValueChange = { lesson3 = it },
                    label = { Text("Video Lecture 3 Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Text("Complete Note Text (Free abstract preview)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notePreviewContent,
                    onValueChange = { notePreviewContent = it },
                    placeholder = { Text("Input integration equations or summaries. Purchased users will download the full list.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            var showNotificationPublishSuccess by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedPrice = priceInput.toDoubleOrNull() ?: 4.99
                        if (isAddingCourse) {
                            viewModel.uploadCourse(title, description, parsedPrice, categorySelection, listOf(lesson1, lesson2, lesson3))
                        } else {
                            viewModel.uploadNote(title, description, parsedPrice, categorySelection, notePreviewContent)
                        }
                        showNotificationPublishSuccess = true
                        title = ""
                        description = ""
                        priceInput = ""
                        notePreviewContent = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("publish_button")
            ) {
                Text("Publish to Marketplace", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (showNotificationPublishSuccess) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "🎉 Content successfully listed in Home Marketplace catalog!",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- 8. ACADEMY DASHBOARD SCREEN ---
@Composable
fun DashboardScreen(
    viewModel: EduHubViewModel,
    courses: List<CourseEntity>,
    notes: List<NoteEntity>,
    purchasedIds: Set<String>,
    onCourseClick: (CourseEntity) -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onTabChange: (NavigationTab) -> Unit,
    onResourceClick: (EducationalResourceEntity) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val resourceProgress by viewModel.resourceProgress.collectAsStateWithLifecycle(emptyList())

    val rawFirestoreResources by viewModel.firestoreEducationalResources.collectAsStateWithLifecycle()
    val isFirestoreLoading by viewModel.isFirestoreLoading.collectAsStateWithLifecycle()

    val myUploadedResources = remember(rawFirestoreResources, currentUser) {
        rawFirestoreResources.filter { it.authorId == currentUser?.id }
    }

    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val wishlistedCourses = remember(wishlistItems, courses) {
        val courseIds = wishlistItems.filter { it.itemType == "COURSE" }.map { it.itemId }.toSet()
        courses.filter { it.id in courseIds }
    }
    val wishlistedNotes = remember(wishlistItems, notes) {
        val noteIds = wishlistItems.filter { it.itemType == "NOTE" }.map { it.itemId }.toSet()
        notes.filter { it.id in noteIds }
    }

    val allEducationalResources by viewModel.allEducationalResources.collectAsStateWithLifecycle()
    val allPurchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val allWishlistItems by viewModel.allWishlistItems.collectAsStateWithLifecycle()

    val purchasedCourses = remember(purchasedIds, courses) {
        courses.filter { it.id in purchasedIds }
    }
    val purchasedNotes = remember(purchasedIds, notes) {
        notes.filter { it.id in purchasedIds }
    }
    val purchasedResources = remember(purchasedIds, allEducationalResources) {
        allEducationalResources.filter { it.id in purchasedIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        currentUser?.let { user ->
            UserProfile(
                user = user,
                uploadedResources = myUploadedResources,
                isFirestoreLoading = isFirestoreLoading,
                onResourceClick = onResourceClick,
                onRefreshFirestore = { viewModel.syncAndFetchFirestore() },
                onGoToStudioTab = { onTabChange(NavigationTab.STUDIO) },
                wishlistedCourses = wishlistedCourses,
                wishlistedNotes = wishlistedNotes,
                onCourseClick = onCourseClick,
                onNoteClick = onNoteClick,
                purchasedCourses = purchasedCourses,
                purchasedNotes = purchasedNotes,
                purchasedResources = purchasedResources,
                allPurchases = allPurchases,
                allWishlistItems = allWishlistItems,
                onDeleteBulkResources = { ids, onComplete ->
                    viewModel.deleteBulkResources(ids, onComplete)
                },
                resourceProgress = resourceProgress
            )
        } ?: Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            stringResource(id = R.string.dashboard_member),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "member@u.edu",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        stringResource(id = R.string.dashboard_premium_member),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        stringResource(id = R.string.dashboard_premium_desc),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = currentUser?.premiumUser == true,
                    onCheckedChange = { viewModel.togglePremiumSubscription() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 🌐 MODERN LANGUAGE SWITCHER CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.dashboard_language_section),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isEnglish = appLanguage == "en"
                    val isArabic = appLanguage == "ar"

                    // English Select Button
                    Button(
                        onClick = { viewModel.setAppLanguage("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).height(42.dp),
                        border = if (!isEnglish) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Arabic Select Button
                    Button(
                        onClick = { viewModel.setAppLanguage("ar") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isArabic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isArabic) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).height(42.dp),
                        border = if (!isArabic) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("العربية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 🌙 GLOBAL DARK MODE TOGGLE CARD ---
        val themeHelper = LocalThemeHelper.current
        Card(
            modifier = Modifier.fillMaxWidth().testTag("dark_mode_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Theme Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (themeHelper.isDark) "Dark Theme Enabled" else "Light Theme Enabled",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toggle visual styling preferences",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = themeHelper.isDark,
                    onCheckedChange = { themeHelper.toggleTheme() },
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            stringResource(id = R.string.dashboard_library),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        val boughtCourses = courses.filter { purchasedIds.contains(it.id) }
        val boughtNotes = notes.filter { purchasedIds.contains(it.id) }

        if (boughtCourses.isEmpty() && boughtNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(id = R.string.dashboard_no_purchases),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            boughtCourses.forEach { crs ->
                ListItem(
                    headlineContent = { Text(crs.title, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Course by ${crs.instructorName}", fontSize = 11.sp) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onCourseClick(crs) }
                )
                HorizontalDivider()
            }
            boughtNotes.forEach { nt ->
                ListItem(
                    headlineContent = { Text(nt.title, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Summary Notes • Seller ${nt.sellerName}", fontSize = 11.sp) },
                    leadingContent = { Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.secondary) },
                    modifier = Modifier.clickable { onNoteClick(nt) }
                )
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.dashboard_logout))
            }

            if (currentUser?.role == "STUDENT") {
                Button(
                    onClick = { viewModel.updateRole("INSTRUCTOR") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.dashboard_become_creator))
                }
            } else {
                Button(
                    onClick = { viewModel.updateRole("STUDENT") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.dashboard_switch_student))
                }
            }
        }
    }
}

// ==================== REUSABLE WIDGETS & SUBCOMPONENT DIALOGS ====================

@Composable
fun NoItemsPlaceholder(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            msg,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CourseRowItem(
    course: CourseEntity,
    isPurchased: Boolean,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(course.category, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Floating Saved Toggle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(44.dp)
                ) {
                    IconButton(
                        onClick = { onWishlistToggle() },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("wishlist_btn_${course.id}")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (isWishlisted) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    course.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Instructor: ${course.instructorName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(course.rating.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (isPurchased) "Enrolled" else "$${course.price}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun NoteRowItem(
    note: NoteEntity,
    isPurchased: Boolean,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )

                // Floating Saved Toggle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(44.dp)
                ) {
                    IconButton(
                        onClick = { onWishlistToggle() },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("wishlist_btn_${note.id}")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (isWishlisted) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    note.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(note.rating.toString(), fontSize = 10.sp)
                    }
                    Text(
                        if (isPurchased) "Unlocked" else "$${note.price}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    isPurchased: Boolean,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("By ${note.sellerName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(note.rating.toString(), fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onWishlistToggle() },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("wishlist_btn_${note.id}")
            ) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isWishlisted) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = { onClick() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPurchased) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isPurchased) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                )
            ) {
                Text(if (isPurchased) "View PDF" else "$${note.price}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CourseDetailDialog(
    course: CourseEntity,
    isPurchased: Boolean,
    isInstructor: Boolean,
    onBuyCourse: () -> Unit,
    onAddReview: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: EduHubViewModel
) {
    var isPlayingLecture by remember { mutableStateOf<Map<String, String>?>(null) }
    var reviewRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    val reviews by viewModel.getReviews(course.id, "COURSE").collectAsStateWithLifecycle(emptyList())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(course.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Cancel, null)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(course.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Curated by ${course.instructorName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(course.description, fontSize = 12.sp, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isPurchased && !isInstructor) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Full Syllabus Video bundle", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("$${course.price}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Button(onClick = onBuyCourse) {
                                    Text("Unlock & Enroll")
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                if (isInstructor) "🎉 You are the creator of this syllabus." else "✅ You have enrolled. Unlimited playback active.",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Interactive Video Lectures", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val lessonsList = remember(course.lessonsJson) {
                        try {
                            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                            val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                            val adapter = moshi.adapter<List<Map<String, String>>>(listType)
                            adapter.fromJson(course.lessonsJson) ?: emptyList()
                        } catch (e: Exception) {
                            listOf(
                                mapOf("title" to "1. Conceptual introduction", "duration" to "14:10"),
                                mapOf("title" to "2. Practical exam applications", "duration" to "19:40")
                            )
                        }
                    }

                    lessonsList.forEach { lesson ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isPurchased || isInstructor) {
                                        isPlayingLecture = lesson
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPurchased || isInstructor) Icons.Default.PlayArrow else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isPurchased || isInstructor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(lesson["title"] ?: "Lecture", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(lesson["duration"] ?: "12:00", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    isPlayingLecture?.let { lecture ->
                        AlertDialog(
                            onDismissRequest = { isPlayingLecture = null },
                            title = { Text(lecture["title"] ?: "Lecture Video Stream", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .background(Color.Black, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Streaming dynamic HD tutorial...", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Lecture length: ${lecture["duration"]}. Subtitles in English & Arabic available.",
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { isPlayingLecture = null }) { Text("Close Stream") }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Student Feedback & Reviews", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isPurchased && !isInstructor) {
                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            placeholder = { Text("Share honest feedback with classmates...") },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Write review comments") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rating: ")
                            (1..5).forEach { star ->
                                IconButton(onClick = { reviewRating = star }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = if (reviewRating >= star) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    if (reviewComment.isNotBlank()) {
                                        onAddReview(reviewRating, reviewComment)
                                        reviewComment = ""
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Submit Review", fontSize = 11.sp)
                            }
                        }
                    }

                    reviews.forEach { rev ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(rev.userName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row {
                                        (1..rev.rating).forEach {
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(rev.comment, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EducationalResourceDetailDialog(
    resource: EducationalResourceEntity,
    onAddReview: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: EduHubViewModel
) {
    var reviewRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    val reviews by viewModel.getReviews(resource.id, "RESOURCE").collectAsStateWithLifecycle(emptyList())
    val comments by viewModel.activeResourceComments.collectAsStateWithLifecycle()
    val isCommentsLoading by viewModel.isCommentsLoading.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val purchasedIds by viewModel.purchasedIds.collectAsStateWithLifecycle()

    val allResources by viewModel.allEducationalResources.collectAsStateWithLifecycle()
    val allPurchases by viewModel.allPurchases.collectAsStateWithLifecycle()

    val creatorResources = remember(resource.authorId, allResources) {
        allResources.filter { it.authorId == resource.authorId }
    }
    val creatorUploadedCount = creatorResources.size

    val creatorSales = remember(resource.authorId, creatorResources, allPurchases) {
        if (creatorResources.isEmpty()) 0
        else {
            val resourceIds = creatorResources.map { it.id }.toSet()
            allPurchases.count { it.contentId in resourceIds }
        }
    }

    val creatorAvgRating = remember(creatorResources) {
        val ratedResources = creatorResources.filter { it.reviewsCount > 0 }
        if (ratedResources.isEmpty()) {
            resource.averageRating
        } else {
            val average = ratedResources.map { it.averageRating }.average()
            if (average.isNaN()) resource.averageRating else average
        }
    }
    val totalReviewsCount = remember(creatorResources) {
        creatorResources.sumOf { it.reviewsCount }
    }

    val isTopContributor = creatorSales >= 2 || creatorUploadedCount >= 3
    val isExpert = (creatorAvgRating >= 4.5 && (totalReviewsCount >= 1 || resource.reviewsCount >= 1))

    LaunchedEffect(resource.id) {
        viewModel.fetchComments(resource.id)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("resource_detail_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when {
                            resource.category.contains("Science", ignoreCase = true) -> Icons.Default.Science
                            resource.category.contains("Math", ignoreCase = true) || resource.category.contains("Calculus", ignoreCase = true) || resource.category.contains("Arithmetic", ignoreCase = true) -> Icons.Default.Calculate
                            resource.category.contains("Literature", ignoreCase = true) || resource.category.contains("English", ignoreCase = true) || resource.category.contains("History", ignoreCase = true) -> Icons.Default.Book
                            resource.category.contains("Computer", ignoreCase = true) || resource.category.contains("Tech", ignoreCase = true) -> Icons.Default.Computer
                            else -> Icons.Default.Category
                        }
                        
                        val isDark = LocalThemeHelper.current.isDark
                        val categoryName = resource.category.trim()
                        
                        val (badgeBg, badgeText) = remember(categoryName, isDark) {
                            when {
                                categoryName.contains("Science", ignoreCase = true) -> {
                                    if (isDark) Pair(Color(0xFF1E3A8A), Color(0xFF93C5FD))
                                    else Pair(Color(0xFFEFF6FF), Color(0xFF1D4ED8))
                                }
                                categoryName.contains("Math", ignoreCase = true) || categoryName.contains("Calculus", ignoreCase = true) || categoryName.contains("Arithmetic", ignoreCase = true) -> {
                                    if (isDark) Pair(Color(0xFF064E3B), Color(0xFF6EE7B7))
                                    else Pair(Color(0xFFECFDF5), Color(0xFF047857))
                                }
                                categoryName.contains("Literature", ignoreCase = true) || categoryName.contains("English", ignoreCase = true) || categoryName.contains("History", ignoreCase = true) -> {
                                    if (isDark) Pair(Color(0xFF581C87), Color(0xFFD8B4FE))
                                    else Pair(Color(0xFFFAF5FF), Color(0xFF6D28D9))
                                }
                                categoryName.contains("Computer", ignoreCase = true) || categoryName.contains("Tech", ignoreCase = true) || categoryName.contains("Coding", ignoreCase = true) -> {
                                    if (isDark) Pair(Color(0xFF164E63), Color(0xFF67E8F9))
                                    else Pair(Color(0xFFECFEFF), Color(0xFF0E7490))
                                }
                                categoryName.contains("Design", ignoreCase = true) || categoryName.contains("Art", ignoreCase = true) -> {
                                    if (isDark) Pair(Color(0xFF78350F), Color(0xFFFCD34D))
                                    else Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
                                }
                                else -> {
                                    if (isDark) Pair(Color(0xFF334155), Color(0xFFCBD5E1))
                                    else Pair(Color(0xFFF1F5F9), Color(0xFF475569))
                                }
                            }
                        }

                        Icon(icon, null, tint = badgeText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = resource.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val contextForDialog = LocalContext.current
                        IconButton(
                            onClick = {
                                val clipboard = contextForDialog.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Resource Link", "https://eduhub.app/resource/${resource.id}")
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(contextForDialog, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("dialog_copy_resource_link_btn_${resource.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Copy Link",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_resource_dialog")) {
                            Icon(Icons.Default.Cancel, null)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(resource.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Published by ${resource.authorName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isExpert) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0E7FF),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("dialog_expert_badge_${resource.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Expert Creator",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Expert Creator",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }

                        if (isTopContributor) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("dialog_top_contributor_badge_${resource.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Top Contributor",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Top Contributor",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }

                    if (isExpert || isTopContributor) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Prestige",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = buildString {
                                        append("${resource.authorName} is a verified elite campus contributor. ")
                                        append("They have published $creatorUploadedCount resource(s) ")
                                        if (creatorSales > 0) append("with $creatorSales total sale(s) ")
                                        if (totalReviewsCount > 0) append("and an average rating of ${String.format("%.1f", creatorAvgRating)}★!")
                                    },
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(resource.description, fontSize = 12.sp, lineHeight = 18.sp)

                    if (resource.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Keywords & Tags",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            resource.tags.split(",")
                                .map { it.trim().substringBefore(" ").lowercase() }
                                .filter { it.isNotEmpty() }
                                .forEach { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("#$tag", fontSize = 10.sp) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                        }
                    }

                    val isPurchased = purchasedIds.contains(resource.id)
                    val isAuthor = resource.authorId == currentUser?.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("resource_purchase_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPurchased || isAuthor) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPurchased) "Acquired Resource" else if (isAuthor) "Your Uploaded Resource" else "Resource Access",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPurchased) "You have full access to study this file." else if (isAuthor) "You uploaded this research study material." else "Acquire this resource to unlock resource study contents.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            if (isPurchased || isAuthor) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unlocked",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                val buttonText = if (resource.price == 0.0) "Acquire Free" else "Buy for $${String.format("%.2f", resource.price)}"
                                Button(
                                    onClick = {
                                        viewModel.purchaseContent(resource.id, "RESOURCE", resource.price)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("buy_resource_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isPurchased || isAuthor) {
                        val progressForRes by viewModel.resourceProgress.collectAsStateWithLifecycle(emptyList())
                        val progressRecord = progressForRes.find { it.resourceId == resource.id }
                        val completedCsv = progressRecord?.completedPagesCsv ?: ""
                        val completedSet = remember(completedCsv) {
                            if (completedCsv.isBlank()) emptySet() else completedCsv.split(",").toSet()
                        }
                        val sections = remember(resource.id, resource.title) {
                            getResourceSections(resource.id, resource.title)
                        }
                        val totalSections = sections.size

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("resource_progress_tracker_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = "Progress Tracker",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Interactive Study Progression",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val percent = if (totalSections > 0) completedSet.size.toFloat() / totalSections else 0f
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${completedSet.size} of $totalSections sections completed",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(percent * 100).toInt()}% Done",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { percent },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .testTag("dialog_progress_bar")
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(8.dp))

                                sections.forEachIndexed { index, sectionName ->
                                    val isCompleted = completedSet.contains(index.toString())
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val newSet = if (isCompleted) {
                                                    completedSet - index.toString()
                                                } else {
                                                    completedSet + index.toString()
                                                }
                                                val newCsv = newSet.joinToString(",")
                                                viewModel.updateResourceProgress(resource.id, newCsv)
                                            }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isCompleted,
                                            onCheckedChange = { checked ->
                                                val newSet = if (checked == false) {
                                                    completedSet - index.toString()
                                                } else {
                                                    completedSet + index.toString()
                                                }
                                                val newCsv = newSet.joinToString(",")
                                                viewModel.updateResourceProgress(resource.id, newCsv)
                                            },
                                            modifier = Modifier.testTag("section_checkbox_${index}")
                                        )
                                        Text(
                                            text = sectionName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isCompleted) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Rating", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format("%.1f", resource.averageRating),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row {
                                    (1..5).forEach { star ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (resource.averageRating >= star) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Text("Based on ${resource.reviewsCount} reviews", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Add 1-5 Star Review", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        placeholder = { Text("What did you think of this resource? Share comments with other students...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("resource_review_comment_input"),
                        label = { Text("Write review comments", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Stars: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        (1..5).forEach { star ->
                            IconButton(onClick = { reviewRating = star }, modifier = Modifier.size(36.dp).testTag("select_star_$star")) {
                                Icon(
                                    Icons.Default.Star,
                                    null,
                                    tint = if (reviewRating >= star) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (reviewComment.isNotBlank()) {
                                    onAddReview(reviewRating, reviewComment)
                                    reviewComment = ""
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("submit_resource_review_button")
                        ) {
                            Text("Submit", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Classmates Reviews", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (reviews.isEmpty()) {
                        Text("No reviews yet. Be the first to leave a review!", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        reviews.forEach { rev ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(rev.userName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row {
                                            (1..rev.rating).forEach {
                                                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rev.comment, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // --- Dynamic Discussion Section (Sub-collection) ---
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "Class Discussion",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Student Discussion",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = "${comments.size}",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    var commentText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Ask a question or share thoughts with classmates...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resource_comment_input"),
                        label = { Text("Join the discussion", fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.addComment(resource.id, commentText)
                                        commentText = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_comment_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Post Comment",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isCommentsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else if (comments.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QuestionAnswer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No comments yet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Be the first student to ask a question or discuss this material!",
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("comments_list")
                        ) {
                            comments.forEach { comment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = comment.userName.take(1).uppercase(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = comment.userName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            
                                            val timeString = remember(comment.timestamp) {
                                                val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(comment.timestamp))
                                            }
                                            Text(
                                                text = timeString,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = comment.commentText,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteDetailDialog(
    note: NoteEntity,
    isPurchased: Boolean,
    isSeller: Boolean,
    onBuyNote: () -> Unit,
    onTriggerAiSummary: () -> Unit,
    onAddReview: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: EduHubViewModel
) {
    var reviewRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    val reviews by viewModel.getReviews(note.id, "NOTE").collectAsStateWithLifecycle(emptyList())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(note.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Cancel, null)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(note.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Shared by ${note.sellerName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(note.description, fontSize = 12.sp, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isPurchased && !isSeller) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Printable PDF Summary Notes", fontSize = 11.sp)
                                        Text("$${note.price}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(onClick = onBuyNote) {
                                        Text("Purchase Notes")
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lifetime PDF access active safely.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = onTriggerAiSummary) {
                                Row {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Summarize with AI", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Document Preview", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isPurchased || isSeller) note.previewText else {
                                val truncated = note.previewText.take(150)
                                "$truncated...\n\n[🔒 BUY NOTE CONTENT TO UNLOCK COMPLETE CHEATSHEET & PDF GUIDE]"
                            },
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Classmate Feedback & Reviews", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isPurchased && !isSeller) {
                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            placeholder = { Text("Share feedback...") },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Leave stars feedback") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rating: ")
                            (1..5).forEach { star ->
                                IconButton(onClick = { reviewRating = star }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = if (reviewRating >= star) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    if (reviewComment.isNotBlank()) {
                                        onAddReview(reviewRating, reviewComment)
                                        reviewComment = ""
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Post Review", fontSize = 11.sp)
                            }
                        }
                    }

                    reviews.forEach { rev ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(rev.userName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row {
                                        (1..rev.rating).forEach {
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(8.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(rev.comment, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizPlatformDialog(
    quiz: QuizEntity,
    onDismiss: () -> Unit
) {
    val questionList = remember(quiz.questionsJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, QuizQuestionModel::class.java)
            val adapter = moshi.adapter<List<QuizQuestionModel>>(listType)
            adapter.fromJson(quiz.questionsJson) ?: emptyList()
        } catch (e: Exception) {
            listOf(
                QuizQuestionModel("q1", "What is O(n log n) complex algorithm standard?", listOf("Bubble", "Quick"), 1, "Quick sort averages O(n log n)")
            )
        }
    }

    var currentQuestionIdx by remember { mutableStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var answerSubmitted by remember { mutableStateOf(false) }
    var scoreCount by remember { mutableStateOf(0) }
    var testComplete by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(quiz.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Cancel, null)
                    }
                }

                if (!testComplete && questionList.isNotEmpty()) {
                    val activeQuestion = questionList[currentQuestionIdx]
                    val progressValue = (currentQuestionIdx.toFloat() / questionList.size)

                    LinearProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Question ${currentQuestionIdx + 1} of ${questionList.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            activeQuestion.question,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        activeQuestion.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedOptionIdx == optIndex
                            val optionBorderColor = if (answerSubmitted) {
                                if (optIndex == activeQuestion.correctIndex) Color.Green else if (isSelected) Color.Red else MaterialTheme.colorScheme.outline
                            } else {
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            }

                            val optionBgColor = if (answerSubmitted) {
                                if (optIndex == activeQuestion.correctIndex) Color.Green.copy(alpha = 0.1f) else if (isSelected) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            } else {
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !answerSubmitted) { selectedOptionIdx = optIndex }
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, optionBorderColor),
                                colors = CardDefaults.cardColors(containerColor = optionBgColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (!answerSubmitted) selectedOptionIdx = optIndex },
                                        enabled = !answerSubmitted
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(optionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (answerSubmitted) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val wasCorrect = selectedOptionIdx == activeQuestion.correctIndex
                                    Text(
                                        text = if (wasCorrect) "🎉 Correct Answer!" else "❌ Incorrect",
                                        fontWeight = FontWeight.Bold,
                                        color = if (wasCorrect) Color.Green else Color.Red,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(activeQuestion.explanation, fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!answerSubmitted) {
                            Button(
                                onClick = {
                                    if (selectedOptionIdx != null) {
                                        answerSubmitted = true
                                        if (selectedOptionIdx == activeQuestion.correctIndex) {
                                            scoreCount++
                                        }
                                    }
                                },
                                enabled = selectedOptionIdx != null
                            ) {
                                Text("Check Answer")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (currentQuestionIdx + 1 < questionList.size) {
                                        currentQuestionIdx++
                                        selectedOptionIdx = null
                                        answerSubmitted = false
                                    } else {
                                        testComplete = true
                                    }
                                }
                            ) {
                                Text(if (currentQuestionIdx + 1 == questionList.size) "Show Quiz Summary" else "Next Question")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Quiz Completed Successfully!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Subject Profile: ${quiz.subject}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("YOUR SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "$scoreCount / ${questionList.size}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val pValue = (scoreCount.toFloat() / questionList.size) * 100
                                Text("Performance Rate: ${pValue.toInt()}%", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.width(180.dp)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class QuizQuestionModel(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)
