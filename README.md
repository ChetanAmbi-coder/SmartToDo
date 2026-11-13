# SMART-TO-DO-LIST

An intelligent, professional, dark-navy themed desktop task management application built entirely in **Java 21**, **JavaFX**, and **SQLite (JDBC)**.

---

## 🚀 Key Features

### 1. Productivity Dashboard
- Real-time productivity metrics: Active, Completed, Pending, In-Progress, Overdue, Due Today, Weekly Completion Rate.
- Dynamic 7-day completion Bar Chart and Project Distribution Pie Chart.
- Today's task list and upcoming 7-day deadlines.
- **Smart Recommendations Widget**: Highlights tasks calculated by the priority recommendation engine with explanation badges.
- Automatically updates on all task state changes without manual refreshes.

### 2. Smart Priority Recommendation Engine
- Deterministic, rule-driven algorithmic scoring model (no external AI APIs or paid services):
  $$\text{Score} = (40 \times \text{Importance}) + (30 \times \text{Urgency}) + (20 \times \text{DeadlineProximity}) + (10 \times \text{OverdueStatus})$$
- Normalized factor bounds $[0, 1]$.
- Deterministic multi-factor tie-breaking: Score $\rightarrow$ Earliest Deadline $\rightarrow$ Priority Level $\rightarrow$ Task ID.
- Plain-English reason generator: *"Overdue task"*, *"Deadline approaching"*, *"Critical priority"*, *"High importance"*, etc.

### 3. Comprehensive Task Management
- Specialized filtered views:
  - **My Day**: Daily focused planner with progress bar, remaining tasks, and estimated total workload calculation.
  - **Inbox**: Unassigned tasks awaiting triage.
  - **All Tasks**: Complete registry with search, multi-filter dropdowns, and sorting.
  - **Important**: High-priority flagged tasks.
  - **Upcoming**: Next 7-day scheduled tasks.
  - **Completed**: Archive of finished tasks with completion timestamps.
  - **Overdue**: Critical tasks that missed deadlines.
- Interactive Task Cards with priority stripes, completion checkboxes, status tags, project/category tags, and quick actions.
- Inline Quick-Add bar + full modal Task Dialog with validation.

### 4. Interactive Calendar Module
- **Monthly, Weekly, and Daily views** constructed with native JavaFX components.
- Color-coded task pills based on priority and status.
- Overdue indicators and deadline highlighting.
- Click any task pill on the calendar to open details or edit.

### 5. Focus Mode — Pomodoro Timer
- Standard 25-minute focus intervals, 5-minute short breaks, 15-minute long breaks (after 4 cycles).
- Canvas-rendered circular countdown progress arc with smooth timer display.
- Non-blocking background scheduler (`ScheduledExecutorService`) updating safely via `Platform.runLater()`.
- Session logging to SQLite with task association and today's total focus time.

### 6. Local Reminder & Notification System
- Background scheduler monitoring due reminders.
- Non-intrusive floating in-app notification toasts with auto-dismiss and sound cue toggle.
- Avoids duplicates and excludes deleted/completed tasks.

### 7. Productivity Analytics
- Quantitative performance charts: 7-day completion trends, active project breakdown, priority distribution.
- Safe calculations with zero division safeguards.

### 8. Project & Category Management
- Create, edit, and delete projects with custom colors and progress metrics.
- Safe deletion: deleting a project reassigns tasks to the Inbox without deleting them.
- Pre-seeded system categories: *Personal, Work, Education, Development, Health, Shopping*.

### 9. Themes & Settings
- **Dark Navy Theme** (modern slate, deep navy background, soft grey text, blue accents).
- **Clean Light Theme** (minimalist light grey and white appearance).
- Configurable default task priority, timer durations, notification toggles, and sample data generator.
- All settings persisted to SQLite.

---

## 🛠 Technology Stack

- **Language**: Java 21+
- **UI Framework**: OpenJFX (JavaFX 21.0.2)
- **Database**: SQLite 3 with Xerial SQLite JDBC (`sqlite-jdbc:3.45.1.0`)
- **Build System**: Apache Maven 3.9+
- **Testing**: JUnit 5 (Jupiter 5.10.2)

---

## 📁 Database Architecture & Location

- The SQLite database is automatically created and migrated on application startup.
- **Database File Location**:
  ```
  %USERPROFILE%\.smarttodo\smarttodo.db
  ```
  *(e.g., `C:\Users\<Username>\.smarttodo\smarttodo.db`)*
- Foreign keys are enforced (`PRAGMA foreign_keys = ON;`) with WAL mode enabled (`PRAGMA journal_mode = WAL;`).

### Database Tables:
1. `projects`
2. `categories`
3. `tasks`
4. `subtasks`
5. `focus_sessions`
6. `reminders`
7. `my_day_entries`
8. `app_settings`

---

## ⚙️ Running the Application

### Prerequisites:
- Java 21 or higher installed (`java -version`).
- Maven installed, or use the included `mvn.cmd` wrapper.

### Build and Run Command:

```bash
mvn javafx:run
```

Or using the local project wrapper on Windows:
```cmd
.\mvn.cmd javafx:run
```

### Running Automated Tests:

```bash
mvn test
```

Or:
```cmd
.\mvn.cmd test
```

---

## 🧪 Automated Test Suite

The project includes 24 automated unit tests covering:
- Task validation (empty title, invalid dates, negative durations, reminder constraints).
- CRUD operations and database transactions.
- Recurring task generation on completion.
- Rule-driven priority score calculation, normalization, tie-breaking, and explanation badges.
- Safe project deletion and foreign key cascade behavior.
- Database persistence across restarts.
- Analytics calculations with zero-division safety.
- Reminder scheduler trigger accuracy and duplicate suppression.
- Pomodoro timer state transitions and interval configuration.
