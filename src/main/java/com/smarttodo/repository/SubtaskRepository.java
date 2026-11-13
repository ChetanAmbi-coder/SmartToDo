package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Subtask;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SubtaskRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;

    public SubtaskRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Subtask create(Subtask subtask) throws SQLException {
        String sql = "INSERT INTO subtasks (task_id, title, is_completed, created_at) VALUES (?, ?, ?, ?)";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, subtask.getTaskId());
            stmt.setString(2, subtask.getTitle());
            stmt.setInt(3, subtask.isCompleted() ? 1 : 0);
            stmt.setString(4, (subtask.getCreatedAt() != null ? subtask.getCreatedAt() : LocalDateTime.now()).format(FORMATTER));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    subtask.setId(rs.getLong(1));
                }
            }
        }
        return subtask;
    }

    public void update(Subtask subtask) throws SQLException {
        String sql = "UPDATE subtasks SET title = ?, is_completed = ? WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subtask.getTitle());
            stmt.setInt(2, subtask.isCompleted() ? 1 : 0);
            stmt.setLong(3, subtask.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM subtasks WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public void deleteByTaskId(long taskId) throws SQLException {
        String sql = "DELETE FROM subtasks WHERE task_id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            stmt.executeUpdate();
        }
    }

    public List<Subtask> findByTaskId(long taskId) throws SQLException {
        List<Subtask> list = new ArrayList<>();
        String sql = "SELECT * FROM subtasks WHERE task_id = ? ORDER BY id ASC";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    private Subtask mapResultSet(ResultSet rs) throws SQLException {
        Subtask s = new Subtask();
        s.setId(rs.getLong("id"));
        s.setTaskId(rs.getLong("task_id"));
        s.setTitle(rs.getString("title"));
        s.setCompleted(rs.getInt("is_completed") == 1);
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            s.setCreatedAt(LocalDateTime.parse(createdAtStr, FORMATTER));
        }
        return s;
    }
}
