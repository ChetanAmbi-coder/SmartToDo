package com.smarttodo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Subtask {
    private Long id;
    private Long taskId;
    private String title;
    private boolean isCompleted;
    private LocalDateTime createdAt;

    public Subtask() {
        this.isCompleted = false;
        this.createdAt = LocalDateTime.now();
    }

    public Subtask(Long id, Long taskId, String title, boolean isCompleted, LocalDateTime createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.title = title;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Subtask(Long taskId, String title) {
        this(null, taskId, title, false, LocalDateTime.now());
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
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
        Subtask subtask = (Subtask) o;
        return Objects.equals(id, subtask.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title;
    }
}
