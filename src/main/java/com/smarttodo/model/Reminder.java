package com.smarttodo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Reminder {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private LocalDateTime reminderDateTime;
    private boolean isTriggered;
    private boolean isDismissed;
    private LocalDateTime createdAt;

    public Reminder() {
        this.isTriggered = false;
        this.isDismissed = false;
        this.createdAt = LocalDateTime.now();
    }

    public Reminder(Long id, Long taskId, LocalDateTime reminderDateTime, boolean isTriggered, boolean isDismissed, LocalDateTime createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.reminderDateTime = reminderDateTime;
        this.isTriggered = isTriggered;
        this.isDismissed = isDismissed;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Reminder(Long taskId, LocalDateTime reminderDateTime) {
        this(null, taskId, reminderDateTime, false, false, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public LocalDateTime getReminderDateTime() {
        return reminderDateTime;
    }

    public void setReminderDateTime(LocalDateTime reminderDateTime) {
        this.reminderDateTime = reminderDateTime;
    }

    public boolean isTriggered() {
        return isTriggered;
    }

    public void setTriggered(boolean triggered) {
        isTriggered = triggered;
    }

    public boolean isDismissed() {
        return isDismissed;
    }

    public void setDismissed(boolean dismissed) {
        isDismissed = dismissed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reminder reminder = (Reminder) o;
        return Objects.equals(id, reminder.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
