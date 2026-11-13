package com.smarttodo.service;

import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.repository.MyDayRepository;
import com.smarttodo.repository.TaskRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyDayService {
    public static class MyDaySummary {
        private final int totalTasks;
        private final int completedTasks;
        private final int remainingTasks;
        private final int totalWorkloadMinutes;
        private final double progress;

        public MyDaySummary(int totalTasks, int completedTasks, int remainingTasks, int totalWorkloadMinutes, double progress) {
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.remainingTasks = remainingTasks;
            this.totalWorkloadMinutes = totalWorkloadMinutes;
            this.progress = progress;
        }

        public int getTotalTasks() {
            return totalTasks;
        }

        public int getCompletedTasks() {
            return completedTasks;
        }

        public int getRemainingTasks() {
            return remainingTasks;
        }

        public int getTotalWorkloadMinutes() {
            return totalWorkloadMinutes;
        }

        public double getProgress() {
            return progress;
        }
    }

    private final MyDayRepository myDayRepository;
    private final TaskRepository taskRepository;

    public MyDayService(MyDayRepository myDayRepository, TaskRepository taskRepository) {
        this.myDayRepository = myDayRepository;
        this.taskRepository = taskRepository;
    }

    public List<Task> getMyDayTasks(LocalDate date) throws SQLException {
        Set<Long> taskIds = myDayRepository.getTaskIdsForDate(date);
        List<Task> todayTasks = taskRepository.findTodayTasks(date);

        // Merge today tasks and tasks explicitly added to My Day
        List<Task> result = new ArrayList<>(todayTasks);
        for (Long id : taskIds) {
            boolean alreadyIn = result.stream().anyMatch(t -> t.getId().equals(id));
            if (!alreadyIn) {
                taskRepository.findById(id).ifPresent(result::add);
            }
        }
        for (Task t : result) {
            t.setInMyDay(true);
        }
        return result;
    }

    public void addToMyDay(long taskId, LocalDate date) throws SQLException {
        myDayRepository.addEntry(taskId, date);
    }

    public void removeFromMyDay(long taskId, LocalDate date) throws SQLException {
        myDayRepository.removeEntry(taskId, date);
    }

    public MyDaySummary calculateSummary(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new MyDaySummary(0, 0, 0, 0, 0.0);
        }

        int total = tasks.size();
        int completed = 0;
        int workload = 0;

        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.COMPLETED) {
                completed++;
            } else {
                workload += Math.max(0, t.getEstimatedDuration());
            }
        }

        int remaining = total - completed;
        double progress = total > 0 ? (double) completed / total : 0.0;

        return new MyDaySummary(total, completed, remaining, workload, progress);
    }
}
