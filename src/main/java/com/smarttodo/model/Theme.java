package com.smarttodo.model;

public enum Theme {
    DARK("Dark Navy", "dark-theme"),
    LIGHT("Clean Light", "light-theme");

    private final String displayName;
    private final String cssClass;

    Theme(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static Theme fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DARK;
        }
        try {
            return Theme.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DARK;
        }
    }
}
