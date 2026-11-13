package com.smarttodo.model;

public enum RepeatFrequency {
    NONE("Does not repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String displayName;

    RepeatFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RepeatFrequency fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }
        try {
            return RepeatFrequency.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            for (RepeatFrequency f : values()) {
                if (f.displayName.equalsIgnoreCase(value.trim())) {
                    return f;
                }
            }
            return NONE;
        }
    }
}
