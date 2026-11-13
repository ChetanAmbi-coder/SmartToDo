package com.smarttodo.service;

import com.smarttodo.model.*;
import com.smarttodo.repository.MyDayRepository;
import com.smarttodo.repository.ReminderRepository;
import com.smarttodo.repository.SubtaskRepository;
import com.smarttodo.repository.TaskRepository;
import com.smarttodo.util.ValidationException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskService {
    public interface TaskChangeListener {
        void onTasksChanged();
    }

    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final ReminderRepository reminderRepository;
    private final MyDayRepository myDayRepository;
    private final PriorityEngine priorityEngine;

    private final List<TaskChangeListener> changeListeners = new CopyOnWriteArrayList<>();

    public TaskService(TaskRepository taskRepository,
                       SubtaskRepository subtaskRepository,
                       ReminderRepository reminderRepository,
                       MyDayRepository myDayRepository,
                       PriorityEngine priorityEngine) {
        this.taskRepository = taskRepository;
        this.subtaskRepository = subtaskRepository;
        this.reminderRepository = reminderRepository;
        this.myDayRepository = myDayRepository;
        this.priorityEngine = priorityEngine;
    }

    public void addChangeListener(TaskChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(TaskChangeListener listener) {
        changeListeners.remove(listener);
    }

    public void notifyChange() {
        for (TaskChangeListener listener : changeListeners) {
            try {
                listener.onTasksChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void validateTask(Task task) {
        if (task == null) {
            throw new ValidationException("Task cannot be null.");
        }
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new ValidationException("Task title is required.");
        }
        if (task.getTitle().trim().length() > 255) {
            throw new ValidationException("Task title cannot exceed 255 characters.");
        }

        // Validate date order
        if (task.getStartDate() != null && task.getDueDate() != null) {
            if (task.getDueDate().isBefore(task.getStartDate())) {
                throw new ValidationException("Due date cannot be earlier than start date.");
            }
        }

        // Validate reminder
        if (task.getReminderDateTime() != null) {
            if (task.getDueDate() != null && task.getReminderDateTime().toLocalDate().isAfter(task.getDueDate())) {
                throw new ValidationException("Reminder cannot be scheduled after the task due date.");
            }
        }

        if (task.getEstimatedDuration() < 0) {
            throw new ValidationException("Estimated duration cannot be negative.");
        }
    }

    public Task createTask(Task task) throws SQLException {
        validateTask(task);
        Task created = taskRepository.create(task);

        // Save subtasks if present
        if (task.getSubtasks() != null) {
            for (Subtask s : task.getSubtasks()) {
                s.setTaskId(created.getId());
                subtaskRepository.create(s);
            }
        }

        // Save reminder if present
        if (task.getReminderDateTime() != null) {
            Reminder reminder = new Reminder(created.getId(), task.getReminderDateTime());
            reminderRepository.create(reminder);
        }

        // If inMyDay flag is checked
        if (task.isInMyDay()) {
            myDayRepository.addEntry(created.getId(), LocalDate.now());
        }

        notifyChange();
        return created;
    }

    public void updateTask(Task task) throws SQLException {
        validateTask(task);
        taskRepository.update(task);

        // Update reminder if needed
        reminderRepository.deleteByTaskId(task.getId());
        if (task.getReminderDateTime() != null && task.getStatus() != TaskStatus.COMPLETED) {
            Reminder reminder = new Reminder(task.getId(), task.getReminderDateTime());
            reminderRepository.create(reminder);
        }

        notifyChange();
    }

    public void deleteTask(long taskId) throws SQLException {
        reminderRepository.deleteByTaskId(taskId);
        subtaskRepository.deleteByTaskId(taskId);
        myDayRepository.removeEntry(taskId, LocalDate.now());
        taskRepository.delete(taskId);
        notifyChange();
    }

    public void markComplete(long taskId) throws SQLException {
        Optional<Task> opt = taskRepository.findById(taskId);
        if (opt.isPresent()) {
            Task task = opt.get();
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.update(task);

            // Handle recurring task logic
            if (task.getRepeatFrequency() != null && task.getRepeatFrequency() != RepeatFrequency.NONE) {
                createNextOccurrence(task);
            }

            notifyChange();
        }
    }

    public void reopenTask(long taskId) throws SQLException {
        Optional<Task> opt = taskRepository.findById(taskId);
        if (opt.isPresent()) {
            Task task = opt.get();
            task.setStatus(TaskStatus.TODO);
            task.setCompletedAt(null);
            taskRepository.update(task);
            notifyChange();
        }
    }

    public void toggleImportant(long taskId) throws SQLException {
        Optional<Task> opt = taskRepository.findById(taskId);
        if (opt.isPresent()) {
            Task task = opt.get();
            task.setImportant(!task.isImportant());
            taskRepository.update(task);
            notifyChange();
        }
    }

    public void toggleMyDay(long taskId, LocalDate date) throws SQLException {
        boolean isInDay = myDayRepository.isTaskInMyDay(taskId, date);
        if (isInDay) {
            myDayRepository.removeEntry(taskId, date);
        } else {
            myDayRepository.addEntry(taskId, date);
        }
        notifyChange();
    }

    public Optional<Task> getTaskWithSubtasks(long id) throws SQLException {
        Optional<Task> opt = taskRepository.findById(id);
        if (opt.isPresent()) {
            Task task = opt.get();
            List<Subtask> subtasks = subtaskRepository.findByTaskId(id);
            task.setSubtasks(subtasks);
            task.setInMyDay(myDayRepository.isTaskInMyDay(id, LocalDate.now()));
            return Optional.of(task);
        }
        return Optional.empty();
    }

    public List<Task> getAllTasks() throws SQLException {
        return taskRepository.findAll();
    }

    public List<Task> getActiveTasks() throws SQLException {
        return taskRepository.findActiveTasks();
    }

    public List<Task> getTodayTasks() throws SQLException {
        return taskRepository.findTodayTasks(LocalDate.now());
    }

    public List<Task> getUpcomingTasks() throws SQLException {
        LocalDate today = LocalDate.now();
        return taskRepository.findUpcomingTasks(today, today.plusDays(7));
    }

    public List<Task> getImportantTasks() throws SQLException {
        return taskRepository.findImportant();
    }

    public List<Task> getOverdueTasks() throws SQLException {
        return taskRepository.findOverdueTasks(LocalDate.now());
    }

    public List<Task> getInboxTasks() throws SQLException {
        return taskRepository.findInboxTasks();
    }

    public List<Task> getCompletedTasks() throws SQLException {
        return taskRepository.findByStatus(TaskStatus.COMPLETED);
    }

    public List<Task> getRecentCompleted(int limit) throws SQLException {
        return taskRepository.findRecentCompleted(limit);
    }

    public List<Task> getRecommendedTasks(int limit) throws SQLException {
        List<Task> active = taskRepository.findActiveTasks();
        return priorityEngine.getRecommendations(active, limit);
    }

    public Subtask addSubtask(long taskId, String title) throws SQLException {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Subtask title cannot be empty.");
        }
        Subtask subtask = new Subtask(taskId, title.trim());
        Subtask created = subtaskRepository.create(subtask);
        notifyChange();
        return created;
    }

    public void toggleSubtask(long subtaskId, boolean completed) throws SQLException {
        Subtask s = new Subtask();
        s.setId(subtaskId);
        s.setCompleted(completed);
        subtaskRepository.update(s);
        notifyChange();
    }

    public void deleteSubtask(long subtaskId) throws SQLException {
        subtaskRepository.delete(subtaskId);
        notifyChange();
    }

    private void createNextOccurrence(Task original) throws SQLException {
        Task next = new Task();
        next.setTitle(original.getTitle());
        next.setDescription(original.getDescription());
        next.setPriority(original.getPriority());
        next.setStatus(TaskStatus.TODO);
        next.setImportant(original.isImportant());
        next.setProjectId(original.getProjectId());
        next.setCategoryId(original.getCategoryId());
        next.setEstimatedDuration(original.getEstimatedDuration());
        next.setRepeatFrequency(original.getRepeatFrequency());
        next.setNotes(original.getNotes());

        LocalDate originalDue = original.getDueDate() != null ? original.getDueDate() : LocalDate.now();
        LocalDate nextDue = switch (original.getRepeatFrequency()) {
            case DAILY -> originalDue.plusDays(1);
            case WEEKLY -> originalDue.plusWeeks(1);
            case MONTHLY -> originalDue.plusMonths(1);
            default -> originalDue;
        };

        next.setDueDate(nextDue);
        next.setDueTime(original.getDueTime());
        if (original.getStartDate() != null) {
            long daysDiff = original.getDueDate() != null ?
                    java.time.temporal.ChronoUnit.DAYS.between(original.getStartDate(), original.getDueDate()) : 0;
            next.setStartDate(nextDue.minusDays(Math.max(0, daysDiff)));
        }

        taskRepository.create(next);
    }
}
