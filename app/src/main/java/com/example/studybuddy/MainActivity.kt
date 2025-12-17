package com.example.studybuddy

import android.app.DatePickerDialog
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.studybuddy.ui.theme.StudyBuddyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------- DATA ----------

data class StudySession(
    val id: Long,
    val subject: String,
    val minutes: Int,
    val topics: String,
    val timestamp: Long
)

enum class Screen {
    LOGIN,
    SIGNUP,
    HOME,
    ADD,
    EDIT,
    SUMMARY,
    TIPS,
    SETTINGS,
    MOTIVATION,
    POMODORO
}

// ---------- SIMPLE PREFS (LOGIN PERSISTENCE + WEEKLY GOAL) ----------

private const val PREFS_NAME = "studybuddy_prefs"
private const val KEY_LOGGED_IN = "logged_in"
private const val KEY_EMAIL = "email"
private const val KEY_WEEKLY_GOAL = "weekly_goal"

private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

private fun loadLoggedIn(context: Context): Boolean = prefs(context).getBoolean(KEY_LOGGED_IN, false)
private fun saveLoggedIn(context: Context, value: Boolean) { prefs(context).edit().putBoolean(KEY_LOGGED_IN, value).apply() }

private fun loadEmail(context: Context): String = prefs(context).getString(KEY_EMAIL, "") ?: ""
private fun saveEmail(context: Context, value: String) { prefs(context).edit().putString(KEY_EMAIL, value).apply() }

private fun loadWeeklyGoal(context: Context): Int = prefs(context).getInt(KEY_WEEKLY_GOAL, 300)
private fun saveWeeklyGoal(context: Context, value: Int) { prefs(context).edit().putInt(KEY_WEEKLY_GOAL, value).apply() }

// ---------- HELPERS ----------

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun startOfDayMillis(year: Int, month0: Int, day: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month0)
    cal.set(Calendar.DAY_OF_MONTH, day)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
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
    val context = LocalContext.current

    var currentScreen by remember {
        mutableStateOf(if (loadLoggedIn(context)) Screen.HOME else Screen.LOGIN)
    }

    // weekly goal persistence
    var weeklyGoal by remember { mutableStateOf(loadWeeklyGoal(context)) }

    // in-memory sessions
    val sessions = remember { mutableStateListOf<StudySession>() }

    // edit target
    var editTarget by remember { mutableStateOf<StudySession?>(null) }

    fun addSession(subject: String, minutes: Int, topics: String, timestamp: Long) {
        if (subject.isBlank() || minutes <= 0) return
        sessions.add(
            StudySession(
                id = System.currentTimeMillis(),
                subject = subject.trim(),
                minutes = minutes,
                topics = topics.trim(),
                timestamp = timestamp
            )
        )
    }

    fun updateSession(updated: StudySession) {
        val idx = sessions.indexOfFirst { it.id == updated.id }
        if (idx != -1) sessions[idx] = updated
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

    val topSubject: String? =
        minutesBySubject.entries.maxByOrNull { it.value }?.key

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.LOGIN -> LoginScreen(
                initialEmail = loadEmail(context),
                onLogin = { email ->
                    saveEmail(context, email)
                    saveLoggedIn(context, true)
                    currentScreen = Screen.HOME
                },
                onGoSignup = { currentScreen = Screen.SIGNUP }
            )

            Screen.SIGNUP -> SignupScreen(
                onSignup = { email ->
                    saveEmail(context, email)
                    saveLoggedIn(context, true)
                    currentScreen = Screen.HOME
                },
                onBack = { currentScreen = Screen.LOGIN }
            )

            Screen.HOME -> HomeScreen(
                sessions = sessions,
                totalMinutes = totalMinutes,
                last7DaysMinutes = last7DaysMinutes,
                weeklyGoal = weeklyGoal,
                topSubject = topSubject,
                onAddClick = { currentScreen = Screen.ADD },
                onSummaryClick = { currentScreen = Screen.SUMMARY },
                onTipsClick = { currentScreen = Screen.TIPS },
                onSettingsClick = { currentScreen = Screen.SETTINGS },
                onMotivationClick = { currentScreen = Screen.MOTIVATION },
                onPomodoroClick = { currentScreen = Screen.POMODORO },
                onDeleteSession = { deleteSession(it) },
                onEditSession = {
                    editTarget = it
                    currentScreen = Screen.EDIT
                }
            )

            Screen.ADD -> AddSessionScreen(
                onSave = { subject, minutes, topics, timestamp ->
                    addSession(subject, minutes, topics, timestamp)
                    currentScreen = Screen.HOME
                },
                onCancel = { currentScreen = Screen.HOME }
            )

            Screen.EDIT -> EditSessionScreen(
                session = editTarget,
                onSave = { updated ->
                    updateSession(updated)
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
                onToggleDarkMode = onToggleDarkMode,
                weeklyGoal = weeklyGoal,
                onSaveWeeklyGoal = { newGoal ->
                    weeklyGoal = newGoal
                    saveWeeklyGoal(context, newGoal)
                },
                onClearAll = { sessions.clear() },
                onLogout = {
                    saveLoggedIn(context, false)
                    currentScreen = Screen.LOGIN
                },
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.MOTIVATION -> MotivationScreen(
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.POMODORO -> PomodoroScreen(
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }
}

// ---------- LOGIN / SIGNUP ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialEmail: String,
    onLogin: (String) -> Unit,
    onGoSignup: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Login") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            if (showError) {
                Text("Enter an email + password", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (email.trim().isEmpty() || password.isEmpty()) showError = true
                    else onLogin(email.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Login") }

            TextButton(onClick = onGoSignup) {
                Text("Create an account")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignup: (String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Up") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            if (showError) {
                Text("Enter an email + password", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (email.trim().isEmpty() || password.isEmpty()) showError = true
                    else onSignup(email.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Create account") }
        }
    }
}

// ---------- HOME SCREEN ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessions: List<StudySession>,
    totalMinutes: Int,
    last7DaysMinutes: Int,
    weeklyGoal: Int,
    topSubject: String?,
    onAddClick: () -> Unit,
    onSummaryClick: () -> Unit,
    onTipsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMotivationClick: () -> Unit,
    onPomodoroClick: () -> Unit,
    onDeleteSession: (StudySession) -> Unit,
    onEditSession: (StudySession) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Study Buddy") }) },
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
                        else (last7DaysMinutes.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Top subject: ${topSubject ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSummaryClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) { Text("View summary by subject") }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPomodoroClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) { Text("Pomodoro") }

                OutlinedButton(
                    onClick = onMotivationClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) { Text("Motivation") }
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
                ) { Text("Study tips") }

                OutlinedButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) { Text("Settings") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Recent sessions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (sessions.isEmpty()) {
                Text("No sessions yet. Tap + to add one.")
            } else {
                LazyColumn {
                    items(sessions.asReversed(), key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onDelete = { onDeleteSession(session) },
                            onEdit = { onEditSession(session) }
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
    onDelete: () -> Unit,
    onEdit: () -> Unit
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
                Text(text = session.subject, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = formatDate(session.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val topicLines = session.topics.lines().filter { it.isNotBlank() }
                if (topicLines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Topics:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    topicLines.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${session.minutes} min", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ---------- ADD / EDIT SESSION (DATE PICKER INCLUDED) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionScreen(
    onSave: (String, Int, String, Long) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val cal = remember { Calendar.getInstance() }

    var subject by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // default: today
    var chosenTimestamp by remember { mutableStateOf(startOfDayMillis(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))) }

    val dateLabel = remember(chosenTimestamp) { formatDate(chosenTimestamp) }

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
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
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
                onValueChange = { minutesText = it },
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val now = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m0, d ->
                            chosenTimestamp = startOfDayMillis(y, m0, d)
                        },
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Date: $dateLabel")
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please enter a subject and minutes > 0", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val minutes = minutesText.toIntOrNull() ?: 0
                    if (subject.isBlank() || minutes <= 0) {
                        showError = true
                    } else {
                        showError = false
                        onSave(subject, minutes, topics, chosenTimestamp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionScreen(
    session: StudySession?,
    onSave: (StudySession) -> Unit,
    onCancel: () -> Unit
) {
    if (session == null) {
        // safety fallback
        onCancel()
        return
    }

    val context = LocalContext.current

    var subject by remember { mutableStateOf(session.subject) }
    var minutesText by remember { mutableStateOf(session.minutes.toString()) }
    var topics by remember { mutableStateOf(session.topics) }
    var chosenTimestamp by remember { mutableStateOf(session.timestamp) }
    var showError by remember { mutableStateOf(false) }

    val dateLabel = remember(chosenTimestamp) { formatDate(chosenTimestamp) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Study Session") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
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
                onValueChange = { minutesText = it },
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = chosenTimestamp
                    DatePickerDialog(
                        context,
                        { _, y, m0, d ->
                            chosenTimestamp = startOfDayMillis(y, m0, d)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Date: $dateLabel")
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please enter a subject and minutes > 0", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val minutes = minutesText.toIntOrNull() ?: 0
                    if (subject.isBlank() || minutes <= 0) {
                        showError = true
                    } else {
                        showError = false
                        onSave(
                            session.copy(
                                subject = subject.trim(),
                                minutes = minutes,
                                topics = topics.trim(),
                                timestamp = chosenTimestamp
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save changes") }
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
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
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

// ---------- STUDY TIPS SCREEN (UNCHANGED CONTENT) ----------

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
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
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

// ---------- SETTINGS SCREEN (ADD WEEKLY GOAL EDIT + LOGOUT) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDark: Boolean,
    onToggleDarkMode: () -> Unit,
    weeklyGoal: Int,
    onSaveWeeklyGoal: (Int) -> Unit,
    onClearAll: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    var goalText by remember { mutableStateOf(weeklyGoal.toString()) }
    var goalError by remember { mutableStateOf(false) }

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
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark mode")
                Switch(checked = isDark, onCheckedChange = { onToggleDarkMode() })
            }

            Text(text = "Weekly goal (minutes)")

            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Example: 300") }
            )

            if (goalError) {
                Text("Enter a number > 0", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val g = goalText.toIntOrNull() ?: 0
                    if (g <= 0) {
                        goalError = true
                    } else {
                        goalError = false
                        onSaveWeeklyGoal(g)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save weekly goal") }

            Button(
                onClick = onClearAll,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Clear all study sessions") }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Log out") }

            Text(
                text = "App version 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

// ---------- POMODORO (BACK IN) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit
) {
    var minutesText by remember { mutableStateOf("25") }
    var secondsLeft by remember { mutableStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun resetFromInput() {
        val m = (minutesText.toIntOrNull() ?: 25).coerceIn(1, 180)
        secondsLeft = m * 60
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        if (secondsLeft == 0) isRunning = false
    }

    val mm = (secondsLeft / 60).toString().padStart(2, '0')
    val ss = (secondsLeft % 60).toString().padStart(2, '0')

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Focus timer", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = minutesText,
                onValueChange = { minutesText = it },
                label = { Text("Minutes (e.g. 25)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "$mm:$ss",
                style = MaterialTheme.typography.headlineLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (!isRunning) {
                            resetFromInput()
                            isRunning = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Start") }

                OutlinedButton(
                    onClick = { isRunning = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Pause") }

                OutlinedButton(
                    onClick = {
                        isRunning = false
                        resetFromInput()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Reset") }
            }

            Text(
                text = "Tip: Use 25 min focus + 5 min break.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- MOTIVATION (API) ----------
// IMPORTANT: this screen assumes you have AdviceApiService + data classes in AdviceApi.kt.
// If you want, paste your AdviceApi.kt and I’ll align the response model exactly.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var adviceText by remember { mutableStateOf<String?>(null) }

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
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Get a quick motivational tip:", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = {
                    isLoading = true
                    errorText = null
                    adviceText = null

                    scope.launch {
                        try {
                            // EXPECTS: AdviceApiService.api.getAdvice() returns a model that contains slip.advice
                            val resp = AdviceApiService.api.getAdvice()
                            adviceText = resp.slip?.advice ?: "No advice found. Try again."
                        } catch (e: Exception) {
                            errorText = "Could not load advice. Try again."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Get advice") }

            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            adviceText?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
