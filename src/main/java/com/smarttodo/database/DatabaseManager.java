package com.smarttodo.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final String dbUrl;
    private Connection sharedConnection;

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        initialize();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            String dbDir = System.getProperty("user.home") + File.separator + ".smarttodo";
            File dir = new File(dbDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String defaultPath = dbDir + File.separator + "smarttodo.db";
            instance = new DatabaseManager("jdbc:sqlite:" + defaultPath);
        }
        return instance;
    }

    public static synchronized void setInstance(DatabaseManager customInstance) {
        if (instance != null) {
            instance.close();
        }
        instance = customInstance;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (sharedConnection == null || sharedConnection.isClosed()) {
            sharedConnection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = sharedConnection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
            }
        }
        return sharedConnection;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Projects table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    color TEXT DEFAULT '#3B82F6',
                    icon TEXT DEFAULT 'folder',
                    created_at TEXT NOT NULL
                );
            """);

            // 2. Categories table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    color TEXT DEFAULT '#64748B',
                    icon TEXT DEFAULT 'tag',
                    is_system INTEGER DEFAULT 0
                );
            """);

            // 3. Tasks table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    status TEXT NOT NULL DEFAULT 'TODO',
                    is_important INTEGER NOT NULL DEFAULT 0,
                    project_id INTEGER,
                    category_id INTEGER,
                    start_date TEXT,
                    due_date TEXT,
                    due_time TEXT,
                    estimated_duration INTEGER DEFAULT 30,
                    reminder_date_time TEXT,
                    repeat_frequency TEXT DEFAULT 'NONE',
                    notes TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    completed_at TEXT,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
                );
            """);

            // 4. Subtasks table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subtasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
                );
            """);

            // 5. Focus sessions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS focus_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER,
                    duration_minutes INTEGER NOT NULL,
                    session_type TEXT NOT NULL DEFAULT 'FOCUS',
                    completed_at TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
                );
            """);

            // 6. Reminders table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    reminder_date_time TEXT NOT NULL,
                    is_triggered INTEGER NOT NULL DEFAULT 0,
                    is_dismissed INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
                );
            """);

            // 7. My Day entries table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS my_day_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    entry_date TEXT NOT NULL,
                    added_at TEXT NOT NULL,
                    UNIQUE(task_id, entry_date),
                    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
                );
            """);

            // 8. App Settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL
                );
            """);

            // Indexes for fast querying
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks(project_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_subtasks_task ON subtasks(task_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_my_day_date ON my_day_entries(entry_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_reminders_trigger ON reminders(is_triggered, is_dismissed);");

            // Seed default categories
            seedDefaultCategories(conn);

            // Seed default settings
            seedDefaultSettings(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    private void seedDefaultCategories(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM categories";
        try (Statement stmt = conn.createStatement(); var rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO categories (name, color, icon, is_system) VALUES
                    ('Personal', '#EC4899', 'user', 1),
                    ('Work', '#3B82F6', 'briefcase', 1),
                    ('Education', '#8B5CF6', 'academic-cap', 1),
                    ('Development', '#10B981', 'code', 1),
                    ('Health', '#06B6D4', 'heart', 1),
                    ('Shopping', '#F59E0B', 'shopping-cart', 1);
                """);
            }
        }
    }

    private void seedDefaultSettings(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM app_settings";
        try (Statement stmt = conn.createStatement(); var rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO app_settings (setting_key, setting_value) VALUES
                    ('theme', 'DARK'),
                    ('default_priority', 'MEDIUM'),
                    ('pomodoro_focus_minutes', '25'),
                    ('pomodoro_short_break_minutes', '5'),
                    ('pomodoro_long_break_minutes', '15'),
                    ('pomodoro_long_break_interval', '4'),
                    ('notifications_enabled', 'true'),
                    ('sound_enabled', 'true');
                """);
            }
        }
    }

    public synchronized void close() {
        if (sharedConnection != null) {
            try {
                if (!sharedConnection.isClosed()) {
                    sharedConnection.close();
                }
            } catch (SQLException e) {
                // Log or ignore on shutdown
            }
            sharedConnection = null;
        }
    }
}
