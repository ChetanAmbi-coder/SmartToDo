package com.smarttodo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class FocusSession {
    public enum SessionType {
        FOCUS("Focus Session", "#3B82F6"),
        SHORT_BREAK("Short Break", "#10B981"),
        LONG_BREAK("Long Break", "#8B5CF6");

        private final String displayName;
        private final String colorHex;

        SessionType(String displayName, String colorHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColorHex() {
            return colorHex;
        }
    }

    private Long id;
    private Long taskId;
    private String taskTitle;
    private int durationMinutes;
    private SessionType sessionType;
    private LocalDateTime completedAt;
    private String notes;

    public FocusSession() {
        this.durationMinutes = 25;
        this.sessionType = SessionType.FOCUS;
        this.completedAt = LocalDateTime.now();
    }

    public FocusSession(Long id, Long taskId, int durationMinutes, SessionType sessionType, LocalDateTime completedAt, String notes) {
        this.id = id;
        this.taskId = taskId;
        this.durationMinutes = durationMinutes;
        this.sessionType = sessionType != null ? sessionType : SessionType.FOCUS;
        this.completedAt = completedAt != null ? completedAt : LocalDateTime.now();
        this.notes = notes;
    }

    public FocusSession(Long taskId, int durationMinutes, SessionType sessionType) {
        this(null, taskId, durationMinutes, sessionType, LocalDateTime.now(), null);
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FocusSession that = (FocusSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
