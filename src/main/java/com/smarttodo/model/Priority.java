package com.smarttodo.model;

public enum Priority {
    CRITICAL("Critical", "#EF4444", 4, 1.0),
    HIGH("High", "#F97316", 3, 0.75),
    MEDIUM("Medium", "#3B82F6", 2, 0.50),
    LOW("Low", "#10B981", 1, 0.25);

    private final String displayName;
    private final String colorHex;
    private final int level;
    private final double weight;

    Priority(String displayName, String colorHex, int level, double weight) {
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.level = level;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public int getLevel() {
        return level;
    }

    public double getWeight() {
        return weight;
    }

    public static Priority fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEDIUM;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            for (Priority p : values()) {
                if (p.displayName.equalsIgnoreCase(value.trim())) {
                    return p;
                }
            }
            return MEDIUM;
        }
    }
}
