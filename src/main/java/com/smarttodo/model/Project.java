package com.smarttodo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Project {
    private Long id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private LocalDateTime createdAt;
    
    // Transient statistics for UI display
    private int totalTasks;
    private int completedTasks;

    public Project() {
        this.color = "#3B82F6";
        this.icon = "folder";
        this.createdAt = LocalDateTime.now();
    }

    public Project(Long id, String name, String description, String color, String icon, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color != null ? color : "#3B82F6";
        this.icon = icon != null ? icon : "folder";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Project(String name, String description, String color, String icon) {
        this(null, name, description, color, icon, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public double getProgress() {
        if (totalTasks == 0) return 0.0;
        return (double) completedTasks / totalTasks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
