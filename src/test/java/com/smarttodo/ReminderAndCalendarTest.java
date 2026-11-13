package com.smarttodo;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Priority;
import com.smarttodo.model.Reminder;
import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.repository.ReminderRepository;
import com.smarttodo.repository.TaskRepository;
import com.smarttodo.service.ReminderSchedulerService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ReminderAndCalendarTest {
    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private ReminderRepository reminderRepository;

    @BeforeEach
    void setUp() {
        File dbFile = tempDir.resolve("test_calendar_reminders.db").toFile();
        dbManager = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath());
        taskRepository = new TaskRepository(dbManager);
        reminderRepository = new ReminderRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Calendar filtering returns exact tasks for specific date and ranges")
    void testCalendarDateFiltering() throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);

        Task tToday = new Task("Task Today");
        tToday.setDueDate(today);
        taskRepository.create(tToday);

        Task tTomorrow = new Task("Task Tomorrow");
        tTomorrow.setDueDate(tomorrow);
        taskRepository.create(tTomorrow);

        Task tNextWeek = new Task("Task Next Week");
        tNextWeek.setDueDate(nextWeek);
        taskRepository.create(tNextWeek);

        List<Task> todayTasks = taskRepository.findTodayTasks(today);
        assertTrue(todayTasks.stream().anyMatch(t -> t.getTitle().equals("Task Today")));
        assertFalse(todayTasks.stream().anyMatch(t -> t.getTitle().equals("Task Next Week")));

        List<Task> upcoming = taskRepository.findUpcomingTasks(today, today.plusDays(3));
        assertEquals(2, upcoming.size());
    }

    @Test
    @DisplayName("Reminder scheduler detects pending due reminders and ignores completed tasks")
    void testReminderSchedulerLogic() throws SQLException {
        LocalDateTime past = LocalDateTime.now().minusMinutes(5);

        // 1. Task with due reminder
        Task activeTask = new Task("Active task with reminder");
        activeTask = taskRepository.create(activeTask);

        Reminder activeReminder = new Reminder(activeTask.getId(), past);
        reminderRepository.create(activeReminder);

        // 2. Task that is already completed
        Task completedTask = new Task("Completed task with reminder");
        completedTask.setStatus(TaskStatus.COMPLETED);
        completedTask = taskRepository.create(completedTask);

        Reminder completedReminder = new Reminder(completedTask.getId(), past);
        reminderRepository.create(completedReminder);

        // Verify query only returns active task's reminder
        List<Reminder> pending = reminderRepository.findPendingReminders(LocalDateTime.now());
        assertEquals(1, pending.size());
        assertEquals(activeTask.getId(), pending.get(0).getTaskId());

        // Test notification handler trigger
        ReminderSchedulerService service = new ReminderSchedulerService(reminderRepository);
        List<Long> triggeredIds = new ArrayList<>();
        service.setNotificationHandler(r -> triggeredIds.add(r.getId()));

        service.checkReminders();

        assertEquals(1, triggeredIds.size());
        assertEquals(activeReminder.getId(), triggeredIds.get(0));

        // Subsequent check should not trigger duplicates
        triggeredIds.clear();
        service.checkReminders();
        assertEquals(0, triggeredIds.size(), "Should prevent duplicate notification triggers");

        service.stop();
    }
}
