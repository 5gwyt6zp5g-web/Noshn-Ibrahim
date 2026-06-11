package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity

/**
 * A highly interactive, beautiful, and fully validated Material 3 form component
 * designed for students to publish educational resources directly to Cloud Firestore.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EducationalResourceForm(
    categories: List<CategoryEntity>,
    onSubmit: (title: String, description: String, price: Double, category: String, tags: String) -> Unit,
    onCancel: () -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var tagsInput by remember { mutableStateOf("") }

    // Validation State
    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Icon & Explanation Text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Publish Syllabus & Study Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Store dynamic resources directly in Cloud Firestore",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Dynamic Firestore Submission Failure Alert Banner
        if (errorMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_error_banner"),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error icon",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Submission Failed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Field 1: Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Resource Title *",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (titleError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isBlank()) {
                        titleError = "Title cannot be blank"
                    } else if (it.trim().length < 5) {
                        titleError = "Title must be at least 5 characters"
                    } else if (it.trim().length > 80) {
                        titleError = "Title cannot exceed 80 characters"
                    } else {
                        titleError = null
                    }
                },
                enabled = !isSubmitting,
                placeholder = { Text("e.g. Advanced Quantum Computing Draft") },
                isError = titleError != null,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Title,
                        contentDescription = null,
                        tint = if (titleError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resource_title_input")
            )
            if (titleError != null) {
                Text(
                    text = titleError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Field 2: Description
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Detailed Syllabus & Description *",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (descriptionError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    if (it.isBlank()) {
                        descriptionError = "Description cannot be blank"
                    } else if (it.trim().length < 15) {
                        descriptionError = "Syllabus description must be at least 15 characters"
                    } else if (it.trim().length > 1000) {
                        descriptionError = "Syllabus description cannot exceed 1000 characters"
                    } else {
                        descriptionError = null
                    }
                },
                enabled = !isSubmitting,
                placeholder = { Text("Syllabus keywords, chapter outlines, or detailed learning summaries...") },
                isError = descriptionError != null,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (descriptionError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resource_desc_input")
            )
            if (descriptionError != null) {
                Text(
                    text = descriptionError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Field 3: Price
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Target List Price ($) *",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (priceError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = priceStr,
                onValueChange = {
                    priceStr = it
                    val parsed = it.toDoubleOrNull()
                    if (it.isBlank()) {
                        priceError = "Price is required"
                    } else if (parsed == null) {
                        priceError = "Please enter a valid number (e.g., 9.99)"
                    } else if (parsed < 0.0) {
                        priceError = "Price cannot be negative"
                    } else if (parsed > 10000.0) {
                        priceError = "Price cannot exceed $10,000.00"
                    } else {
                        priceError = null
                    }
                },
                enabled = !isSubmitting,
                placeholder = { Text("e.g. 14.99 (or 0 for Free)") },
                isError = priceError != null,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = if (priceError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resource_price_input")
            )
            if (priceError != null) {
                Text(
                    text = priceError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Field 4: Category Segment (M3 Grid/Chips style)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Academic Category *",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Choose the campus discipline where this resource will be indexed:",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = selectedCategoryIndex == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (!isSubmitting) selectedCategoryIndex = index },
                        enabled = !isSubmitting,
                        label = { Text(category.name, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Field 5: Custom Tags (Polished tag chip visualizer)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Custom Keyword Tags",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Enter comma-separated keywords to help other students filter & discover (e.g., midterm, cheat-sheet, algebra):",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                enabled = !isSubmitting,
                placeholder = { Text("e.g. formula-sheet, physics, cheat-sheet") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resource_tags_input")
            )
            
            // Dynamic chip list of typed keywords
            val parsedTags = remember(tagsInput) {
                tagsInput.split(",")
                    .map { it.trim().substringBefore(" ").lowercase() }
                    .filter { it.isNotEmpty() }
            }
            if (parsedTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    parsedTags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text("#$tag", fontSize = 10.sp) },
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    // Pre-submission validation suite
                    var isValid = true
                    
                    if (title.isBlank()) {
                        titleError = "Title cannot be blank"
                        isValid = false
                    } else if (title.trim().length < 5) {
                        titleError = "Title must be at least 5 characters"
                        isValid = false
                    } else if (title.trim().length > 80) {
                        titleError = "Title cannot exceed 80 characters"
                        isValid = false
                    }

                    if (description.isBlank()) {
                        descriptionError = "Description cannot be blank"
                        isValid = false
                    } else if (description.trim().length < 15) {
                        descriptionError = "Syllabus description must be at least 15 characters"
                        isValid = false
                    } else if (description.trim().length > 1000) {
                        descriptionError = "Syllabus description cannot exceed 1000 characters"
                        isValid = false
                    }

                    val parsedPrice = priceStr.toDoubleOrNull()
                    if (priceStr.isBlank()) {
                        priceError = "Price is required"
                        isValid = false
                    } else if (parsedPrice == null) {
                        priceError = "Please enter a valid number (e.g., 9.99)"
                        isValid = false
                    } else if (parsedPrice < 0.0) {
                        priceError = "Price cannot be negative"
                        isValid = false
                    } else if (parsedPrice > 10000.0) {
                        priceError = "Price cannot exceed $10,000.00"
                        isValid = false
                    }

                    if (isValid && !isSubmitting) {
                        val finalPrice = parsedPrice ?: 0.0
                        val finalCategory = if (categories.isNotEmpty()) {
                            categories[selectedCategoryIndex].name
                        } else {
                            "Computer Science"
                        }
                        onSubmit(title, description, finalPrice, finalCategory, tagsInput)
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("resource_submit_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publishing...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish to Cloud", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
