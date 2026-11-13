package com.smarttodo;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.FocusSession;
import com.smarttodo.model.Priority;
import com.smarttodo.model.Project;
import com.smarttodo.model.Task;
import com.smarttodo.repository.FocusSessionRepository;
import com.smarttodo.repository.ProjectRepository;
import com.smarttodo.repository.TaskRepository;
import com.smarttodo.service.AnalyticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AnalyticsServiceTest {
    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private FocusSessionRepository focusSessionRepository;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        File dbFile = tempDir.resolve("test_analytics.db").toFile();
        dbManager = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath());
        taskRepository = new TaskRepository(dbManager);
        projectRepository = new ProjectRepository(dbManager);
        focusSessionRepository = new FocusSessionRepository(dbManager);
        analyticsService = new AnalyticsService(taskRepository, projectRepository, focusSessionRepository);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Empty database handles all calculations safely without division by zero")
    void testEmptyAnalyticsSafe() throws SQLException {
        AnalyticsService.AnalyticsData data = analyticsService.getAnalyticsData();
        assertNotNull(data);
        assertEquals(0, data.activeTasks);
        assertEquals(0, data.completedTasks);
        assertEquals(0.0, data.completionPercentage, "Empty completion percentage should be 0.0");
        assertEquals(0, data.totalFocusMinutes);
        assertEquals(0.0, data.averageDailyCompleted);
        assertEquals(0, data.overdueTasks);
    }

    @Test
    @DisplayName("Analytics calculates metrics accurately from SQLite records")
    void testCalculationsWithData() throws SQLException {
        LocalDate today = LocalDate.now();

        // 1. Create a project
        Project project = projectRepository.create(new Project("Core System", "Desc", "#3B82F6", "code"));

        // 2. Completed task
        Task t1 = new Task("Task 1 Done");
        t1.setProjectId(project.getId());
        t1.setStatus(com.smarttodo.model.TaskStatus.COMPLETED);
        t1.setCompletedAt(LocalDateTime.now());
        taskRepository.create(t1);

        // 3. Active task due today
        Task t2 = new Task("Task 2 Today");
        t2.setProjectId(project.getId());
        t2.setDueDate(today);
        t2.setStatus(com.smarttodo.model.TaskStatus.TODO);
        taskRepository.create(t2);

        // 4. Overdue active task
        Task t3 = new Task("Task 3 Overdue");
        t3.setDueDate(today.minusDays(2));
        t3.setStatus(com.smarttodo.model.TaskStatus.TODO);
        taskRepository.create(t3);

        // 5. Add a completed focus session
        FocusSession session = new FocusSession(t2.getId(), 25, FocusSession.SessionType.FOCUS);
        session.setCompletedAt(LocalDateTime.now());
        focusSessionRepository.create(session);

        AnalyticsService.AnalyticsData data = analyticsService.getAnalyticsData();

        assertEquals(2, data.activeTasks);
        assertEquals(1, data.completedTasks);
        assertEquals(1, data.dueTodayTasks);
        assertEquals(1, data.overdueTasks);

        // Total tasks = 3, completed = 1 -> 33.33%
        assertEquals(33.33, data.completionPercentage, 0.5);

        // Focus session
        assertEquals(25, data.totalFocusMinutes);
        assertEquals(25, data.todayFocusMinutes);

        // Project distribution
        assertTrue(data.projectDistribution.containsKey("Core System"));
    }
}
