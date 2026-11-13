package com.smarttodo.repository;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Project;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;

    public ProjectRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Project create(Project project) throws SQLException {
        String sql = "INSERT INTO projects (name, description, color, icon, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, project.getName());
            stmt.setString(2, project.getDescription());
            stmt.setString(3, project.getColor());
            stmt.setString(4, project.getIcon());
            stmt.setString(5, (project.getCreatedAt() != null ? project.getCreatedAt() : LocalDateTime.now()).format(FORMATTER));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    project.setId(rs.getLong(1));
                }
            }
        }
        return project;
    }

    public void update(Project project) throws SQLException {
        String sql = "UPDATE projects SET name = ?, description = ?, color = ?, icon = ? WHERE id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, project.getName());
            stmt.setString(2, project.getDescription());
            stmt.setString(3, project.getColor());
            stmt.setString(4, project.getIcon());
            stmt.setLong(5, project.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        Connection conn = databaseManager.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            // Reassign any associated tasks to null (Inbox) before deleting project
            String reassignSql = "UPDATE tasks SET project_id = NULL WHERE project_id = ?";
            try (PreparedStatement reassignStmt = conn.prepareStatement(reassignSql)) {
                reassignStmt.setLong(1, id);
                reassignStmt.executeUpdate();
            }

            String deleteSql = "DELETE FROM projects WHERE id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setLong(1, id);
                deleteStmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    public Optional<Project> findById(long id) throws SQLException {
        String sql = """
            SELECT p.*,
                   (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.id) AS total_tasks,
                   (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.id AND t.status = 'COMPLETED') AS completed_tasks
            FROM projects p
            WHERE p.id = ?
        """;
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

    public List<Project> findAll() throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = """
            SELECT p.*,
                   (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.id) AS total_tasks,
                   (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.id AND t.status = 'COMPLETED') AS completed_tasks
            FROM projects p
            ORDER BY p.name ASC
        """;
        Connection conn = databaseManager.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private Project mapResultSet(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setColor(rs.getString("color"));
        p.setIcon(rs.getString("icon"));
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            p.setCreatedAt(LocalDateTime.parse(createdAtStr, FORMATTER));
        }
        p.setTotalTasks(rs.getInt("total_tasks"));
        p.setCompletedTasks(rs.getInt("completed_tasks"));
        return p;
    }
}
