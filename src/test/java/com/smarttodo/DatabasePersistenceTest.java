package com.smarttodo;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Category;
import com.smarttodo.model.Project;
import com.smarttodo.model.Task;
import com.smarttodo.repository.CategoryRepository;
import com.smarttodo.repository.ProjectRepository;
import com.smarttodo.repository.SettingsRepository;
import com.smarttodo.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DatabasePersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Verify records survive database close and reopen (application restart simulation)")
    void testPersistenceAcrossRestart() throws SQLException {
        File dbFile = tempDir.resolve("restart_test.db").toFile();
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        // 1. First session: Create records
        DatabaseManager db1 = new DatabaseManager(jdbcUrl);
        TaskRepository taskRepo1 = new TaskRepository(db1);
        SettingsRepository settingsRepo1 = new SettingsRepository(db1);

        Task task = new Task("Persistent Task");
        task.setDescription("Must survive application restart.");
        taskRepo1.create(task);

        settingsRepo1.set("theme", "LIGHT");
        db1.close();

        // 2. Second session: Re-open database with new manager instance
        DatabaseManager db2 = new DatabaseManager(jdbcUrl);
        TaskRepository taskRepo2 = new TaskRepository(db2);
        SettingsRepository settingsRepo2 = new SettingsRepository(db2);

        List<Task> tasks = taskRepo2.findAll();
        assertEquals(1, tasks.size(), "Task should be preserved after restart");
        assertEquals("Persistent Task", tasks.get(0).getTitle());
        assertEquals("Must survive application restart.", tasks.get(0).getDescription());

        String savedTheme = settingsRepo2.get("theme", "DARK");
        assertEquals("LIGHT", savedTheme, "Settings must be preserved after restart");

        db2.close();
    }

    @Test
    @DisplayName("Project deletion safely reassigns tasks to null (Inbox) without deleting tasks")
    void testSafeProjectDeletion() throws SQLException {
        File dbFile = tempDir.resolve("safe_delete_test.db").toFile();
        DatabaseManager db = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath());
        ProjectRepository projRepo = new ProjectRepository(db);
        TaskRepository taskRepo = new TaskRepository(db);

        Project project = projRepo.create(new Project("Temporary Project", "desc", "#3B82F6", "folder"));
        Task task = new Task("Task in temporary project");
        task.setProjectId(project.getId());
        Task createdTask = taskRepo.create(task);

        // Verify task has project id
        assertEquals(project.getId(), taskRepo.findById(createdTask.getId()).get().getProjectId());

        // Delete project
        projRepo.delete(project.getId());

        // Verify project is deleted
        assertTrue(projRepo.findById(project.getId()).isEmpty());

        // Verify task is NOT deleted, but its project_id is now null
        Optional<Task> preservedTask = taskRepo.findById(createdTask.getId());
        assertTrue(preservedTask.isPresent(), "Task must not be deleted when its project is deleted");
        assertNull(preservedTask.get().getProjectId(), "Task project_id must safely revert to null");

        db.close();
    }

    @Test
    @DisplayName("Default categories are seeded and system categories cannot be deleted")
    void testSystemCategoryProtection() throws SQLException {
        File dbFile = tempDir.resolve("category_test.db").toFile();
        DatabaseManager db = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath());
        CategoryRepository catRepo = new CategoryRepository(db);

        List<Category> all = catRepo.findAll();
        assertFalse(all.isEmpty(), "Default categories should be seeded automatically");
        assertTrue(all.stream().anyMatch(c -> c.getName().equals("Work")));
        assertTrue(all.stream().anyMatch(c -> c.getName().equals("Personal")));

        // Attempting to delete a system category via repository will not delete it
        Category workCat = all.stream().filter(c -> c.getName().equals("Work")).findFirst().get();
        catRepo.delete(workCat.getId());

        Optional<Category> stillThere = catRepo.findById(workCat.getId());
        assertTrue(stillThere.isPresent(), "System category should not be deleted");

        db.close();
    }
}
