package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A beautiful, reusable, state-aware Search Bar widget designed strictly under Material 3 layout principles.
 * Displays whether the active content collection filters are connected live to Google Cloud Firestore or the Local Room Cache.
 */
@Composable
fun FirestoreSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    isFirestoreOnline: Boolean,
    isFirestoreLoading: Boolean,
    firestoreMessage: String?,
    onClearMessage: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpandedStatus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Source status alert ribbon (Briefly describes Firestore status / errors)
        AnimatedVisibility(
            visible = firestoreMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (firestoreMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("firestore_status_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFirestoreOnline) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFirestoreOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = "Firestore Status",
                            tint = if (isFirestoreOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = firestoreMessage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isFirestoreOnline) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Status",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Main Premium Capsule Search Input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { 
                Text(
                    text = "Search dynamic cloud syllabus, authors, summaries...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ) 
            },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Search, 
                    contentDescription = "Search Collections",
                    tint = MaterialTheme.colorScheme.primary
                ) 
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    // Clear search text button
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChanged("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Firestore sync button
                    if (isFirestoreLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(20.dp)
                        )
                    } else {
                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier.testTag("firestore_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Cloud Firestore",
                                tint = if (isFirestoreOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_search_bar"),
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Source Connection indicators / tooltips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isFirestoreOnline) 
                    Color(0xFFE2F5EC) // Dynamic green accent
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clickable { isExpandedStatus = !isExpandedStatus }
                    .testTag("database_source_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (isFirestoreOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFirestoreOnline) "Cloud Firestore" else "Room (Offline Cache)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFirestoreOnline) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpandedStatus) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Connection Info",
                        tint = if (isFirestoreOnline) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = if (query.isNotBlank()) "Filtering active" else "Listing all content",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }

        // Expandable explanation tooltip helper
        AnimatedVisibility(
            visible = isExpandedStatus,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .testTag("source_tooltip_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isFirestoreOnline) {
                        "🔒 Live Connection: The campus database is live on Cloud Firestore! Searching filters records directly fetched from your remote Firestore collections."
                    } else {
                        "⚡ Offline Sandbox Mode: The cloud is currently offline or unconfigured. Search results fallback automatically to the high-performance local SQLite Room database cache."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
