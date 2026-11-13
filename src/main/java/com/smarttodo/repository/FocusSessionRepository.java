package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.FocusSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FocusSessionRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;

    public FocusSessionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public FocusSession create(FocusSession session) throws SQLException {
        String sql = "INSERT INTO focus_sessions (task_id, duration_minutes, session_type, completed_at, notes) VALUES (?, ?, ?, ?, ?)";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (session.getTaskId() != null) {
                stmt.setLong(1, session.getTaskId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, session.getDurationMinutes());
            stmt.setString(3, session.getSessionType().name());
            stmt.setString(4, (session.getCompletedAt() != null ? session.getCompletedAt() : LocalDateTime.now()).format(FORMATTER));
            stmt.setString(5, session.getNotes());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    session.setId(rs.getLong(1));
                }
            }
        }
        return session;
    }

    public List<FocusSession> findRecent(int limit) throws SQLException {
        List<FocusSession> list = new ArrayList<>();
        String sql = """
            SELECT fs.*, t.title AS task_title
            FROM focus_sessions fs
            LEFT JOIN tasks t ON fs.task_id = t.id
            ORDER BY fs.completed_at DESC
            LIMIT ?
        """;
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

    public int getTotalFocusMinutes() throws SQLException {
        String sql = "SELECT SUM(duration_minutes) FROM focus_sessions WHERE session_type = 'FOCUS'";
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getTodayFocusMinutes() throws SQLException {
        String todayPrefix = LocalDate.now().toString() + "%";
        String sql = "SELECT SUM(duration_minutes) FROM focus_sessions WHERE session_type = 'FOCUS' AND completed_at LIKE ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, todayPrefix);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Map<LocalDate, Integer> getDailyFocusMinutesForLastDays(int days) throws SQLException {
        Map<LocalDate, Integer> map = new HashMap<>();
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        String sql = """
            SELECT substr(completed_at, 1, 10) as session_date, SUM(duration_minutes) as total_mins
            FROM focus_sessions
            WHERE session_type = 'FOCUS' AND substr(completed_at, 1, 10) >= ?
            GROUP BY substr(completed_at, 1, 10)
        """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, startDate.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(rs.getString("session_date"));
                    map.put(date, rs.getInt("total_mins"));
                }
            }
        }
        return map;
    }

    private FocusSession mapResultSet(ResultSet rs) throws SQLException {
        FocusSession fs = new FocusSession();
        fs.setId(rs.getLong("id"));
        long taskId = rs.getLong("task_id");
        if (!rs.wasNull()) {
            fs.setTaskId(taskId);
        }
        fs.setTaskTitle(rs.getString("task_title"));
        fs.setDurationMinutes(rs.getInt("duration_minutes"));
        String typeStr = rs.getString("session_type");
        if (typeStr != null) {
            try {
                fs.setSessionType(FocusSession.SessionType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                fs.setSessionType(FocusSession.SessionType.FOCUS);
            }
        }
        String completedStr = rs.getString("completed_at");
        if (completedStr != null) {
            fs.setCompletedAt(LocalDateTime.parse(completedStr, FORMATTER));
        }
        fs.setNotes(rs.getString("notes"));
        return fs;
    }
}
