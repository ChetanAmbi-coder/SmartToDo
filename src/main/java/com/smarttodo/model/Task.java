package com.smarttodo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Task {
    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private boolean isImportant;
    private Long projectId;
    private Long categoryId;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private int estimatedDuration; // in minutes
    private LocalDateTime reminderDateTime;
    private RepeatFrequency repeatFrequency;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // Associated entities (can be loaded eagerly or lazily)
    private List<Subtask> subtasks = new ArrayList<>();
    private Project project;
    private Category category;
    private boolean inMyDay;

    // Transient fields for Smart Priority Engine
    private double recommendationScore;
    private String recommendationReason;

    public Task() {
        this.priority = Priority.MEDIUM;
        this.status = TaskStatus.TODO;
        this.isImportant = false;
        this.repeatFrequency = RepeatFrequency.NONE;
        this.estimatedDuration = 30;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Task(String title) {
        this();
        this.title = title;
    }

    public boolean isOverdue() {
        if (status == TaskStatus.COMPLETED || dueDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return true;
        }
        if (dueDate.isEqual(today) && dueTime != null) {
            return LocalTime.now().isAfter(dueTime);
        }
        return false;
    }

    public boolean isDueToday() {
        if (dueDate == null) {
            return false;
        }
        return dueDate.isEqual(LocalDate.now());
    }

    public boolean isDueUpcoming() {
        if (dueDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);
        return !dueDate.isBefore(today) && !dueDate.isAfter(sevenDaysLater);
    }

    public double getSubtaskProgress() {
        if (subtasks == null || subtasks.isEmpty()) {
            return 0.0;
        }
        long completed = subtasks.stream().filter(Subtask::isCompleted).count();
        return (double) completed / subtasks.size();
    }

    public int getCompletedSubtasksCount() {
        if (subtasks == null) return 0;
        return (int) subtasks.stream().filter(Subtask::isCompleted).count();
    }

    public int getTotalSubtasksCount() {
        return subtasks == null ? 0 : subtasks.size();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority != null ? priority : Priority.MEDIUM;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status != null ? status : TaskStatus.TODO;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    public int getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public LocalDateTime getReminderDateTime() {
        return reminderDateTime;
    }

    public void setReminderDateTime(LocalDateTime reminderDateTime) {
        this.reminderDateTime = reminderDateTime;
    }

    public RepeatFrequency getRepeatFrequency() {
        return repeatFrequency;
    }

    public void setRepeatFrequency(RepeatFrequency repeatFrequency) {
        this.repeatFrequency = repeatFrequency != null ? repeatFrequency : RepeatFrequency.NONE;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks != null ? subtasks : new ArrayList<>();
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            this.projectId = project.getId();
        }
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        if (category != null) {
            this.categoryId = category.getId();
        }
    }

    public boolean isInMyDay() {
        return inMyDay;
    }

    public void setInMyDay(boolean inMyDay) {
        this.inMyDay = inMyDay;
    }

    public double getRecommendationScore() {
        return recommendationScore;
    }

    public void setRecommendationScore(double recommendationScore) {
        this.recommendationScore = recommendationScore;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", dueDate=" + dueDate +
                '}';
    }
}
