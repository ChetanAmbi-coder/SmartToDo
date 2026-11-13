package com.smarttodo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MyDayEntry {
    private Long id;
    private Long taskId;
    private LocalDate entryDate;
    private LocalDateTime addedAt;

    public MyDayEntry() {
        this.entryDate = LocalDate.now();
        this.addedAt = LocalDateTime.now();
    }

    public MyDayEntry(Long id, Long taskId, LocalDate entryDate, LocalDateTime addedAt) {
        this.id = id;
        this.taskId = taskId;
        this.entryDate = entryDate != null ? entryDate : LocalDate.now();
        this.addedAt = addedAt != null ? addedAt : LocalDateTime.now();
    }

    public MyDayEntry(Long taskId, LocalDate entryDate) {
        this(null, taskId, entryDate, LocalDateTime.now());
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

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
