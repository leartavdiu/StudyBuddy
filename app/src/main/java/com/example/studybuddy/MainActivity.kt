package com.example.studybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.studybuddy.ui.theme.StudyBuddyTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- DATA ----------

data class StudySession(
    val subject: String,
    val minutes: Int,
    val topics: String,
    val timestamp: Long         // when this session was created
)

enum class Screen {
    HOME,
    ADD,
    SUMMARY,
    TIPS,
    SETTINGS,
    STUDY_RESOURCES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDark by remember { mutableStateOf(false) }

            StudyBuddyTheme(darkTheme = isDark) {
                StudyBuddyApp(
                    isDark = isDark,
                    onToggleDarkMode = { isDark = !isDark }
                )
            }
        }
    }
}

// ---------- ROOT APP (FAKE NAV) ----------

@Composable
fun StudyBuddyApp(
    isDark: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    val sessions = remember { mutableStateListOf<StudySession>() }

    // weekly goal is now editable in Settings
    var weeklyGoal by remember { mutableStateOf(300) }

    fun addSession(subject: String, minutes: Int, topics: String) {
        if (subject.isBlank() || minutes <= 0) return
        val now = System.currentTimeMillis()
        sessions.add(
            StudySession(
                subject = subject.trim(),
                minutes = minutes,
                topics = topics.trim(),
                timestamp = now
            )
        )
    }

    fun deleteSession(session: StudySession) {
        sessions.remove(session)
    }

    val totalMinutes = sessions.sumOf { it.minutes }

    // last 7 days total
    val now = System.currentTimeMillis()
    val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
    val last7DaysMinutes = sessions
        .filter { it.timestamp >= now - sevenDaysMillis }
        .sumOf { it.minutes }

    val minutesBySubject: Map<String, Int> =
        sessions.groupBy { it.subject }.mapValues { entry ->
            entry.value.sumOf { it.minutes }
        }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.HOME -> HomeScreen(
                sessions = sessions,
                totalMinutes = totalMinutes,
                last7DaysMinutes = last7DaysMinutes,
                weeklyGoal = weeklyGoal,
                onAddClick = { currentScreen = Screen.ADD },
                onSummaryClick = { currentScreen = Screen.SUMMARY },
                onTipsClick = { currentScreen = Screen.TIPS },
                onSettingsClick = { currentScreen = Screen.SETTINGS },
                onResourcesClick = { currentScreen = Screen.STUDY_RESOURCES },
                onDeleteSession = { deleteSession(it) }
            )

            Screen.ADD -> AddSessionScreen(
                onSave = { subject, minutes, topics ->
                    addSession(subject, minutes, topics)
                    currentScreen = Screen.HOME
                },
                onCancel = { currentScreen = Screen.HOME }
            )

            Screen.SUMMARY -> SummaryScreen(
                minutesBySubject = minutesBySubject,
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.TIPS -> StudyTipsScreen(
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.SETTINGS -> SettingsScreen(
                isDark = isDark,
                weeklyGoal = weeklyGoal,
                onWeeklyGoalChange = { weeklyGoal = it },
                onToggleDarkMode = onToggleDarkMode,
                onClearAll = { sessions.clear() },
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.STUDY_RESOURCES -> StudyResourcesScreen(
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }
}

// ---------- HELPERS ----------

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ---------- HOME SCREEN ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessions: List<StudySession>,
    totalMinutes: Int,
    last7DaysMinutes: Int,
    weeklyGoal: Int,
    onAddClick: () -> Unit,
    onSummaryClick: () -> Unit,
    onTipsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onResourcesClick: () -> Unit,
    onDeleteSession: (StudySession) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Study Buddy") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add session")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Header / stats card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total minutes studied",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalMinutes min",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last 7 days: $last7DaysMinutes min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Weekly goal: $weeklyGoal min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val progress =
                        if (weeklyGoal <= 0) 0f
                        else (last7DaysMinutes.toFloat() / weeklyGoal.toFloat())
                            .coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSummaryClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("View summary by subject")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onResourcesClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Motivation")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTipsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Study tips")
                }
                OutlinedButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Settings")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent sessions",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (sessions.isEmpty()) {
                Text("No sessions yet. Tap + to add one.")
            } else {
                LazyColumn {
                    items(sessions.asReversed(), key = { it.timestamp }) { session ->
                        SessionCard(
                            session = session,
                            onDelete = { onDeleteSession(session) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: StudySession,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = session.subject,
                    style = MaterialTheme.typography.titleMedium
                )

                // date line
                Text(
                    text = formatDate(session.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val topicLines = session.topics.lines().filter { it.isNotBlank() }
                val topicColor = MaterialTheme.colorScheme.onSurfaceVariant

                if (topicLines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Topics:",
                        style = MaterialTheme.typography.bodySmall,
                        color = topicColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    topicLines.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = topicColor
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${session.minutes} min",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ---------- ADD SESSION SCREEN ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionScreen(
    onSave: (String, Int, String) -> Unit,
    onCancel: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Study Session") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = minutesText,
                onValueChange = { text -> minutesText = text },
                label = { Text("Minutes") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = topics,
                onValueChange = { topics = it },
                label = { Text("Topics covered (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                singleLine = false
            )

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please enter a subject and minutes > 0",
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val minutes = minutesText.toIntOrNull() ?: 0
                    if (subject.isBlank() || minutes <= 0) {
                        showError = true
                    } else {
                        showError = false
                        onSave(subject, minutes, topics)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        }
    }
}

// ---------- SUMMARY SCREEN ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    minutesBySubject: Map<String, Int>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary by Subject") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (minutesBySubject.isEmpty()) {
                Text("No study sessions yet.")
            } else {
                LazyColumn {
                    items(minutesBySubject.entries.toList(), key = { it.key }) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.key)
                                Text("${entry.value} min")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- STUDY TIPS SCREEN ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTipsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Tips") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Some quick tips:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val tips = listOf(
                "Break study time into 25–30 minute blocks with short breaks.",
                "Write down topics you don’t understand and review them later.",
                "Mix old material with new material in each session.",
                "Study a little every day instead of cramming.",
                "Turn off notifications during focused study time."
            )

            tips.forEach { tip ->
                Spacer(modifier = Modifier.height(6.dp))
                Text("• $tip")
            }
        }
    }
}

// ---------- MOTIVATION SCREEN (ADVICE API) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyResourcesScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var ideaText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Motivation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Need an idea or some motivation?",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = {
                    isLoading = true
                    errorText = null
                    ideaText = null

                    scope.launch {
                        try {
                            val response = AdviceApiService.api.getAdvice()
                            ideaText = response.slip?.advice ?: "No suggestion available."

                        } catch (e: Exception) {
                            errorText = "Could not load a suggestion. Check your internet and try again."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Get study idea")
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            errorText?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error
                )
            }

            ideaText?.let { idea ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = idea,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// ---------- SETTINGS SCREEN ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDark: Boolean,
    weeklyGoal: Int,
    onWeeklyGoalChange: (Int) -> Unit,
    onToggleDarkMode: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var weeklyGoalText by remember { mutableStateOf(weeklyGoal.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark mode")
                Switch(
                    checked = isDark,
                    onCheckedChange = { onToggleDarkMode() }
                )
            }

            Column {
                Text("Weekly goal (minutes)")
                OutlinedTextField(
                    value = weeklyGoalText,
                    onValueChange = { text ->
                        weeklyGoalText = text
                        val value = text.toIntOrNull()
                        if (value != null && value > 0) {
                            onWeeklyGoalChange(value)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Weekly goal in minutes") }
                )
            }

            Button(
                onClick = { onClearAll() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear all study sessions")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "App version 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
