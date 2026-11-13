package com.smarttodo.model;

public enum TaskStatus {
    TODO("To Do", "#94A3B8"),
    IN_PROGRESS("In Progress", "#38BDF8"),
    COMPLETED("Completed", "#10B981");

    private final String displayName;
    private final String colorHex;

    TaskStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static TaskStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return TODO;
        }
        try {
            return TaskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            for (TaskStatus s : values()) {
                if (s.displayName.equalsIgnoreCase(value.trim())) {
                    return s;
                }
            }
            return TODO;
        }
    }
}
