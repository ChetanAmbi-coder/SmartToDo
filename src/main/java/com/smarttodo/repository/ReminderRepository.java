package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Reminder;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReminderRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;

    public ReminderRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Reminder create(Reminder reminder) throws SQLException {
        String sql = "INSERT INTO reminders (task_id, reminder_date_time, is_triggered, is_dismissed, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, reminder.getTaskId());
            stmt.setString(2, reminder.getReminderDateTime().format(FORMATTER));
            stmt.setInt(3, reminder.isTriggered() ? 1 : 0);
            stmt.setInt(4, reminder.isDismissed() ? 1 : 0);
            stmt.setString(5, (reminder.getCreatedAt() != null ? reminder.getCreatedAt() : LocalDateTime.now()).format(FORMATTER));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reminder.setId(rs.getLong(1));
                }
            }
        }
        return reminder;
    }

    public void markTriggered(long id) throws SQLException {
        String sql = "UPDATE reminders SET is_triggered = 1 WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public void markDismissed(long id) throws SQLException {
        String sql = "UPDATE reminders SET is_dismissed = 1 WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public List<Reminder> findPendingReminders(LocalDateTime asOf) throws SQLException {
        List<Reminder> list = new ArrayList<>();
        // Join with tasks to ensure task is not deleted and not completed
        String sql = """
            SELECT r.*, t.title AS task_title
            FROM reminders r
            INNER JOIN tasks t ON r.task_id = t.id
            WHERE r.is_triggered = 0 
              AND r.is_dismissed = 0
              AND t.status != 'COMPLETED'
              AND r.reminder_date_time <= ?
            ORDER BY r.reminder_date_time ASC
        """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, asOf.format(FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public void deleteByTaskId(long taskId) throws SQLException {
        String sql = "DELETE FROM reminders WHERE task_id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM reminders WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Reminder mapResultSet(ResultSet rs) throws SQLException {
        Reminder r = new Reminder();
        r.setId(rs.getLong("id"));
        r.setTaskId(rs.getLong("task_id"));
        r.setTaskTitle(rs.getString("task_title"));
        String dtStr = rs.getString("reminder_date_time");
        if (dtStr != null) {
            r.setReminderDateTime(LocalDateTime.parse(dtStr, FORMATTER));
        }
        r.setTriggered(rs.getInt("is_triggered") == 1);
        r.setDismissed(rs.getInt("is_dismissed") == 1);
        String createdStr = rs.getString("created_at");
        if (createdStr != null) {
            r.setCreatedAt(LocalDateTime.parse(createdStr, FORMATTER));
        }
        return r;
    }
}
