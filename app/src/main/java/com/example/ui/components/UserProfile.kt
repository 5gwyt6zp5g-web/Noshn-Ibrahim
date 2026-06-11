package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EducationalResourceEntity
import com.example.data.UserEntity
import com.example.data.CourseEntity
import com.example.data.NoteEntity
import com.example.data.PurchaseEntity
import com.example.data.WishlistItemEntity
import com.example.data.ResourceProgressEntity
import androidx.compose.ui.graphics.StrokeCap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getResourceSections(resourceId: String, title: String): List<String> {
    return when (resourceId) {
        "res_quantum" -> listOf("1. Hilbert Space & Bra-ket", "2. Superposition & Block sphere", "3. Entanglement (EPR)", "4. Quantum Logic Gates")
        "res_literature" -> listOf("1. Narrative Structures", "2. Elizabethan & Romanticism Poetry", "3. Victorian Novel Outlines", "4. Modernist Prose & Plays")
        "res_biology" -> listOf("1. Structure & Cellular RNA", "2. DNA Replication Loops", "3. Genetic Regulatory Loops", "4. Recombinant DNA Labs")
        "res_linear_alg" -> listOf("1. Matrix Multiplication Review", "2. Eigenvalues & Eigenvectors", "3. Singular Value Decomposition", "4. PCA Projection Tricks")
        else -> {
            val lowercase = title.lowercase()
            when {
                lowercase.contains("math") || lowercase.contains("algebra") || lowercase.contains("calculus") -> {
                    listOf("1. Fundamental Axioms", "2. Intermediate Proofs & Equations", "3. Advanced Application Exercises", "4. Key Formula Cheat Sheet")
                }
                lowercase.contains("bio") || lowercase.contains("chem") || lowercase.contains("science") -> {
                    listOf("1. Lab Protocols & Principles", "2. Molecular Pathways", "3. Empirical Observations", "4. Summary & Review Quiz")
                }
                lowercase.contains("computer") || lowercase.contains("code") || lowercase.contains("algorithm") -> {
                    listOf("1. Complexity Analysis (Big O)", "2. Code Templates", "3. Memory & Performance Tradeoffs", "4. Reference Implementations")
                }
                else -> {
                    listOf("1. Core Introduction", "2. In-depth Concept Breakdown", "3. Critical Case Exercises", "4. Review Outline & FAQ")
                }
            }
        }
    }
}

/**
 * A beautiful, functional, and production-grade UserProfile component
 * that showcases the user's details and active uploads from Cloud Firestore.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfile(
    user: UserEntity,
    uploadedResources: List<EducationalResourceEntity>,
    isFirestoreLoading: Boolean,
    onResourceClick: (EducationalResourceEntity) -> Unit,
    onRefreshFirestore: () -> Unit,
    onGoToStudioTab: () -> Unit,
    wishlistedCourses: List<CourseEntity> = emptyList(),
    wishlistedNotes: List<NoteEntity> = emptyList(),
    onCourseClick: (CourseEntity) -> Unit = {},
    onNoteClick: (NoteEntity) -> Unit = {},
    purchasedCourses: List<CourseEntity> = emptyList(),
    purchasedNotes: List<NoteEntity> = emptyList(),
    purchasedResources: List<EducationalResourceEntity> = emptyList(),
    allPurchases: List<PurchaseEntity> = emptyList(),
    allWishlistItems: List<WishlistItemEntity> = emptyList(),
    onDeleteBulkResources: (List<String>, () -> Unit) -> Unit = { _, _ -> },
    resourceProgress: List<ResourceProgressEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- PROFILE SUMMARY CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_summary_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header row with Icon Avatar and Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initialChar = if (user.name.isNotBlank()) user.name.first().uppercase() else "U"
                        Text(
                            text = initialChar,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = user.name,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (user.premiumUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.testTag("premium_badge")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Premium Member",
                                            modifier = Modifier.size(10.dp),
                                            tint = Color(0xFFF59E0B)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "PRO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = user.email,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Role Tag
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val roleColorClass = when (user.role) {
                                "ADMIN" -> MaterialTheme.colorScheme.error
                                "INSTRUCTOR" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                            val roleContainerClass = when (user.role) {
                                "ADMIN" -> MaterialTheme.colorScheme.errorContainer
                                "INSTRUCTOR" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }

                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = user.role,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = roleContainerClass,
                                    labelColor = roleColorClass
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // User Metrics / Detail Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Uploaded count",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uploadedResources.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Uploaded",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Earnings amount",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%.2f", user.earnings)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Earnings",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "User Account Status",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Verified",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                        Text(
                            text = "Status",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- PROFILE TABS AND NAVIGATION ---
        var selectedTab by remember { mutableStateOf(0) }
        val tabTitles = listOf("My Uploads", "Purchased", "Favorites", "Creator Stats")

        // Bulk Selection & Deletion States
        var isSelectionMode by remember { mutableStateOf(false) }
        val selectedResourceIds = remember { mutableStateListOf<String>() }
        var showConfirmationDialog by remember { mutableStateOf(false) }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().testTag("profile_tabs")
        ) {
            tabTitles.forEachIndexed { index, title ->
                val count = when (index) {
                    0 -> uploadedResources.size
                    1 -> purchasedCourses.size + purchasedNotes.size + purchasedResources.size
                    2 -> wishlistedCourses.size + wishlistedNotes.size
                    3 -> {
                        val uploadedIds = uploadedResources.map { it.id }.toSet()
                        val purchasedCount = allPurchases.count { it.contentId in uploadedIds }
                        val favoritedCount = allWishlistItems.count { it.itemId in uploadedIds }
                        purchasedCount + favoritedCount
                    }
                    else -> 0
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val icon = when (index) {
                                0 -> Icons.Default.CloudDone
                                1 -> Icons.Default.Receipt
                                2 -> Icons.Default.Favorite
                                3 -> Icons.Default.Assessment
                                else -> Icons.Default.Star
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                             )
                            if (count > 0) {
                                Badge(
                                    containerColor = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text("$count", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 2.dp))
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("profile_tab_$index")
                )
            }
        }

        // --- TAB CONTENT AREA ---
        if (selectedTab == 0) {
            // --- UPLOADED EDUCATIONAL RESOURCES LIST SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Uploads",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Uploaded Resources",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${uploadedResources.size}",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshFirestore,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("profile_refresh_button")
                    ) {
                        if (isFirestoreLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh uploads list",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (uploadedResources.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                isSelectionMode = !isSelectionMode
                                selectedResourceIds.clear()
                            }.testTag("toggle_selection_mode_container")
                        ) {
                            Checkbox(
                                checked = isSelectionMode,
                                onCheckedChange = { checked ->
                                    isSelectionMode = checked
                                    selectedResourceIds.clear()
                                },
                                modifier = Modifier.size(36.dp).testTag("selection_mode_checkbox")
                            )
                            Text(
                                text = "Bulk Select",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelectionMode) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (selectedResourceIds.size == uploadedResources.size) {
                                            selectedResourceIds.clear()
                                        } else {
                                            selectedResourceIds.clear()
                                            selectedResourceIds.addAll(uploadedResources.map { it.id })
                                        }
                                    },
                                    modifier = Modifier.testTag("bulk_select_all_btn")
                                ) {
                                    Text(
                                        text = if (selectedResourceIds.size == uploadedResources.size) "Clear All" else "Select All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (selectedResourceIds.isNotEmpty()) {
                                            showConfirmationDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    enabled = selectedResourceIds.isNotEmpty(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp).testTag("bulk_delete_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Bulk Delete",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delete (${selectedResourceIds.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (isFirestoreLoading && uploadedResources.isEmpty()) {
                    // Show a list of premium skeletons!
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val brush = rememberInfiniteShimmerBrush()
                        repeat(3) {
                            ProfileResourceSkeletonCard(brush = brush)
                        }
                    }
                } else if (uploadedResources.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_uploads_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "No Uploads icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Cloud Resources Yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Publish your notes or lecture syllabi directly to Cloud Firestore of this campus network.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onGoToStudioTab,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.height(38.dp).testTag("blank_uploads_cta")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Add upload",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go to Resource Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("uploaded_resources_list")
                    ) {
                        uploadedResources.forEach { resource ->
                            val isSelected = selectedResourceIds.contains(resource.id)
                            UploadedResourceRow(
                                resource = resource,
                                onClick = { onResourceClick(resource) },
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onSelectionChange = { selected ->
                                    if (selected) {
                                        if (!selectedResourceIds.contains(resource.id)) {
                                            selectedResourceIds.add(resource.id)
                                        }
                                    } else {
                                        selectedResourceIds.remove(resource.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            // --- PURCHASED LABECTURE & COURSE RESOURCES LIST SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Purchased resources",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Acquired Materials",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        val totalPurchases = purchasedCourses.size + purchasedNotes.size + purchasedResources.size
                        Text(
                            text = "$totalPurchases",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (purchasedCourses.isEmpty() && purchasedNotes.isEmpty() && purchasedResources.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_purchased_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "No Purchases icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Materials Acquired Yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Explore and enroll in interactive university lecture courses, purchase key notes, or study guidelines on the Main Storefront.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Enrolled Courses Group
                        if (purchasedCourses.isNotEmpty()) {
                            Text(
                                text = "Enrolled Courses",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            purchasedCourses.forEach { course ->
                                Card(
                                    onClick = { onCourseClick(course) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("purchased_course_item_${course.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Course icon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = course.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "By ${course.instructorName} • Video Course",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Access Course",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Purchased Notes Group
                        if (purchasedNotes.isNotEmpty()) {
                            Text(
                                text = "Revision Note Summaries",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            purchasedNotes.forEach { note ->
                                Card(
                                    onClick = { onNoteClick(note) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("purchased_note_item_${note.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Book,
                                                contentDescription = "PDF icon",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "By ${note.sellerName} • PDF study guide",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Access PDF",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Purchased Educational Resources Group
                        if (purchasedResources.isNotEmpty()) {
                            Text(
                                text = "Acquired Study Guides & Materials",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            purchasedResources.forEach { resource ->
                                Card(
                                    onClick = { onResourceClick(resource) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("purchased_resource_item_${resource.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val icon = when {
                                                resource.category.contains("Science", ignoreCase = true) -> Icons.Default.Science
                                                resource.category.contains("Math", ignoreCase = true) || resource.category.contains("Calculus", ignoreCase = true) -> Icons.Default.Calculate
                                                else -> Icons.Default.Category
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = resource.category,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = resource.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "By ${resource.authorName} • ${resource.category}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            // Visual completion progress
                                            val progressForRes = remember(resourceProgress, resource.id) {
                                                resourceProgress.find { it.resourceId == resource.id }
                                            }
                                            val completedCsv = progressForRes?.completedPagesCsv ?: ""
                                            val completedCount = if (completedCsv.isBlank()) 0 else completedCsv.split(",").size
                                            val sections = remember(resource.id, resource.title) {
                                                getResourceSections(resource.id, resource.title)
                                            }
                                            val totalCount = sections.size
                                            if (totalCount > 0) {
                                                val percent = completedCount.toFloat() / totalCount
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    LinearProgressIndicator(
                                                        progress = { percent },
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                                        strokeCap = StrokeCap.Round,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(5.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .testTag("resource_progress_bar_${resource.id}")
                                                    )
                                                    Text(
                                                        text = "${(percent * 100).toInt()}% Done ($completedCount/$totalCount)",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.testTag("resource_progress_text_${resource.id}")
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val contextForCard = androidx.compose.ui.platform.LocalContext.current
                                        IconButton(
                                            onClick = {
                                                val clipboard = contextForCard.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("Resource Link", "https://eduhub.app/resource/${resource.id}")
                                                clipboard.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(contextForCard, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("copy_purchased_resource_link_btn_${resource.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = "Copy Link",
                                                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Open Resource",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 2) {
            // --- MY WISHLIST / SAVED FAVORITES SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "My Favorites",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Wishlist & Favorites",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(
                                text = "${wishlistedCourses.size + wishlistedNotes.size}",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // PDF Export action button
                    if (wishlistedCourses.isNotEmpty() || wishlistedNotes.isNotEmpty()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        var exporting by remember { mutableStateOf(false) }

                        FilledTonalButton(
                            onClick = {
                                exporting = true
                                PdfExportHelper.exportFavoritesToPdf(
                                    context = context,
                                    user = user,
                                    courses = wishlistedCourses,
                                    notes = wishlistedNotes,
                                    onSuccess = { path ->
                                        exporting = false
                                        android.widget.Toast.makeText(context, "Favorites PDF generated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                        PdfExportHelper.openPdfFile(context, path)
                                    },
                                    onError = { error ->
                                        exporting = false
                                        android.widget.Toast.makeText(context, "Export failed: $error", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("export_favorites_pdf_btn"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            if (exporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export PDF",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (wishlistedCourses.isEmpty() && wishlistedNotes.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_favorites_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Empty wishlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Wishlist is Empty",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Keep track of courses or PDF revision keynotes you wish to buy or read later. Save them by tapping the Heart (♥) icon.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("wishlist_items_list")
                    ) {
                        wishlistedCourses.forEach { course ->
                            Card(
                                onClick = { onCourseClick(course) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wishlist_course_item_${course.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Course icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = course.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "By ${course.instructorName} • Course",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$${course.price}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        wishlistedNotes.forEach { note ->
                            Card(
                                onClick = { onNoteClick(note) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wishlist_note_item_${note.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = "PDF icon",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "By ${note.sellerName} • PDF summary",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$${note.price}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 3) {
            // --- CREATOR ANALYTICS & DASHBOARD TAB ---
            CreatorStatsDashboard(
                uploadedResources = uploadedResources,
                allPurchases = allPurchases,
                allWishlistItems = allWishlistItems,
                onResourceClick = onResourceClick
            )
        }
        
        if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                modifier = Modifier.testTag("bulk_delete_confirmation_dialog"),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Permanently Delete Resources?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "You are about to bulk delete ${selectedResourceIds.size} revision resource(s). This action is permanent and cannot be undone.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "The following items will be removed from this campus feed and Cloud Firestore:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val selectedResources = uploadedResources.filter { it.id in selectedResourceIds }
                                selectedResources.take(4).forEach { res ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = res.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (selectedResources.size > 4) {
                                    Text(
                                        text = "and ${selectedResources.size - 4} more...",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val idsToDelete = selectedResourceIds.toList()
                            onDeleteBulkResources(idsToDelete) {
                                selectedResourceIds.clear()
                                isSelectionMode = false
                                showConfirmationDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_bulk_delete_yes_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yes, Delete All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmationDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("confirm_bulk_delete_cancel_btn")
                    ) {
                        Text("Keep Materials", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadedResourceRow(
    resource: EducationalResourceEntity,
    onClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    Card(
        onClick = {
            if (isSelectionMode) {
                onSelectionChange(!isSelected)
            } else {
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("uploaded_resource_item_${resource.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .testTag("select_resource_checkbox_${resource.id}")
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    resource.category.contains("Science", ignoreCase = true) -> Icons.Default.Science
                    resource.category.contains("Math", ignoreCase = true) || resource.category.contains("Calculus", ignoreCase = true) -> Icons.Default.Calculate
                    resource.category.contains("Design", ignoreCase = true) || resource.category.contains("Art", ignoreCase = true) -> Icons.Default.Category
                    else -> Icons.Default.Book
                }
                Icon(
                    imageVector = icon,
                    contentDescription = resource.category,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resource.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (resource.price == 0.0) "Free" else "$${String.format("%.2f", resource.price)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (resource.price == 0.0) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = resource.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val formattedDate = try {
                        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        sdf.format(Date(resource.timestamp))
                    } catch (e: Exception) {
                        "Cloud Posted"
                    }
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (resource.reviewsCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${String.format("%.1f", resource.averageRating)} (${resource.reviewsCount} reviews)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Trailing copy link icon button
            val contextForCard = androidx.compose.ui.platform.LocalContext.current
            IconButton(
                onClick = {
                    val clipboard = contextForCard.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Resource Link", "https://eduhub.app/resource/${resource.id}")
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(contextForCard, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("copy_uploaded_resource_link_btn_${resource.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Copy Link",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileResourceSkeletonCard(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(45.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberInfiniteShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_animation"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreatorStatsDashboard(
    uploadedResources: List<EducationalResourceEntity>,
    allPurchases: List<PurchaseEntity>,
    allWishlistItems: List<WishlistItemEntity>,
    onResourceClick: (EducationalResourceEntity) -> Unit
) {
    val uploadedResourceIds = remember(uploadedResources) { uploadedResources.map { it.id }.toSet() }
    val resourcePurchases = remember(allPurchases, uploadedResourceIds) {
        allPurchases.filter { it.contentId in uploadedResourceIds }
    }
    val resourceWishlists = remember(allWishlistItems, uploadedResourceIds) {
        allWishlistItems.filter { it.itemId in uploadedResourceIds }
    }

    val totalSalesCount = resourcePurchases.size
    val totalFavoritesCount = resourceWishlists.size
    val totalGrossEarnings = resourcePurchases.sumOf { it.pricePaid }
    val netCreatorEarnings = totalGrossEarnings * 0.70

    val resourceStats = remember(uploadedResources, resourcePurchases, resourceWishlists) {
        uploadedResources.map { res ->
            val purchases = resourcePurchases.filter { it.contentId == res.id }
            val favorites = resourceWishlists.filter { it.itemId == res.id }
            val gross = purchases.sumOf { it.pricePaid }
            val net = gross * 0.70
            CreatorResourceStat(
                resource = res,
                salesCount = purchases.size,
                favoritesCount = favorites.size,
                netEarnings = net
            )
        }
    }
    val maxResourceNetEarnings = remember(resourceStats) {
        resourceStats.maxOfOrNull { it.netEarnings } ?: 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("creator_stats_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Resource Performance & Earnings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Wallet icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "Net Author Payout (70% Share)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$${String.format("%.2f", netCreatorEarnings)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Based on $${String.format("%.2f", totalGrossEarnings)} gross campus sales (EduHub takes 30%)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Sales",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalSalesCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("stat_total_sales")
                        )
                        Text(
                            text = "Acquisitions",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Saved",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalFavoritesCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("stat_total_favorites")
                        )
                        Text(
                            text = "In Wishlists",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Works",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uploadedResources.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("stat_total_uploads")
                        )
                        Text(
                            text = "Published",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Text(
            text = "Individual Resource Breakdown",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (resourceStats.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_empty_stats_card"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Blank statistics icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Statistics Available",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Once you upload revision materials and students purchase or save them, full detail insights will trigger here.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("dashboard_stats_items_list")
            ) {
                resourceStats.forEach { stat ->
                    CreatorResourcePerformanceCard(
                        stat = stat,
                        maxNetEarnings = maxResourceNetEarnings,
                        onClick = { onResourceClick(stat.resource) }
                    )
                }
            }
        }
    }
}

data class CreatorResourceStat(
    val resource: EducationalResourceEntity,
    val salesCount: Int,
    val favoritesCount: Int,
    val netEarnings: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorResourcePerformanceCard(
    stat: CreatorResourceStat,
    maxNetEarnings: Double,
    onClick: () -> Unit
) {
    val resource = stat.resource

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("creator_stat_item_${resource.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when {
                        resource.category.contains("Science", ignoreCase = true) -> Icons.Default.Science
                        resource.category.contains("Math", ignoreCase = true) || resource.category.contains("Calculus", ignoreCase = true) -> Icons.Default.Calculate
                        resource.category.contains("Design", ignoreCase = true) || resource.category.contains("Art", ignoreCase = true) -> Icons.Default.Category
                        else -> Icons.Default.Book
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${resource.category} • Listed for " + (if (resource.price == 0.0) "Free" else "$${String.format("%.2f", resource.price)}"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%.2f", stat.netEarnings)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Net Earnings",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${stat.salesCount} sales",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "${stat.favoritesCount} saves",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = "Demand",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val progressValue = if (maxNetEarnings > 0) {
                        (stat.netEarnings / maxNetEarnings).toFloat()
                    } else if (stat.salesCount > 0 || stat.favoritesCount > 0) {
                        0.5f
                    } else {
                        0.0f
                    }

                    LinearProgressIndicator(
                        progress = progressValue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
