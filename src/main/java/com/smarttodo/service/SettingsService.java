package com.smarttodo.service;

import com.smarttodo.model.Priority;
import com.smarttodo.model.Theme;
import com.smarttodo.repository.SettingsRepository;

import java.sql.SQLException;

public class SettingsService {
    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public Theme getTheme() {
        try {
            return Theme.fromString(settingsRepository.get("theme", "DARK"));
        } catch (SQLException e) {
            return Theme.DARK;
        }
    }

    public void setTheme(Theme theme) throws SQLException {
        settingsRepository.set("theme", theme.name());
    }

    public Priority getDefaultPriority() {
        try {
            return Priority.fromString(settingsRepository.get("default_priority", "MEDIUM"));
        } catch (SQLException e) {
            return Priority.MEDIUM;
        }
    }

    public void setDefaultPriority(Priority priority) throws SQLException {
        settingsRepository.set("default_priority", priority.name());
    }

    public int getPomodoroFocusMinutes() {
        try {
            return Integer.parseInt(settingsRepository.get("pomodoro_focus_minutes", "25"));
        } catch (Exception e) {
            return 25;
        }
    }

    public void setPomodoroFocusMinutes(int minutes) throws SQLException {
        settingsRepository.set("pomodoro_focus_minutes", String.valueOf(minutes));
    }

    public int getPomodoroShortBreakMinutes() {
        try {
            return Integer.parseInt(settingsRepository.get("pomodoro_short_break_minutes", "5"));
        } catch (Exception e) {
            return 5;
        }
    }

    public void setPomodoroShortBreakMinutes(int minutes) throws SQLException {
        settingsRepository.set("pomodoro_short_break_minutes", String.valueOf(minutes));
    }

    public int getPomodoroLongBreakMinutes() {
        try {
            return Integer.parseInt(settingsRepository.get("pomodoro_long_break_minutes", "15"));
        } catch (Exception e) {
            return 15;
        }
    }

    public void setPomodoroLongBreakMinutes(int minutes) throws SQLException {
        settingsRepository.set("pomodoro_long_break_minutes", String.valueOf(minutes));
    }

    public boolean isNotificationsEnabled() {
        try {
            return Boolean.parseBoolean(settingsRepository.get("notifications_enabled", "true"));
        } catch (SQLException e) {
            return true;
        }
    }

    public void setNotificationsEnabled(boolean enabled) throws SQLException {
        settingsRepository.set("notifications_enabled", String.valueOf(enabled));
    }

    public boolean isSoundEnabled() {
        try {
            return Boolean.parseBoolean(settingsRepository.get("sound_enabled", "true"));
        } catch (SQLException e) {
            return true;
        }
    }

    public void setSoundEnabled(boolean enabled) throws SQLException {
        settingsRepository.set("sound_enabled", String.valueOf(enabled));
    }
}
