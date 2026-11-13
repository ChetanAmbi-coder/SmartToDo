package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SettingsRepository {
    private final DatabaseManager databaseManager;

    public SettingsRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public String get(String key, String defaultValue) throws SQLException {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        }
        return defaultValue;
    }

    public void set(String key, String value) throws SQLException {
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                     "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        }
    }

    public Map<String, String> getAll() throws SQLException {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM app_settings";
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        return map;
    }
}
