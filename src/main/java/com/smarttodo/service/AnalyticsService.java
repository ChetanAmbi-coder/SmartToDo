package com.smarttodo.service;

import com.smarttodo.model.Project;
import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.repository.FocusSessionRepository;
import com.smarttodo.repository.ProjectRepository;
import com.smarttodo.repository.TaskRepository;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class AnalyticsService {

    public static class AnalyticsData {
        public int activeTasks;
        public int completedTasks;
        public int pendingTasks;
        public int inProgressTasks;
        public int overdueTasks;
        public int dueTodayTasks;
        public double completionPercentage;
        public int totalFocusMinutes;
        public int todayFocusMinutes;
        public double averageDailyCompleted;
        public int dailyCompleted;
        public int weeklyCompleted;
        public int monthlyCompleted;
        public Map<LocalDate, Integer> dailyCompletionHistory = new LinkedHashMap<>();
        public Map<String, Integer> projectDistribution = new LinkedHashMap<>();
        public Map<String, Integer> categoryDistribution = new LinkedHashMap<>();
        public Map<String, Integer> priorityDistribution = new LinkedHashMap<>();
    }

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final FocusSessionRepository focusSessionRepository;

    public AnalyticsService(TaskRepository taskRepository,
                            ProjectRepository projectRepository,
                            FocusSessionRepository focusSessionRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.focusSessionRepository = focusSessionRepository;
    }

    public AnalyticsData getAnalyticsData() throws SQLException {
        AnalyticsData data = new AnalyticsData();
        LocalDate today = LocalDate.now();

        data.activeTasks = taskRepository.countActive();
        data.completedTasks = taskRepository.countCompleted();
        data.pendingTasks = taskRepository.countPending();
        data.inProgressTasks = taskRepository.countInProgress();
        data.overdueTasks = taskRepository.countOverdue(today);
        data.dueTodayTasks = taskRepository.countDueToday(today);

        int totalAll = data.activeTasks + data.completedTasks;
        data.completionPercentage = totalAll > 0 ? ((double) data.completedTasks / totalAll) * 100.0 : 0.0;

        // Daily completed
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        data.dailyCompleted = taskRepository.countCompletedInRange(startOfDay, endOfDay);

        // Weekly completed
        LocalDate startOfWeekDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        data.weeklyCompleted = taskRepository.countCompletedInRange(startOfWeekDate.atStartOfDay(), endOfDay);

        // Monthly completed
        LocalDate startOfMonthDate = today.with(TemporalAdjusters.firstDayOfMonth());
        data.monthlyCompleted = taskRepository.countCompletedInRange(startOfMonthDate.atStartOfDay(), endOfDay);

        // Focus session times
        data.totalFocusMinutes = focusSessionRepository.getTotalFocusMinutes();
        data.todayFocusMinutes = focusSessionRepository.getTodayFocusMinutes();

        // 7-day completion history
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int count = taskRepository.countCompletedInRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
            data.dailyCompletionHistory.put(d, count);
        }

        // Average daily completed over last 7 days
        double sum7 = data.dailyCompletionHistory.values().stream().mapToInt(Integer::intValue).sum();
        data.averageDailyCompleted = sum7 / 7.0;

        // Project distribution
        List<Project> projects = projectRepository.findAll();
        for (Project p : projects) {
            data.projectDistribution.put(p.getName(), p.getTotalTasks());
        }
        int inboxCount = taskRepository.findInboxTasks().size();
        if (inboxCount > 0) {
            data.projectDistribution.put("Inbox", inboxCount);
        }

        // Priority distribution of active tasks
        List<Task> activeTasks = taskRepository.findActiveTasks();
        for (Task t : activeTasks) {
            String pName = t.getPriority() != null ? t.getPriority().getDisplayName() : "Medium";
            data.priorityDistribution.put(pName, data.priorityDistribution.getOrDefault(pName, 0) + 1);

            String catName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
            data.categoryDistribution.put(catName, data.categoryDistribution.getOrDefault(catName, 0) + 1);
        }

        return data;
    }
}
