package com.smarttodo;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.*;
import com.smarttodo.repository.*;
import com.smarttodo.service.PriorityEngine;
import com.smarttodo.service.TaskService;
import com.smarttodo.util.ValidationException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TaskServiceTest {
    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private SubtaskRepository subtaskRepository;
    private ReminderRepository reminderRepository;
    private MyDayRepository myDayRepository;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        File dbFile = tempDir.resolve("test_smarttodo.db").toFile();
        dbManager = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath());
        taskRepository = new TaskRepository(dbManager);
        subtaskRepository = new SubtaskRepository(dbManager);
        reminderRepository = new ReminderRepository(dbManager);
        myDayRepository = new MyDayRepository(dbManager);
        PriorityEngine priorityEngine = new PriorityEngine();

        taskService = new TaskService(taskRepository, subtaskRepository, reminderRepository, myDayRepository, priorityEngine);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Create task persists properly in SQLite")
    void testCreateTask() throws SQLException {
        Task task = new Task("Write Project Architecture Documentation");
        task.setDescription("Detail high-level packages and database schemas.");
        task.setPriority(Priority.HIGH);
        task.setDueDate(LocalDate.now().plusDays(2));
        task.setEstimatedDuration(45);

        Task created = taskService.createTask(task);
        assertNotNull(created.getId(), "Generated ID should not be null");

        Optional<Task> retrieved = taskService.getTaskWithSubtasks(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Write Project Architecture Documentation", retrieved.get().getTitle());
        assertEquals(Priority.HIGH, retrieved.get().getPriority());
        assertEquals(TaskStatus.TODO, retrieved.get().getStatus());
    }

    @Test
    @DisplayName("Validation rejects empty title")
    void testValidationEmptyTitle() {
        Task task = new Task("");
        assertThrows(ValidationException.class, () -> taskService.createTask(task));

        Task taskNull = new Task(null);
        assertThrows(ValidationException.class, () -> taskService.createTask(taskNull));
    }

    @Test
    @DisplayName("Validation rejects due date earlier than start date")
    void testValidationDateOrder() {
        Task task = new Task("Invalid Date Task");
        task.setStartDate(LocalDate.now().plusDays(5));
        task.setDueDate(LocalDate.now().plusDays(2)); // earlier than start date

        assertThrows(ValidationException.class, () -> taskService.createTask(task));
    }

    @Test
    @DisplayName("Validation rejects reminder scheduled after task deadline")
    void testValidationReminderAfterDeadline() {
        Task task = new Task("Test Reminder Constraints");
        task.setDueDate(LocalDate.now().plusDays(2));
        task.setReminderDateTime(LocalDateTime.now().plusDays(4)); // after due date

        assertThrows(ValidationException.class, () -> taskService.createTask(task));
    }

    @Test
    @DisplayName("Task update modifies title, priority and fields")
    void testUpdateTask() throws SQLException {
        Task task = new Task("Initial Title");
        task.setPriority(Priority.LOW);
        Task created = taskService.createTask(task);

        created.setTitle("Updated Title");
        created.setPriority(Priority.CRITICAL);
        taskService.updateTask(created);

        Optional<Task> reloaded = taskService.getTaskWithSubtasks(created.getId());
        assertTrue(reloaded.isPresent());
        assertEquals("Updated Title", reloaded.get().getTitle());
        assertEquals(Priority.CRITICAL, reloaded.get().getPriority());
    }

    @Test
    @DisplayName("Task deletion removes task and subtasks from SQLite")
    void testDeleteTask() throws SQLException {
        Task task = new Task("Task to be deleted");
        Task created = taskService.createTask(task);
        long id = created.getId();

        taskService.addSubtask(id, "Subtask 1");
        taskService.addSubtask(id, "Subtask 2");

        assertEquals(1, taskService.getAllTasks().size());
        assertEquals(2, subtaskRepository.findByTaskId(id).size());

        taskService.deleteTask(id);

        assertEquals(0, taskService.getAllTasks().size());
        assertEquals(0, subtaskRepository.findByTaskId(id).size());
    }

    @Test
    @DisplayName("Completing and reopening a task changes status and completed_at")
    void testCompleteAndReopen() throws SQLException {
        Task task = new Task("Finish Homework");
        Task created = taskService.createTask(task);

        // Mark complete
        taskService.markComplete(created.getId());
        Optional<Task> completed = taskService.getTaskWithSubtasks(created.getId());
        assertTrue(completed.isPresent());
        assertEquals(TaskStatus.COMPLETED, completed.get().getStatus());
        assertNotNull(completed.get().getCompletedAt());

        // Reopen
        taskService.reopenTask(created.getId());
        Optional<Task> reopened = taskService.getTaskWithSubtasks(created.getId());
        assertTrue(reopened.isPresent());
        assertEquals(TaskStatus.TODO, reopened.get().getStatus());
        assertNull(reopened.get().getCompletedAt());
    }

    @Test
    @DisplayName("Recurring daily task generates next occurrence when completed")
    void testRecurringTaskCompletion() throws SQLException {
        Task recurring = new Task("Daily Exercise");
        recurring.setRepeatFrequency(RepeatFrequency.DAILY);
        LocalDate initialDue = LocalDate.now();
        recurring.setDueDate(initialDue);

        Task created = taskService.createTask(recurring);
        taskService.markComplete(created.getId());

        List<Task> all = taskService.getAllTasks();
        assertEquals(2, all.size(), "Should have completed task and generated next recurring task");

        // Find the active new one
        Task nextOccurrence = all.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .findFirst()
                .orElse(null);

        assertNotNull(nextOccurrence);
        assertEquals("Daily Exercise", nextOccurrence.getTitle());
        assertEquals(initialDue.plusDays(1), nextOccurrence.getDueDate());
    }
}
