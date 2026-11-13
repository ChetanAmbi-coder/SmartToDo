package com.smarttodo.model;

import java.util.Objects;

public class Category {
    private Long id;
    private String name;
    private String color;
    private String icon;
    private boolean isSystem;

    public Category() {
        this.color = "#64748B";
        this.icon = "tag";
        this.isSystem = false;
    }

    public Category(Long id, String name, String color, String icon, boolean isSystem) {
        this.id = id;
        this.name = name;
        this.color = color != null ? color : "#64748B";
        this.icon = icon != null ? icon : "tag";
        this.isSystem = isSystem;
    }

    public Category(String name, String color, String icon, boolean isSystem) {
        this(null, name, color, icon, isSystem);
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

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
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
