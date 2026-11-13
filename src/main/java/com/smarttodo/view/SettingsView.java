package com.smarttodo.view;

import com.smarttodo.config.ThemeManager;
import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Priority;
import com.smarttodo.model.Project;
import com.smarttodo.model.Task;
import com.smarttodo.model.Theme;
import com.smarttodo.notification.NotificationManager;
import com.smarttodo.service.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class SettingsView extends ScrollPane {
    private final SettingsService settingsService;
    private final ThemeManager themeManager;
    private final PomodoroService pomodoroService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final DatabaseManager databaseManager;

    private final ToggleGroup themeGroup = new ToggleGroup();
    private final RadioButton darkRadio = new RadioButton("Dark Navy (Modern Slate)");
    private final RadioButton lightRadio = new RadioButton("Clean Light (Minimalist)");

    private final ComboBox<Priority> priorityCombo = new ComboBox<>();
    private final Spinner<Integer> focusMinutesSpinner = new Spinner<>(5, 120, 25, 5);
    private final Spinner<Integer> shortBreakSpinner = new Spinner<>(1, 30, 5, 1);
    private final Spinner<Integer> longBreakSpinner = new Spinner<>(5, 60, 15, 5);

    private final CheckBox notificationsCheck = new CheckBox("Enable local desktop reminder notifications");
    private final CheckBox soundCheck = new CheckBox("Enable notification sound cues");

    public SettingsView(SettingsService settingsService,
                        ThemeManager themeManager,
                        PomodoroService pomodoroService,
                        TaskService taskService,
                        ProjectService projectService,
                        DatabaseManager databaseManager) {
        this.settingsService = settingsService;
        this.themeManager = themeManager;
        this.pomodoroService = pomodoroService;
        this.taskService = taskService;
        this.projectService = projectService;
        this.databaseManager = databaseManager;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));
        content.setMaxWidth(700);

        // Header
        Label headerTitle = new Label("Preferences & Settings");
        headerTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        Label headerSubtitle = new Label("Customize visual theme, timer intervals, notification preferences, and storage.");
        headerSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");
        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);

        // Section 1: Appearance
        VBox themeCard = new VBox(12);
        themeCard.getStyleClass().add("card");
        Label themeTitle = new Label("🎨 Appearance & Theme");
        themeTitle.getStyleClass().add("card-title");

        darkRadio.setToggleGroup(themeGroup);
        lightRadio.setToggleGroup(themeGroup);
        if (themeManager.getCurrentTheme() == Theme.DARK) {
            darkRadio.setSelected(true);
        } else {
            lightRadio.setSelected(true);
        }

        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == darkRadio) {
                themeManager.setTheme(Theme.DARK);
            } else if (newVal == lightRadio) {
                themeManager.setTheme(Theme.LIGHT);
            }
        });

        HBox radioBox = new HBox(20, darkRadio, lightRadio);
        themeCard.getChildren().addAll(themeTitle, radioBox);

        // Section 2: Task Defaults
        VBox defaultsCard = new VBox(12);
        defaultsCard.getStyleClass().add("card");
        Label defTitle = new Label("⚡ Task Defaults");
        defTitle.getStyleClass().add("card-title");

        priorityCombo.setItems(FXCollections.observableArrayList(Priority.values()));
        priorityCombo.setValue(settingsService.getDefaultPriority());
        priorityCombo.setOnAction(e -> {
            try {
                settingsService.setDefaultPriority(priorityCombo.getValue());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        HBox prioRow = new HBox(12, new Label("Default Task Priority:"), priorityCombo);
        prioRow.setAlignment(Pos.CENTER_LEFT);
        defaultsCard.getChildren().addAll(defTitle, prioRow);

        // Section 3: Pomodoro Timer Settings
        VBox pomoCard = new VBox(12);
        pomoCard.getStyleClass().add("card");
        Label pomoTitle = new Label("⏱ Pomodoro Focus Intervals");
        pomoTitle.getStyleClass().add("card-title");

        focusMinutesSpinner.getValueFactory().setValue(settingsService.getPomodoroFocusMinutes());
        shortBreakSpinner.getValueFactory().setValue(settingsService.getPomodoroShortBreakMinutes());
        longBreakSpinner.getValueFactory().setValue(settingsService.getPomodoroLongBreakMinutes());

        focusMinutesSpinner.valueProperty().addListener((obs, oldVal, newVal) -> savePomoSettings());
        shortBreakSpinner.valueProperty().addListener((obs, oldVal, newVal) -> savePomoSettings());
        longBreakSpinner.valueProperty().addListener((obs, oldVal, newVal) -> savePomoSettings());

        GridPane pomoGrid = new GridPane();
        pomoGrid.setHgap(16);
        pomoGrid.setVgap(10);
        pomoGrid.add(new Label("Focus Duration:"), 0, 0);
        pomoGrid.add(focusMinutesSpinner, 1, 0);
        pomoGrid.add(new Label("minutes"), 2, 0);

        pomoGrid.add(new Label("Short Break:"), 0, 1);
        pomoGrid.add(shortBreakSpinner, 1, 1);
        pomoGrid.add(new Label("minutes"), 2, 1);

        pomoGrid.add(new Label("Long Break:"), 0, 2);
        pomoGrid.add(longBreakSpinner, 1, 2);
        pomoGrid.add(new Label("minutes"), 2, 2);

        pomoCard.getChildren().addAll(pomoTitle, pomoGrid);

        // Section 4: Notifications
        VBox notifCard = new VBox(12);
        notifCard.getStyleClass().add("card");
        Label notifTitle = new Label("🔔 Notifications & Reminders");
        notifTitle.getStyleClass().add("card-title");

        notificationsCheck.setSelected(settingsService.isNotificationsEnabled());
        notificationsCheck.setOnAction(e -> {
            try {
                settingsService.setNotificationsEnabled(notificationsCheck.isSelected());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        soundCheck.setSelected(settingsService.isSoundEnabled());
        soundCheck.setOnAction(e -> {
            try {
                settingsService.setSoundEnabled(soundCheck.isSelected());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        notifCard.getChildren().addAll(notifTitle, notificationsCheck, soundCheck);

        // Section 5: Database & Sample Data
        VBox storageCard = new VBox(12);
        storageCard.getStyleClass().add("card");
        Label storageTitle = new Label("💾 Persistent Storage");
        storageTitle.getStyleClass().add("card-title");

        Label dbPathLabel = new Label("Database location: " + databaseManager.getDbUrl());
        dbPathLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-family: monospace; -fx-font-size: 11px;");

        HBox sampleBox = new HBox(12);
        sampleBox.setAlignment(Pos.CENTER_LEFT);
        Button seedBtn = new Button("Load Sample Data");
        seedBtn.getStyleClass().add("btn-secondary");
        seedBtn.setOnAction(e -> seedSampleData());

        Label seedNote = new Label("Populates realistic projects, categories, and tasks for demonstration.");
        seedNote.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px;");
        sampleBox.getChildren().addAll(seedBtn, seedNote);

        storageCard.getChildren().addAll(storageTitle, dbPathLabel, sampleBox);

        content.getChildren().addAll(headerBox, themeCard, defaultsCard, pomoCard, notifCard, storageCard);
        setContent(content);
    }

    private void savePomoSettings() {
        try {
            int focus = focusMinutesSpinner.getValue();
            int sBreak = shortBreakSpinner.getValue();
            int lBreak = longBreakSpinner.getValue();
            settingsService.setPomodoroFocusMinutes(focus);
            settingsService.setPomodoroShortBreakMinutes(sBreak);
            settingsService.setPomodoroLongBreakMinutes(lBreak);
            pomodoroService.setDurations(focus, sBreak, lBreak);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void seedSampleData() {
        try {
            // Create sample projects if needed
            Project proj1 = projectService.createProject(new Project("Product Launch Q1", "Deliver core features for v1.0 release", "#3B82F6", "rocket"));
            Project proj2 = projectService.createProject(new Project("Health & Fitness", "Daily workouts and marathon preparation", "#10B981", "heart"));

            LocalDate today = LocalDate.now();

            // Task 1: Overdue
            Task t1 = new Task("Finalize API specification & database migrations");
            t1.setDescription("Review SQLite foreign keys, indexes, and write unit tests.");
            t1.setPriority(Priority.CRITICAL);
            t1.setImportant(true);
            t1.setDueDate(today.minusDays(1));
            t1.setDueTime(LocalTime.of(16, 0));
            t1.setProjectId(proj1.getId());
            t1.setEstimatedDuration(60);
            taskService.createTask(t1);

            // Task 2: Due Today
            Task t2 = new Task("Sprint planning meeting with architecture team");
            t2.setDescription("Align on priority recommendation algorithm and UI layout.");
            t2.setPriority(Priority.HIGH);
            t2.setImportant(true);
            t2.setDueDate(today);
            t2.setDueTime(LocalTime.of(15, 30));
            t2.setInMyDay(true);
            t2.setProjectId(proj1.getId());
            t2.setEstimatedDuration(45);
            taskService.createTask(t2);

            // Task 3: Upcoming
            Task t3 = new Task("5km Morning tempo run");
            t3.setPriority(Priority.MEDIUM);
            t3.setDueDate(today.plusDays(1));
            t3.setDueTime(LocalTime.of(7, 0));
            t3.setProjectId(proj2.getId());
            t3.setEstimatedDuration(30);
            taskService.createTask(t3);

            // Task 4: Another task
            Task t4 = new Task("Review customer feedback and survey results");
            t4.setPriority(Priority.LOW);
            t4.setDueDate(today.plusDays(3));
            t4.setEstimatedDuration(45);
            taskService.createTask(t4);

            taskService.notifyChange();

            NotificationManager.getInstance().showNotification(
                    "Sample Data Added",
                    "Realistic projects and tasks have been added to your database.",
                    com.smarttodo.components.ToastNotification.Type.SUCCESS
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
