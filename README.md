# Study Buddy 📚

Study Buddy is a simple Android app that helps students keep track of their study sessions, see where their time is going, and stay motivated with quick tips and advice.

The app is built with **Kotlin**, **Jetpack Compose**, and uses a small public API (Advice Slip) for motivational messages.

---

## Features

### 🧮 Track study sessions
- Add a session with:
  - **Subject** (e.g., “Data Structures”, “App Development”)
  - **Minutes studied**
  - **Topics covered** (multi-line notes, one topic per line)
- See all recent sessions in a scrollable list.
- Each session card shows:
  - Subject  
  - Date  
  - Total minutes  
  - Bullet list of topics
- Delete individual sessions from the list.

---

### 📊 Dashboard & stats (Home screen)

The home screen shows a quick overview of your study activity:

- **Total minutes studied** (all time)
- **Minutes in the last 7 days**
- **Weekly goal** (in minutes)
- A **progress bar** that compares last-7-days minutes to your weekly goal  
- **Top subject**: the subject you’ve spent the most minutes on (e.g. “Top subject: App Development (140 min)”)

Buttons on the home screen:

- **“View summary by subject”** – opens a breakdown of total minutes per subject
- **“Motivation”** – opens the motivation screen that calls the Advice API
- **“Study tips”** – static screen with helpful study tips
- **“Settings”** – dark mode + goal settings
- **“Focus timer”** – simple Pomodoro-style timer
- Floating **“+”** button – add a new study session

---

### 📑 Summary by subject

The **Summary** screen shows a list of all subjects with the total minutes studied for each one.

This makes it easy to answer questions like:
- Which class am I actually studying the most for?
- Where am I falling behind?

---

### 💡 Study tips screen

A simple screen with a short list of practical study tips, for example:

- Break study time into 25–30 minute blocks with short breaks.
- Mix old material with new material.
- Turn off notifications while studying.

This screen is static (no network calls) and is meant to show:
- Content design
- Another Compose screen in the app

---

### ⚙️ Settings (dark mode + weekly goal)

The **Settings** screen lets the user customize the app a bit:

- **Dark mode switch**
  - Toggles between light and dark themes for the entire app.
- **Weekly goal control**
  - Change the weekly study goal (in minutes).
  - The home screen progress bar uses this value.
- **Clear all sessions**
  - Button to delete all study sessions and reset the history.

---

### 🔥 Motivation (API integration)

The **Motivation** screen is where the app uses an external API.

- Uses the **Advice Slip API**: `https://api.adviceslip.com/advice`
- When the user taps **“Get study idea”**:
  - The app makes a Retrofit call on a background thread (via coroutines).
  - Parses the JSON response into a simple data class.
  - Displays the advice text in the UI.
- If the request fails (no internet, API down, etc.), the app shows a friendly error message.

This screen demonstrates:

- Retrofit setup (base URL, interface, service object)
- A `suspend` function call
- Using `remember` and `rememberCoroutineScope()` in Compose to manage async state (`isLoading`, `errorText`, `ideaText`)

---

### ⏱ Focus timer (Pomodoro-style)

The **Focus timer** screen is a basic Pomodoro-style timer:

- Choose a focus duration (e.g., 25 minutes).
- Start / pause / reset the timer.
- Uses Compose state and a ticking coroutine/`LaunchedEffect` to update the countdown.
- This feature ties into the “study productivity” theme and gives more to demo during the presentation.

*(If you changed the exact behavior, you can adjust this description to match your implementation.)*

---

## Architecture & Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
  - `Scaffold`, `TopAppBar`, `Card`, `LazyColumn`, `OutlinedTextField`, `Button`, `Switch`, etc.
- **State management:** `remember`, `mutableStateOf`, `mutableStateListOf`
- **Networking:**
  - **Retrofit 2**  
  - **GsonConverterFactory** for JSON
  - Kotlin **coroutines** (`suspend` functions + `rememberCoroutineScope().launch { ... }`)
- **Theming:**
  - Custom `StudyBuddyTheme`
  - Light / dark mode toggle in settings
- **Data storage (for this project):**
  - In-memory list of `StudySession` objects while the app is running  
  - Each session: `subject`, `minutes`, `topics`, `timestamp`

---

## Data models

```kotlin
data class StudySession(
    val subject: String,
    val minutes: Int,
    val topics: String,
    val timestamp: Long
)
