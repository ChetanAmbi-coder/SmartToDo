package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepository {
    private final DatabaseManager databaseManager;

    public CategoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Category create(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, color, icon, is_system) VALUES (?, ?, ?, ?)";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getColor());
            stmt.setString(3, category.getIcon());
            stmt.setInt(4, category.isSystem() ? 1 : 0);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getLong(1));
                }
            }
        }
        return category;
    }

    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, color = ?, icon = ?, is_system = ? WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getColor());
            stmt.setString(3, category.getIcon());
            stmt.setInt(4, category.isSystem() ? 1 : 0);
            stmt.setLong(5, category.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ? AND is_system = 0";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public Optional<Category> findById(long id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ?";
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

    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY is_system DESC, name ASC";
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private Category mapResultSet(ResultSet rs) throws SQLException {
        return new Category(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("color"),
                rs.getString("icon"),
                rs.getInt("is_system") == 1
        );
    }
}
