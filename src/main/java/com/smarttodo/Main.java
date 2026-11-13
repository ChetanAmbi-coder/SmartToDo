package com.smarttodo;

import com.smarttodo.config.ThemeManager;
import com.smarttodo.database.DatabaseManager;
import com.smarttodo.notification.NotificationManager;
import com.smarttodo.repository.*;
import com.smarttodo.service.*;
import com.smarttodo.view.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private DatabaseManager databaseManager;
    private PomodoroService pomodoroService;
    private ReminderSchedulerService reminderSchedulerService;

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize persistent SQLite database
        databaseManager = DatabaseManager.getInstance();

        // 2. Initialize Repositories
        TaskRepository taskRepository = new TaskRepository(databaseManager);
        ProjectRepository projectRepository = new ProjectRepository(databaseManager);
        CategoryRepository categoryRepository = new CategoryRepository(databaseManager);
        SubtaskRepository subtaskRepository = new SubtaskRepository(databaseManager);
        FocusSessionRepository focusSessionRepository = new FocusSessionRepository(databaseManager);
        ReminderRepository reminderRepository = new ReminderRepository(databaseManager);
        MyDayRepository myDayRepository = new MyDayRepository(databaseManager);
        SettingsRepository settingsRepository = new SettingsRepository(databaseManager);

        // 3. Initialize Domain Services & Engines
        PriorityEngine priorityEngine = new PriorityEngine();
        TaskService taskService = new TaskService(
                taskRepository, subtaskRepository, reminderRepository, myDayRepository, priorityEngine
        );
        ProjectService projectService = new ProjectService(projectRepository);
        CategoryService categoryService = new CategoryService(categoryRepository);
        MyDayService myDayService = new MyDayService(myDayRepository, taskRepository);
        AnalyticsService analyticsService = new AnalyticsService(
                taskRepository, projectRepository, focusSessionRepository
        );
        pomodoroService = new PomodoroService(focusSessionRepository);
        SearchFilterService searchFilterService = new SearchFilterService(taskRepository);
        SettingsService settingsService = new SettingsService(settingsRepository);

        // 4. Initialize Theme Manager
        ThemeManager.initialize(settingsRepository);
        ThemeManager themeManager = ThemeManager.getInstance();

        // 5. Initialize Reminder & Notification System
        reminderSchedulerService = new ReminderSchedulerService(reminderRepository);
        reminderSchedulerService.setNotificationHandler(reminder -> {
            if (settingsService.isNotificationsEnabled()) {
                NotificationManager.getInstance().showNotification(
                        "Task Reminder",
                        "Scheduled reminder for: " + (reminder.getTaskTitle() != null ? reminder.getTaskTitle() : "Your task"),
                        com.smarttodo.components.ToastNotification.Type.WARNING
                );
            }
        });
        reminderSchedulerService.start();

        // 6. Build Main Layout
        MainLayout mainLayout = new MainLayout(
                primaryStage,
                databaseManager,
                taskService,
                projectService,
                categoryService,
                myDayService,
                analyticsService,
                pomodoroService,
                searchFilterService,
                settingsService,
                themeManager,
                taskRepository,
                focusSessionRepository
        );

        // 7. Setup JavaFX Scene & Stage
        Scene scene = new Scene(mainLayout, 1200, 800);
        String cssPath = getClass().getResource("/css/style.css") != null ?
                getClass().getResource("/css/style.css").toExternalForm() : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }
        themeManager.setPrimaryScene(scene);

        primaryStage.setTitle("SMART-TO-DO-LIST — Intelligent Desktop Productivity");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);

        // Clean shutdown hook
        primaryStage.setOnCloseRequest(e -> {
            if (pomodoroService != null) {
                pomodoroService.shutdown();
            }
            if (reminderSchedulerService != null) {
                reminderSchedulerService.stop();
            }
            if (databaseManager != null) {
                databaseManager.close();
            }
        });

        primaryStage.show();
    }

    @Override
    public void stop() {
        if (pomodoroService != null) {
            pomodoroService.shutdown();
        }
        if (reminderSchedulerService != null) {
            reminderSchedulerService.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
