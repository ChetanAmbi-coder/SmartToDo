package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DatabaseManager databaseManager;

    public TaskRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Task create(Task task) throws SQLException {
        String sql = """
            INSERT INTO tasks (
                title, description, priority, status, is_important,
                project_id, category_id, start_date, due_date, due_time,
                estimated_duration, reminder_date_time, repeat_frequency, notes,
                created_at, updated_at, completed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = databaseManager.getConnection();
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, (task.getPriority() != null ? task.getPriority() : Priority.MEDIUM).name());
            stmt.setString(4, (task.getStatus() != null ? task.getStatus() : TaskStatus.TODO).name());
            stmt.setInt(5, task.isImportant() ? 1 : 0);

            if (task.getProjectId() != null) {
                stmt.setLong(6, task.getProjectId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            if (task.getCategoryId() != null) {
                stmt.setLong(7, task.getCategoryId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setString(8, task.getStartDate() != null ? task.getStartDate().format(DATE_FORMATTER) : null);
            stmt.setString(9, task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : null);
            stmt.setString(10, task.getDueTime() != null ? task.getDueTime().format(TIME_FORMATTER) : null);
            stmt.setInt(11, task.getEstimatedDuration());
            stmt.setString(12, task.getReminderDateTime() != null ? task.getReminderDateTime().format(DATE_TIME_FORMATTER) : null);
            stmt.setString(13, (task.getRepeatFrequency() != null ? task.getRepeatFrequency() : RepeatFrequency.NONE).name());
            stmt.setString(14, task.getNotes());
            stmt.setString(15, now.format(DATE_TIME_FORMATTER));
            stmt.setString(16, now.format(DATE_TIME_FORMATTER));
            stmt.setString(17, task.getCompletedAt() != null ? task.getCompletedAt().format(DATE_TIME_FORMATTER) : null);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setId(rs.getLong(1));
                }
            }
        }
        return task;
    }

    public void update(Task task) throws SQLException {
        String sql = """
            UPDATE tasks SET
                title = ?, description = ?, priority = ?, status = ?, is_important = ?,
                project_id = ?, category_id = ?, start_date = ?, due_date = ?, due_time = ?,
                estimated_duration = ?, reminder_date_time = ?, repeat_frequency = ?, notes = ?,
                updated_at = ?, completed_at = ?
            WHERE id = ?
        """;

        Connection conn = databaseManager.getConnection();
        LocalDateTime now = LocalDateTime.now();
        task.setUpdatedAt(now);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, (task.getPriority() != null ? task.getPriority() : Priority.MEDIUM).name());
            stmt.setString(4, (task.getStatus() != null ? task.getStatus() : TaskStatus.TODO).name());
            stmt.setInt(5, task.isImportant() ? 1 : 0);

            if (task.getProjectId() != null) {
                stmt.setLong(6, task.getProjectId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            if (task.getCategoryId() != null) {
                stmt.setLong(7, task.getCategoryId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setString(8, task.getStartDate() != null ? task.getStartDate().format(DATE_FORMATTER) : null);
            stmt.setString(9, task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : null);
            stmt.setString(10, task.getDueTime() != null ? task.getDueTime().format(TIME_FORMATTER) : null);
            stmt.setInt(11, task.getEstimatedDuration());
            stmt.setString(12, task.getReminderDateTime() != null ? task.getReminderDateTime().format(DATE_TIME_FORMATTER) : null);
            stmt.setString(13, (task.getRepeatFrequency() != null ? task.getRepeatFrequency() : RepeatFrequency.NONE).name());
            stmt.setString(14, task.getNotes());
            stmt.setString(15, now.format(DATE_TIME_FORMATTER));
            stmt.setString(16, task.getCompletedAt() != null ? task.getCompletedAt().format(DATE_TIME_FORMATTER) : null);
            stmt.setLong(17, task.getId());

            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public Optional<Task> findById(long id) throws SQLException {
        String sql = getBaseSelectQuery() + " WHERE t.id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Task> findAll() throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " ORDER BY t.created_at DESC";
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Task> findActiveTasks() throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.status != 'COMPLETED' ORDER BY t.due_date ASC, t.priority DESC";
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Task> findByStatus(TaskStatus status) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.status = ? ORDER BY t.updated_at DESC";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Task> findImportant() throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.is_important = 1 AND t.status != 'COMPLETED' ORDER BY t.due_date ASC, t.priority DESC";
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Task> findTodayTasks(LocalDate today) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + """
            WHERE (t.due_date = ? OR t.id IN (SELECT task_id FROM my_day_entries WHERE entry_date = ?))
            ORDER BY t.status ASC, t.due_time ASC, t.priority DESC
        """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, today.format(DATE_FORMATTER));
            stmt.setString(2, today.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Task> findUpcomingTasks(LocalDate start, LocalDate end) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.status != 'COMPLETED' AND t.due_date >= ? AND t.due_date <= ? ORDER BY t.due_date ASC, t.due_time ASC";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, start.format(DATE_FORMATTER));
            stmt.setString(2, end.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Task> findOverdueTasks(LocalDate today) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.status != 'COMPLETED' AND t.due_date < ? ORDER BY t.due_date ASC";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, today.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Task> findByProject(long projectId) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.project_id = ? ORDER BY t.status ASC, t.due_date ASC";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Task> findInboxTasks() throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.project_id IS NULL AND t.status != 'COMPLETED' ORDER BY t.created_at DESC";
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Task> findRecentCompleted(int limit) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE t.status = 'COMPLETED' ORDER BY t.completed_at DESC LIMIT ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Task> searchAndFilter(String query, Priority priority, Long categoryId, Long projectId,
                                      TaskStatus status, LocalDate dueDate, Boolean isImportant,
                                      Boolean isOverdue, String sortBy, boolean ascending) throws SQLException {
        List<Task> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(getBaseSelectQuery());
        sql.append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(t.title) LIKE ? OR LOWER(t.description) LIKE ? OR LOWER(t.notes) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (priority != null) {
            sql.append(" AND t.priority = ? ");
            params.add(priority.name());
        }

        if (categoryId != null) {
            sql.append(" AND t.category_id = ? ");
            params.add(categoryId);
        }

        if (projectId != null) {
            if (projectId == -1L) {
                // Special value for Inbox (no project)
                sql.append(" AND t.project_id IS NULL ");
            } else {
                sql.append(" AND t.project_id = ? ");
                params.add(projectId);
            }
        }

        if (status != null) {
            sql.append(" AND t.status = ? ");
            params.add(status.name());
        }

        if (dueDate != null) {
            sql.append(" AND t.due_date = ? ");
            params.add(dueDate.format(DATE_FORMATTER));
        }

        if (isImportant != null) {
            sql.append(" AND t.is_important = ? ");
            params.add(isImportant ? 1 : 0);
        }

        if (isOverdue != null && isOverdue) {
            sql.append(" AND t.status != 'COMPLETED' AND t.due_date < ? ");
            params.add(LocalDate.now().format(DATE_FORMATTER));
        }

        // Sorting
        sql.append(" ORDER BY ");
        if ("deadline".equalsIgnoreCase(sortBy)) {
            sql.append(" (t.due_date IS NULL) ASC, t.due_date ");
        } else if ("priority".equalsIgnoreCase(sortBy)) {
            sql.append(" CASE t.priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END ");
        } else if ("title".equalsIgnoreCase(sortBy)) {
            sql.append(" t.title ");
        } else {
            sql.append(" t.created_at ");
        }
        sql.append(ascending ? " ASC" : " DESC");

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    // Statistics queries
    public int countActive() throws SQLException {
        return executeCount("SELECT COUNT(*) FROM tasks WHERE status != 'COMPLETED'");
    }

    public int countCompleted() throws SQLException {
        return executeCount("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'");
    }

    public int countPending() throws SQLException {
        return executeCount("SELECT COUNT(*) FROM tasks WHERE status = 'TODO'");
    }

    public int countInProgress() throws SQLException {
        return executeCount("SELECT COUNT(*) FROM tasks WHERE status = 'IN_PROGRESS'");
    }

    public int countOverdue(LocalDate today) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status != 'COMPLETED' AND due_date < ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, today.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countDueToday(LocalDate today) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status != 'COMPLETED' AND due_date = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, today.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countCompletedInRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND completed_at >= ? AND completed_at <= ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, start.format(DATE_TIME_FORMATTER));
            stmt.setString(2, end.format(DATE_TIME_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int executeCount(String sql) throws SQLException {
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private String getBaseSelectQuery() {
        return """
            SELECT t.*,
                   p.name AS project_name, p.color AS project_color, p.icon AS project_icon,
                   c.name AS category_name, c.color AS category_color, c.icon AS category_icon,
                   (SELECT COUNT(*) FROM my_day_entries md WHERE md.task_id = t.id AND md.entry_date = DATE('now', 'localtime')) > 0 AS in_my_day
            FROM tasks t
            LEFT JOIN projects p ON t.project_id = p.id
            LEFT JOIN categories c ON t.category_id = c.id
        """;
    }

    private Task mapResultSet(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getLong("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setPriority(Priority.fromString(rs.getString("priority")));
        t.setStatus(TaskStatus.fromString(rs.getString("status")));
        t.setImportant(rs.getInt("is_important") == 1);

        long projectId = rs.getLong("project_id");
        if (!rs.wasNull()) {
            t.setProjectId(projectId);
            Project p = new Project();
            p.setId(projectId);
            p.setName(rs.getString("project_name"));
            p.setColor(rs.getString("project_color"));
            p.setIcon(rs.getString("project_icon"));
            t.setProject(p);
        }

        long categoryId = rs.getLong("category_id");
        if (!rs.wasNull()) {
            t.setCategoryId(categoryId);
            Category c = new Category();
            c.setId(categoryId);
            c.setName(rs.getString("category_name"));
            c.setColor(rs.getString("category_color"));
            c.setIcon(rs.getString("category_icon"));
            t.setCategory(c);
        }

        String startDateStr = rs.getString("start_date");
        if (startDateStr != null) {
            t.setStartDate(LocalDate.parse(startDateStr, DATE_FORMATTER));
        }

        String dueDateStr = rs.getString("due_date");
        if (dueDateStr != null) {
            t.setDueDate(LocalDate.parse(dueDateStr, DATE_FORMATTER));
        }

        String dueTimeStr = rs.getString("due_time");
        if (dueTimeStr != null) {
            t.setDueTime(LocalTime.parse(dueTimeStr, TIME_FORMATTER));
        }

        t.setEstimatedDuration(rs.getInt("estimated_duration"));

        String reminderStr = rs.getString("reminder_date_time");
        if (reminderStr != null) {
            t.setReminderDateTime(LocalDateTime.parse(reminderStr, DATE_TIME_FORMATTER));
        }

        t.setRepeatFrequency(RepeatFrequency.fromString(rs.getString("repeat_frequency")));
        t.setNotes(rs.getString("notes"));

        String createdStr = rs.getString("created_at");
        if (createdStr != null) {
            t.setCreatedAt(LocalDateTime.parse(createdStr, DATE_TIME_FORMATTER));
        }

        String updatedStr = rs.getString("updated_at");
        if (updatedStr != null) {
            t.setUpdatedAt(LocalDateTime.parse(updatedStr, DATE_TIME_FORMATTER));
        }

        String completedStr = rs.getString("completed_at");
        if (completedStr != null) {
            t.setCompletedAt(LocalDateTime.parse(completedStr, DATE_TIME_FORMATTER));
        }

        try {
            t.setInMyDay(rs.getInt("in_my_day") == 1);
        } catch (SQLException ignored) {}

        return t;
    }
}
