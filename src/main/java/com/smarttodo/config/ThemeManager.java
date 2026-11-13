package com.smarttodo.config;

import com.smarttodo.model.Theme;
import com.smarttodo.repository.SettingsRepository;
import javafx.scene.Scene;

import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class ThemeManager {
    public interface ThemeChangeListener {
        void onThemeChanged(Theme theme);
    }

    private static ThemeManager instance;
    private final SettingsRepository settingsRepository;
    private Theme currentTheme = Theme.DARK;
    private final List<ThemeChangeListener> listeners = new CopyOnWriteArrayList<>();
    private Scene primaryScene;

    public ThemeManager(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        loadTheme();
    }

    public static synchronized void initialize(SettingsRepository repo) {
        if (instance == null) {
            instance = new ThemeManager(repo);
        }
    }

    public static synchronized ThemeManager getInstance() {
        return instance;
    }

    public void setPrimaryScene(Scene scene) {
        this.primaryScene = scene;
        applyThemeToScene();
    }

    public void addListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(Theme theme) {
        if (theme == null) return;
        this.currentTheme = theme;
        try {
            settingsRepository.set("theme", theme.name());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        applyThemeToScene();
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(theme);
        }
    }

    public void toggleTheme() {
        setTheme(currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    private void loadTheme() {
        try {
            String val = settingsRepository.get("theme", "DARK");
            this.currentTheme = Theme.fromString(val);
        } catch (SQLException e) {
            this.currentTheme = Theme.DARK;
        }
    }

    private void applyThemeToScene() {
        if (primaryScene != null) {
            primaryScene.getRoot().getStyleClass().removeAll("dark-theme", "light-theme");
            primaryScene.getRoot().getStyleClass().add(currentTheme.getCssClass());
        }
    }
}
