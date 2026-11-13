package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.MyDayEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyDayRepository {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;

    public MyDayRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void addEntry(long taskId, LocalDate date) throws SQLException {
        String sql = "INSERT OR IGNORE INTO my_day_entries (task_id, entry_date, added_at) VALUES (?, ?, ?)";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            stmt.setString(2, date.format(DATE_FORMATTER));
            stmt.setString(3, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            stmt.executeUpdate();
        }
    }

    public void removeEntry(long taskId, LocalDate date) throws SQLException {
        String sql = "DELETE FROM my_day_entries WHERE task_id = ? AND entry_date = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            stmt.setString(2, date.format(DATE_FORMATTER));
            stmt.executeUpdate();
        }
    }

    public boolean isTaskInMyDay(long taskId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM my_day_entries WHERE task_id = ? AND entry_date = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            stmt.setString(2, date.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public Set<Long> getTaskIdsForDate(LocalDate date) throws SQLException {
        Set<Long> ids = new HashSet<>();
        String sql = "SELECT task_id FROM my_day_entries WHERE entry_date = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, date.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("task_id"));
                }
            }
        }
        return ids;
    }
}
