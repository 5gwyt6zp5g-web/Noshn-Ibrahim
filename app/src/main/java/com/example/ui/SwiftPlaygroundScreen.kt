package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Template Apps Definitions
enum class iOSSystemApp(val title: String, val systemIcon: ImageVector, val initialCode: String) {
    CALCULATOR(
        title = "iOS Calculator",
        systemIcon = Icons.Default.Calculate,
        initialCode = """import SwiftUI

struct iOSCalculator: View {
    // ====== iOS System App Code ======
    @State var display = "0"
    @State var accentColor = Color.orange // choices: orange, blue, purple, pink, green, yellow
    @State var buttonCornerRadius = 24 // 0 for strict square, 24 for standard, 48 for circle
    @State var appTitle = "Calculator pro"
    
    var body: some View {
        VStack(spacing: 12) {
            Text(appTitle).font(.headline).foregroundColor(.gray)
            Text(display)
                .font(.system(size: 64))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .padding()
            
            // Interaction grid is initialized live below.
            // Customize accent colors and button corner radii to compile.
        }
    }
}
"""
    ),
    NOTES(
        title = "iOS Notes",
        systemIcon = Icons.Default.Edit,
        initialCode = """import SwiftUI

struct iOSNotes: View {
    // ====== iOS System App Code ======
    @State var headerColor = Color.yellow // choices: yellow, orange, blue, green, pink, purple
    @State var appTitle = "My Study Notes" // Edit title of note app
    @State var showItemCount = true // Show notes counter at the bottom
    
    var body: some View {
        VStack {
            Text(appTitle)
                .font(.largeTitle)
                .bold()
                .foregroundColor(headerColor)
            
            // Add note items in the live text input below!
            // Change colors and headings to live hot-reload.
        }
    }
}
"""
    ),
    REMINDERS(
        title = "iOS Reminders",
        systemIcon = Icons.Default.List,
        initialCode = """import SwiftUI

struct iOSReminders: View {
    // ====== iOS System App Code ======
    @State var appTitle = "Smart Reminders" // Edit title
    @State var categoryColor = Color.pink // choices: pink, red, blue, purple, green, orange
    @State var showCompletedOnly = false // Toggle true/false to filter checked items
    
    var body: some View {
        VStack {
            Text(appTitle)
                .font(.title)
                .bold()
                .foregroundColor(categoryColor)
            
            // Manage and track reminders interactively.
        }
    }
}
"""
    )
}

data class SimulatedState(
    val title: String,
    val themeColor: Color,
    val borderRadius: Int,
    val optionState: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwiftPlaygroundScreen() {
    var selectedApp by remember { mutableStateOf(iOSSystemApp.CALCULATOR) }
    
    // Maintain separate code buffers for each template app so edits are preserved
    var calculatorCode by remember { mutableStateOf(iOSSystemApp.CALCULATOR.initialCode) }
    var notesCode by remember { mutableStateOf(iOSSystemApp.NOTES.initialCode) }
    var remindersCode by remember { mutableStateOf(iOSSystemApp.REMINDERS.initialCode) }

    val currentCode = remember(selectedApp, calculatorCode, notesCode, remindersCode) {
        when (selectedApp) {
            iOSSystemApp.CALCULATOR -> calculatorCode
            iOSSystemApp.NOTES -> notesCode
            iOSSystemApp.REMINDERS -> remindersCode
        }
    }

    var isCompiling by remember { mutableStateOf(false) }
    var logsList by remember { mutableStateOf(listOf("System idle. Code changes are ready for compilation.")) }
    var compileCount by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Interactive simulator app local states
    // 1. Calculator simulated values
    var calcDisplay by remember { mutableStateOf("0") }
    var calcPrevValue by remember { mutableStateOf<Double?>(null) }
    var calcActiveOp by remember { mutableStateOf<String?>(null) }
    var calcResetOnNext by remember { mutableStateOf(false) }

    // 2. Notes simulated values
    val notesList = remember { mutableStateListOf("Math Assignment", "Computer Science cheat-sheet", "Buy campus lunch") }
    var newNoteInput by remember { mutableStateOf("") }

    // 3. Reminders simulated values
    val remindersList = remember {
        mutableStateListOf(
            "Finish midterm submission" to true,
            "Study SwiftUI view state" to false,
            "Review student course resource" to false,
            "Book library room B" to true
        )
    }
    var newReminderInput by remember { mutableStateOf("") }

    // Parsed states (hot-compiled properties)
    val parsedState = remember(currentCode, selectedApp) {
        parseSwiftProperties(currentCode, selectedApp)
    }

    fun addLog(msg: String) {
        logsList = (logsList + "[${java.time.LocalTime.now().toString().take(12)}] $msg").takeLast(50)
    }

    fun runHotCompilation() {
        coroutineScope.launch {
            isCompiling = true
            addLog("STDOUT: [Swift-C] Initializing compilation of structure target ${selectedApp.name}...")
            delay(400)
            addLog("STDOUT: [LLVM-LD] Linking dynamic libraries 'SwiftUI', 'Foundation', 'Combine'...")
            delay(500)
            addLog("STDOUT: [Simulator] Hot-reloading view model and bindings update.")
            delay(400)
            isCompiling = false
            compileCount++
            addLog("SUCCESS: Playgrounds target successfully rebuilt! Status code: 0")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Header Styling matching premium guidelines (asymmetrical bold touch)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.IntegrationInstructions,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Swift Playgrounds",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "A dynamic programming console to customize & code system iOS apps.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Build compiler button matching standard
            Button(
                onClick = { runHotCompilation() },
                enabled = !isCompiling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("hot_compile_btn")
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Build",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assemble", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // App Selector Row
        Card(
            modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Select macOS/iOS App Target Codebase",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iOSSystemApp.values().forEach { app ->
                        val isSelected = selectedApp == app
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedApp = app
                                addLog("Switched active Xcode project to ${app.title}")
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = app.systemIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(app.title, fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("app_chip_${app.name}"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Main Programming Workspace - Code Editor + Simulator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Column A: Swift Code Editor console (Takes remaining weight)
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .height(490.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), // Xcode Dark Slate
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Editor Top Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D2D))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Apple Style window control buttons
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${selectedApp.name.lowercase().capitalize()}.swift",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                        
                        Text(
                            text = "SwiftUI Target",
                            fontSize = 9.sp,
                            color = Color(0xFFFF9500),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Source Code Editor Textfield
                    BasicTextField(
                        value = currentCode,
                        onValueChange = { newVal ->
                            when (selectedApp) {
                                iOSSystemApp.CALCULATOR -> calculatorCode = newVal
                                iOSSystemApp.NOTES -> notesCode = newVal
                                iOSSystemApp.REMINDERS -> remindersCode = newVal
                            }
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF1E1E1E))
                            .padding(12.dp)
                            .testTag("swift_code_input")
                            .verticalScroll(rememberScrollState()),
                        decorationBox = { innerTextField ->
                            // Custom layout displaying line numbers for Xcode coding realism
                            Row {
                                Column(
                                    modifier = Modifier.padding(end = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    val lines = currentCode.count { it == '\n' } + 1
                                    for (i in 1..lines) {
                                        Text(
                                            text = "$i",
                                            color = Color(0xFF555555),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    innerTextField()
                                }
                            }
                        }
                    )
                }
            }

            // Column B: Unified iOS iPhone Simulator Sandbox (Width constrained for mobile proportions)
            Column(
                modifier = Modifier
                    .width(170.dp)
                    .height(490.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Simulated Phone Bezel Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F0F0F), RoundedCornerShape(26.dp))
                        .border(4.dp, Color(0xFF2C2C2C), RoundedCornerShape(26.dp))
                        .padding(4.dp)
                ) {
                    // Mobile display inner safe container
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFEFEFEF))
                    ) {
                        // iPhone Status Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F0F0F))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "09:41",
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            // Simulated FaceID Notch / Dynamic Island
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(10.dp)
                                    .background(Color.Black, RoundedCornerShape(5.dp))
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(8.dp))
                                Icon(Icons.Default.BatteryFull, null, tint = Color.White, modifier = Modifier.size(8.dp))
                            }
                        }

                        // App Content Box running Simulated iOS App
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(if (selectedApp == iOSSystemApp.CALCULATOR) Color.Black else Color.White)
                        ) {
                            when (selectedApp) {
                                iOSSystemApp.CALCULATOR -> {
                                    SimulatediOSCalculator(
                                        appTitle = parsedState.title,
                                        accentColor = parsedState.themeColor,
                                        cornerRadius = parsedState.borderRadius,
                                        display = calcDisplay,
                                        onKeyTap = { char ->
                                            when (char) {
                                                "C" -> {
                                                    calcDisplay = "0"
                                                    calcPrevValue = null
                                                    calcActiveOp = null
                                                }
                                                "+", "-", "*", "/" -> {
                                                    calcPrevValue = calcDisplay.toDoubleOrNull()
                                                    calcActiveOp = char
                                                    calcResetOnNext = true
                                                }
                                                "=" -> {
                                                    val prev = calcPrevValue
                                                    val curr = calcDisplay.toDoubleOrNull()
                                                    if (prev != null && curr != null && calcActiveOp != null) {
                                                        val result = when (calcActiveOp) {
                                                            "+" -> prev + curr
                                                            "-" -> prev - curr
                                                            "*" -> prev * curr
                                                            "/" -> if (curr != 0.0) prev / curr else 0.0
                                                            else -> 0.0
                                                        }
                                                        calcDisplay = if (result % 1.0 == 0.0) result.toInt().toString() else "%.2f".format(result)
                                                        calcPrevValue = null
                                                        calcActiveOp = null
                                                    }
                                                }
                                                else -> { // Numbers 0-9
                                                    if (calcDisplay == "0" || calcResetOnNext) {
                                                        calcDisplay = char
                                                        calcResetOnNext = false
                                                    } else {
                                                        calcDisplay += char
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                iOSSystemApp.NOTES -> {
                                    SimulatediOSNotes(
                                        appTitle = parsedState.title,
                                        themeColor = parsedState.themeColor,
                                        showCount = parsedState.optionState,
                                        notes = notesList,
                                        newNoteText = newNoteInput,
                                        onNoteTextChange = { newNoteInput = it },
                                        onAddNote = {
                                            if (newNoteInput.isNotBlank()) {
                                                notesList.add(newNoteInput.trim())
                                                newNoteInput = ""
                                            }
                                        },
                                        onDeleteNote = { notesList.remove(it) }
                                    )
                                }
                                iOSSystemApp.REMINDERS -> {
                                    SimulatediOSReminders(
                                        appTitle = parsedState.title,
                                        themeColor = parsedState.themeColor,
                                        showDoneOnly = parsedState.optionState,
                                        reminders = remindersList,
                                        newInputStr = newReminderInput,
                                        onInputChange = { newReminderInput = it },
                                        onAddReminder = {
                                            if (newReminderInput.isNotBlank()) {
                                                remindersList.add(newReminderInput.trim() to false)
                                                newReminderInput = ""
                                            }
                                        },
                                        onToggleReminder = { index ->
                                            val current = remindersList[index]
                                            remindersList[index] = current.first to !current.second
                                        }
                                    )
                                }
                            }
                        }

                        // Simulated Home indicator line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedApp == iOSSystemApp.CALCULATOR) Color.Black else Color.White)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(3.dp)
                                    .background(if (selectedApp == iOSSystemApp.CALCULATOR) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f), RoundedCornerShape(1.5.dp))
                            )
                        }
                    }
                }
            }
        }

        // Live Compiler Outputs Console
        Card(
            modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Playgrounds Compilation Logs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Build: $compileCount rebuilt",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .background(Color(0xFF151515), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logsList) { log ->
                            Text(
                                text = log,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (log.contains("SUCCESS")) Color(0xFF27C93F) else if (log.contains("e:")) Color(0xFFFF5F56) else Color(0xFFDDDDDD)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Simulated iOS App Visual Blocks
@Composable
fun SimulatediOSCalculator(
    appTitle: String,
    accentColor: Color,
    cornerRadius: Int,
    display: String,
    onKeyTap: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = appTitle,
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = display,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }

        val rows = listOf(
            listOf("C", "+/-", "%", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { char ->
                        val isOp = char in listOf("/", "*", "-", "+", "=")
                        val isUt = char in listOf("C", "+/-", "%")
                        val btnColor = if (isOp) accentColor else if (isUt) Color(0xFFA5A5A5) else Color(0xFF333333)
                        val textColor = if (isUt) Color.Black else Color.White
                        val weight = if (char == "0") 2f else 1f

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(32.dp)
                                .clip(RoundedCornerShape(cornerRadius.dp))
                                .background(btnColor)
                                .clickable { onKeyTap(char) }
                                .testTag("sim_calc_key_$char"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatediOSNotes(
    appTitle: String,
    themeColor: Color,
    showCount: Boolean,
    notes: List<String>,
    newNoteText: String,
    onNoteTextChange: (String) -> Unit,
    onAddNote: () -> Unit,
    onDeleteNote: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Icon(Icons.Default.Book, null, tint = themeColor, modifier = Modifier.size(14.dp))
            }
            
            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

            // Notes List Scrollable
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(notes) { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note,
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { onDeleteNote(note) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        // Notes Input Panel
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = onNoteTextChange,
                    placeholder = { Text("New study note...", fontSize = 8.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 10.sp, color = Color.Black),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("sim_note_input"),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                IconButton(
                    onClick = onAddNote,
                    modifier = Modifier
                        .size(34.dp)
                        .background(themeColor, RoundedCornerShape(6.dp))
                        .testTag("sim_add_note_btn")
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            if (showCount) {
                Text(
                    text = "${notes.size} notes stored",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SimulatediOSReminders(
    appTitle: String,
    themeColor: Color,
    showDoneOnly: Boolean,
    reminders: List<Pair<String, Boolean>>,
    newInputStr: String,
    onInputChange: (String) -> Unit,
    onAddReminder: () -> Unit,
    onToggleReminder: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColor
            )
            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

            val filteredList = remember(reminders, showDoneOnly) {
                if (showDoneOnly) {
                    reminders.filter { it.second }
                } else {
                    reminders
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredList.size) { localIndex ->
                    val globalIndex = reminders.indexOf(filteredList[localIndex])
                    if (globalIndex != -1) {
                        val item = reminders[globalIndex]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleReminder(globalIndex) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .border(
                                        1.5.dp,
                                        if (item.second) themeColor else Color.Gray,
                                        CircleShape
                                    )
                                    .background(if (item.second) themeColor else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.second) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.first,
                                fontSize = 10.sp,
                                color = if (item.second) Color.LightGray else Color.Black,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Reminders Add Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = newInputStr,
                onValueChange = onInputChange,
                placeholder = { Text("Reminder description...", fontSize = 8.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 10.sp, color = Color.Black),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("sim_reminder_input"),
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            IconButton(
                onClick = onAddReminder,
                modifier = Modifier
                    .size(34.dp)
                    .background(themeColor, RoundedCornerShape(6.dp))
                    .testTag("sim_add_reminder_btn")
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// Regex SwiftUI Custom compiler settings parser
fun parseSwiftProperties(code: String, appType: iOSSystemApp): SimulatedState {
    val defaultTitle = when (appType) {
        iOSSystemApp.CALCULATOR -> "Calculator pro"
        iOSSystemApp.NOTES -> "My Study Notes"
        iOSSystemApp.REMINDERS -> "Smart Reminders"
    }
    
    val defaultColor = when (appType) {
        iOSSystemApp.CALCULATOR -> Color(0xFFFF9500) // Orange
        iOSSystemApp.NOTES -> Color(0xFFFFCC00) // Yellow
        iOSSystemApp.REMINDERS -> Color(0xFFFF2D55) // Pink
    }
    
    val defaultRadius = when (appType) {
        iOSSystemApp.CALCULATOR -> 24
        else -> 6
    }
    
    val defaultOption = when (appType) {
        iOSSystemApp.CALCULATOR -> false
        iOSSystemApp.NOTES -> true
        iOSSystemApp.REMINDERS -> false
    }

    return try {
        // Parse Title String from `appTitle = "..."`
        val titlePattern = """appTitle\s*=\s*"([^"]*)""""
        val parsedTitle = parseString(code, titlePattern, defaultTitle)

        // Parse Color target
        val colorPattern = when (appType) {
            iOSSystemApp.CALCULATOR -> """accentColor\s*=\s*Color\.([a-zA-Z]+)"""
            iOSSystemApp.NOTES -> """headerColor\s*=\s*Color\.([a-zA-Z]+)"""
            iOSSystemApp.REMINDERS -> """categoryColor\s*=\s*Color\.([a-zA-Z]+)"""
        }
        val parsedColor = parseColor(code, colorPattern, defaultColor)

        // Parse custom option choice
        val optionPattern = when (appType) {
            iOSSystemApp.CALCULATOR -> """buttonCornerRadius\s*=\s*(\d+)"""
            iOSSystemApp.NOTES -> """showItemCount\s*=\s*(true|false)"""
            iOSSystemApp.REMINDERS -> """showCompletedOnly\s*=\s*(true|false)"""
        }
        
        if (appType == iOSSystemApp.CALCULATOR) {
            val radius = parseInt(code, optionPattern, defaultRadius)
            SimulatedState(parsedTitle, parsedColor, radius, defaultOption)
        } else {
            val booleanState = parseBoolean(code, optionPattern, defaultOption)
            SimulatedState(parsedTitle, parsedColor, defaultRadius, booleanState)
        }
    } catch (e: Exception) {
        SimulatedState(defaultTitle, defaultColor, defaultRadius, defaultOption)
    }
}

// Low level string extract utilities
private fun parseString(code: String, pattern: String, default: String): String {
    val regex = pattern.toRegex()
    val match = regex.find(code)
    if (match != null) {
        return match.groupValues[1]
    }
    return default
}

private fun parseColor(code: String, pattern: String, default: Color): Color {
    val regex = pattern.toRegex()
    val match = regex.find(code)
    if (match != null) {
        val tintStr = match.groupValues[1].lowercase().trim()
        return when (tintStr) {
            "orange" -> Color(0xFFFF9500)
            "blue" -> Color(0xFF007AFF)
            "purple" -> Color(0xFFAF52DE)
            "green" -> Color(0xFF34C759)
            "red" -> Color(0xFFFF3B30)
            "pink" -> Color(0xFFFF2D55)
            "yellow" -> Color(0xFFFFCC00)
            "black" -> Color(0xFF000000)
            "gray" -> Color(0xFF8E8E93)
            else -> default
        }
    }
    return default
}

private fun parseInt(code: String, pattern: String, default: Int): Int {
    val regex = pattern.toRegex()
    val match = regex.find(code)
    if (match != null) {
        return match.groupValues[1].toIntOrNull() ?: default
    }
    return default
}

private fun parseBoolean(code: String, pattern: String, default: Boolean): Boolean {
    val regex = pattern.toRegex()
    val match = regex.find(code)
    if (match != null) {
        return match.groupValues[1].lowercase() == "true"
    }
    return default
}
