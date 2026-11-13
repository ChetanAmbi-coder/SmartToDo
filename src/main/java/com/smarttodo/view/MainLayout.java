package com.smarttodo.view;

import com.smarttodo.config.ThemeManager;
import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.Task;
import com.smarttodo.notification.NotificationManager;
import com.smarttodo.repository.FocusSessionRepository;
import com.smarttodo.repository.TaskRepository;
import com.smarttodo.service.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class MainLayout extends StackPane {
    private final Stage primaryStage;
    private final DatabaseManager databaseManager;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final CategoryService categoryService;
    private final MyDayService myDayService;
    private final AnalyticsService analyticsService;
    private final PomodoroService pomodoroService;
    private final SearchFilterService searchFilterService;
    private final SettingsService settingsService;
    private final ThemeManager themeManager;
    private final TaskRepository taskRepository;
    private final FocusSessionRepository focusSessionRepository;

    private final BorderPane rootBorderPane = new BorderPane();
    private final SidebarView sidebarView;
    private final TopNavBar topNavBar;

    // View caches
    private DashboardView dashboardView;
    private MyDayView myDayView;
    private TaskListView inboxView;
    private TaskListView allTasksView;
    private TaskListView importantView;
    private TaskListView upcomingView;
    private CalendarView calendarView;
    private ProjectsView projectsView;
    private FocusModeView focusModeView;
    private AnalyticsView analyticsView;
    private SettingsView settingsView;

    private SidebarView.NavItem currentNavItem = SidebarView.NavItem.DASHBOARD;

    public MainLayout(Stage primaryStage,
                      DatabaseManager databaseManager,
                      TaskService taskService,
                      ProjectService projectService,
                      CategoryService categoryService,
                      MyDayService myDayService,
                      AnalyticsService analyticsService,
                      PomodoroService pomodoroService,
                      SearchFilterService searchFilterService,
                      SettingsService settingsService,
                      ThemeManager themeManager,
                      TaskRepository taskRepository,
                      FocusSessionRepository focusSessionRepository) {
        this.primaryStage = primaryStage;
        this.databaseManager = databaseManager;
        this.taskService = taskService;
        this.projectService = projectService;
        this.categoryService = categoryService;
        this.myDayService = myDayService;
        this.analyticsService = analyticsService;
        this.pomodoroService = pomodoroService;
        this.searchFilterService = searchFilterService;
        this.settingsService = settingsService;
        this.themeManager = themeManager;
        this.taskRepository = taskRepository;
        this.focusSessionRepository = focusSessionRepository;

        getStyleClass().add("main-container");

        // Top Navigation Bar
        topNavBar = new TopNavBar(
                themeManager,
                NotificationManager.getInstance(),
                this::handleGlobalSearch,
                this::openCreateTaskDialog
        );

        // Sidebar Navigation
        sidebarView = new SidebarView(taskRepository, this::navigateTo);

        rootBorderPane.setTop(topNavBar);
        rootBorderPane.setLeft(sidebarView);

        // Initialize default view (Dashboard)
        showDashboard();

        // Listen for data changes across the app
        taskService.addChangeListener(this::handleDataChanged);
        projectService.addListener(this::handleDataChanged);
        categoryService.addListener(this::handleDataChanged);

        // Stack toast container on top of root layout
        getChildren().addAll(rootBorderPane, NotificationManager.getInstance().getToastContainer());
    }

    public void navigateTo(SidebarView.NavItem item) {
        this.currentNavItem = item;
        sidebarView.setActiveItem(item);

        switch (item) {
            case DASHBOARD -> showDashboard();
            case MY_DAY -> showMyDay();
            case INBOX -> showInbox();
            case ALL_TASKS -> showAllTasks();
            case IMPORTANT -> showImportant();
            case UPCOMING -> showUpcoming();
            case CALENDAR -> showCalendar();
            case PROJECTS -> showProjects();
            case FOCUS_MODE -> showFocusMode();
            case ANALYTICS -> showAnalytics();
            case SETTINGS -> showSettings();
        }
    }

    private void showDashboard() {
        if (dashboardView == null) {
            dashboardView = new DashboardView(
                    taskService, analyticsService,
                    this::openEditTaskDialog,
                    this::handleToggleComplete,
                    this::handleToggleImportant,
                    this::handleToggleMyDay,
                    this::handleDeleteTask
            );
        } else {
            dashboardView.refresh();
        }
        rootBorderPane.setCenter(dashboardView);
    }

    private void showMyDay() {
        if (myDayView == null) {
            myDayView = new MyDayView(
                    myDayService, taskService,
                    this::openEditTaskDialog,
                    this::handleToggleComplete,
                    this::handleToggleImportant,
                    this::handleToggleMyDay,
                    this::handleDeleteTask,
                    this::openCreateTaskDialog
            );
        } else {
            myDayView.refresh();
        }
        rootBorderPane.setCenter(myDayView);
    }

    private void showInbox() {
        if (inboxView == null) {
            inboxView = new TaskListView(
                    TaskListView.ViewType.INBOX,
                    taskService, projectService, categoryService, searchFilterService,
                    this::openEditTaskDialog,
                    this::handleToggleComplete,
                    this::handleToggleImportant,
                    this::handleToggleMyDay,
                    this::handleDeleteTask,
                    this::openCreateTaskDialog
            );
        } else {
            inboxView.refresh();
        }
        rootBorderPane.setCenter(inboxView);
    }

    private void showAllTasks() {
        if (allTasksView == null) {
            allTasksView = new TaskListView(
                    TaskListView.ViewType.ALL_TASKS,
                    taskService, projectService, categoryService, searchFilterService,
                    this::openEditTaskDialog,
                    this::handleToggleComplete,
                    this::handleToggleImportant,
                    this::handleToggleMyDay,
                    this::handleDeleteTask,
                    this::openCreateTaskDialog
            );
        } else {
            allTasksView.refresh();
        }
        rootBorderPane.setCenter(allTasksView);
    }

    private void showImportant() {
        if (importantView == null) {
            importantView = new TaskListView(
                    TaskListView.ViewType.IMPORTANT,
                    taskService, projectService, categoryService, searchFilterService,
                    this::openEditTaskDialog,
                    this::handleToggleComplete,
                    this::handleToggleImportant,
                    this::handleToggleMyDay,
                    this::handleDeleteTask,
                    this::openCreateTaskDialog
            );
        } else {
            importantView.refresh();
        }
        rootBorderPane.setCenter(importantView);
    }

    private void showUpcoming() {
        if (upcomingView == null) {
            upcomingView = new TaskListView(
                    TaskListView.ViewType.UPCOMING,
                    taskService, projectService, categoryService, searchFilterService,
                    this::openEditTaskDialog,
                    this::handleToggleComplete,
                    this::handleToggleImportant,
                    this::handleToggleMyDay,
                    this::handleDeleteTask,
                    this::openCreateTaskDialog
            );
        } else {
            upcomingView.refresh();
        }
        rootBorderPane.setCenter(upcomingView);
    }

    private void showCalendar() {
        if (calendarView == null) {
            calendarView = new CalendarView(taskService, this::openEditTaskDialog);
        } else {
            calendarView.refresh();
        }
        rootBorderPane.setCenter(calendarView);
    }

    private void showProjects() {
        if (projectsView == null) {
            projectsView = new ProjectsView(
                    projectService, categoryService, taskService, taskRepository,
                    this::openEditTaskDialog,
                    this::handleToggleComplete,
                    this::handleToggleImportant,
                    this::handleToggleMyDay,
                    this::handleDeleteTask
            );
        } else {
            projectsView.refresh();
        }
        rootBorderPane.setCenter(projectsView);
    }

    private void showFocusMode() {
        if (focusModeView == null) {
            focusModeView = new FocusModeView(pomodoroService, focusSessionRepository, taskService);
        } else {
            focusModeView.refresh();
        }
        rootBorderPane.setCenter(focusModeView);
    }

    private void showAnalytics() {
        if (analyticsView == null) {
            analyticsView = new AnalyticsView(analyticsService);
        } else {
            analyticsView.refresh();
        }
        rootBorderPane.setCenter(analyticsView);
    }

    private void showSettings() {
        if (settingsView == null) {
            settingsView = new SettingsView(
                    settingsService, themeManager, pomodoroService,
                    taskService, projectService, databaseManager
            );
        }
        rootBorderPane.setCenter(settingsView);
    }

    private void handleGlobalSearch(String query) {
        showAllTasks();
        sidebarView.setActiveItem(SidebarView.NavItem.ALL_TASKS);
    }

    public void openCreateTaskDialog() {
        TaskDialog dialog = new TaskDialog(primaryStage, null, projectService, categoryService);
        Optional<Task> result = dialog.showDialogAndWait();
        result.ifPresent(task -> {
            try {
                taskService.createTask(task);
                NotificationManager.getInstance().showNotification(
                        "Task Created",
                        "\"" + task.getTitle() + "\" added successfully.",
                        com.smarttodo.components.ToastNotification.Type.SUCCESS
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void openEditTaskDialog(Task task) {
        try {
            // Load full details including subtasks
            Optional<Task> fullTaskOpt = taskService.getTaskWithSubtasks(task.getId());
            Task target = fullTaskOpt.orElse(task);

            TaskDialog dialog = new TaskDialog(primaryStage, target, projectService, categoryService);
            Optional<Task> result = dialog.showDialogAndWait();
            result.ifPresent(updated -> {
                try {
                    taskService.updateTask(updated);
                    NotificationManager.getInstance().showNotification(
                            "Task Updated",
                            "Changes saved to \"" + updated.getTitle() + "\".",
                            com.smarttodo.components.ToastNotification.Type.INFO
                    );
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleToggleComplete(Task task) {
        try {
            if (task.getStatus() == com.smarttodo.model.TaskStatus.COMPLETED) {
                taskService.reopenTask(task.getId());
            } else {
                taskService.markComplete(task.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleToggleImportant(Task task) {
        try {
            taskService.toggleImportant(task.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleToggleMyDay(Task task) {
        try {
            taskService.toggleMyDay(task.getId(), LocalDate.now());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteTask(Task task) {
        try {
            taskService.deleteTask(task.getId());
            NotificationManager.getInstance().showNotification(
                    "Task Deleted",
                    "Task \"" + task.getTitle() + "\" has been deleted.",
                    com.smarttodo.components.ToastNotification.Type.WARNING
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleDataChanged() {
        Platform.runLater(() -> {
            sidebarView.refreshBadges();
            if (dashboardView != null) dashboardView.refresh();
            if (myDayView != null) myDayView.refresh();
            if (inboxView != null) inboxView.refresh();
            if (allTasksView != null) allTasksView.refresh();
            if (importantView != null) importantView.refresh();
            if (upcomingView != null) upcomingView.refresh();
            if (calendarView != null) calendarView.refresh();
            if (projectsView != null) projectsView.refresh();
            if (focusModeView != null) focusModeView.refresh();
            if (analyticsView != null) analyticsView.refresh();
        });
    }
}
