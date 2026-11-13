package com.smarttodo.view;

import com.smarttodo.components.TaskCard;
import com.smarttodo.model.Category;
import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.service.CategoryService;
import com.smarttodo.service.ProjectService;
import com.smarttodo.service.SearchFilterService;
import com.smarttodo.service.TaskService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TaskListView extends BorderPane {
    public enum ViewType {
        INBOX("Inbox", "Uncategorized tasks awaiting review"),
        ALL_TASKS("All Tasks", "Complete registry of your tasks"),
        IMPORTANT("Important", "High-priority flagged tasks"),
        UPCOMING("Upcoming", "Scheduled tasks for the next 7 days"),
        TODAY("Today", "Tasks scheduled or planned for today"),
        OVERDUE("Overdue", "Tasks that have passed their deadline"),
        COMPLETED("Completed", "Finished tasks archive");

        private final String title;
        private final String subtitle;

        ViewType(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }
    }

    private final ViewType viewType;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final CategoryService categoryService;
    private final SearchFilterService searchFilterService;

    private final Consumer<Task> onSelectTask;
    private final Consumer<Task> onToggleComplete;
    private final Consumer<Task> onToggleImportant;
    private final Consumer<Task> onToggleMyDay;
    private final Consumer<Task> onDeleteTask;
    private final Runnable onQuickAddNewTask;

    // Filters UI
    private final TextField searchField = new TextField();
    private final ComboBox<String> priorityFilter = new ComboBox<>();
    private final ComboBox<Category> categoryFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> sortSelector = new ComboBox<>();
    private final Button clearFiltersBtn = new Button("Clear Filters");

    // Quick add inline
    private final TextField quickAddInput = new TextField();

    // Content container
    private final VBox tasksContainer = new VBox(8);
    private final Label countLabel = new Label();
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();

    public TaskListView(ViewType viewType,
                        TaskService taskService,
                        ProjectService projectService,
                        CategoryService categoryService,
                        SearchFilterService searchFilterService,
                        Consumer<Task> onSelectTask,
                        Consumer<Task> onToggleComplete,
                        Consumer<Task> onToggleImportant,
                        Consumer<Task> onToggleMyDay,
                        Consumer<Task> onDeleteTask,
                        Runnable onQuickAddNewTask) {
        this.viewType = viewType;
        this.taskService = taskService;
        this.projectService = projectService;
        this.categoryService = categoryService;
        this.searchFilterService = searchFilterService;
        this.onSelectTask = onSelectTask;
        this.onToggleComplete = onToggleComplete;
        this.onToggleImportant = onToggleImportant;
        this.onToggleMyDay = onToggleMyDay;
        this.onDeleteTask = onDeleteTask;
        this.onQuickAddNewTask = onQuickAddNewTask;

        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        // Top Header and Filters
        VBox topSection = new VBox(14);

        titleLabel.setText(viewType.getTitle());
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");

        subtitleLabel.setText(viewType.getSubtitle());
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");

        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-accent; -fx-font-weight: bold;");

        HBox titleRow = new HBox(12, titleLabel, countLabel);
        titleRow.setAlignment(Pos.BASELINE_LEFT);

        // Filter Bar
        FlowPane filterBar = new FlowPane();
        filterBar.setHgap(10);
        filterBar.setVgap(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Filter by title or notes...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refresh());

        // Priority Filter
        priorityFilter.getItems().addAll("All Priorities", "Critical", "High", "Medium", "Low");
        priorityFilter.getSelectionModel().select(0);
        priorityFilter.setOnAction(e -> refresh());

        // Category Filter
        populateCategoryFilter();
        categoryFilter.setOnAction(e -> refresh());

        // Status Filter
        statusFilter.getItems().addAll("All Statuses", "To Do", "In Progress", "Completed");
        statusFilter.getSelectionModel().select(0);
        statusFilter.setOnAction(e -> refresh());

        // Sort Selector
        sortSelector.getItems().addAll("Sort: Due Date (Earliest)", "Sort: Priority (Highest)", "Sort: Created (Newest)", "Sort: Title (A-Z)");
        sortSelector.getSelectionModel().select(0);
        sortSelector.setOnAction(e -> refresh());

        clearFiltersBtn.getStyleClass().add("btn-secondary");
        clearFiltersBtn.setOnAction(e -> resetFilters());

        filterBar.getChildren().addAll(searchField, priorityFilter, categoryFilter, statusFilter, sortSelector, clearFiltersBtn);

        // Quick Add Box
        HBox quickAddBox = new HBox(8);
        quickAddBox.setAlignment(Pos.CENTER_LEFT);
        quickAddBox.getStyleClass().add("card");
        quickAddBox.setPadding(new Insets(8, 12, 8, 12));

        Label plusIcon = new Label("+");
        plusIcon.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");

        quickAddInput.setPromptText("Quick add a task here and press Enter...");
        quickAddInput.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 13px; -fx-text-fill: -color-text-primary;");
        HBox.setHgrow(quickAddInput, Priority.ALWAYS);
        quickAddInput.setOnAction(e -> handleQuickAdd());

        Button quickAddBtn = new Button("Add");
        quickAddBtn.getStyleClass().add("btn-primary");
        quickAddBtn.setOnAction(e -> handleQuickAdd());

        Button detailedAddBtn = new Button("Detailed...");
        detailedAddBtn.getStyleClass().add("btn-secondary");
        detailedAddBtn.setOnAction(e -> {
            if (onQuickAddNewTask != null) onQuickAddNewTask.run();
        });

        quickAddBox.getChildren().addAll(plusIcon, quickAddInput, quickAddBtn, detailedAddBtn);

        topSection.getChildren().addAll(titleRow, subtitleLabel, filterBar, quickAddBox);
        setTop(topSection);

        // Center: Scrollable Task List
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        tasksContainer.setPadding(new Insets(16, 0, 16, 0));
        scrollPane.setContent(tasksContainer);

        setCenter(scrollPane);

        refresh();
    }

    private void populateCategoryFilter() {
        try {
            List<Category> categories = new ArrayList<>();
            Category all = new Category();
            all.setId(null);
            all.setName("All Categories");
            categories.add(all);
            categories.addAll(categoryService.getAllCategories());
            categoryFilter.setItems(FXCollections.observableArrayList(categories));
            categoryFilter.getSelectionModel().select(0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void resetFilters() {
        searchField.clear();
        priorityFilter.getSelectionModel().select(0);
        categoryFilter.getSelectionModel().select(0);
        statusFilter.getSelectionModel().select(0);
        sortSelector.getSelectionModel().select(0);
        refresh();
    }

    private void handleQuickAdd() {
        String text = quickAddInput.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        try {
            Task task = new Task(text.trim());
            if (viewType == ViewType.TODAY) {
                task.setDueDate(LocalDate.now());
                task.setInMyDay(true);
            } else if (viewType == ViewType.IMPORTANT) {
                task.setImportant(true);
            } else if (viewType == ViewType.UPCOMING) {
                task.setDueDate(LocalDate.now().plusDays(1));
            }
            taskService.createTask(task);
            quickAddInput.clear();
            refresh();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refresh() {
        try {
            List<Task> baseTasks;
            switch (viewType) {
                case INBOX -> baseTasks = taskService.getInboxTasks();
                case ALL_TASKS -> baseTasks = taskService.getAllTasks();
                case IMPORTANT -> baseTasks = taskService.getImportantTasks();
                case UPCOMING -> baseTasks = taskService.getUpcomingTasks();
                case TODAY -> baseTasks = taskService.getTodayTasks();
                case OVERDUE -> baseTasks = taskService.getOverdueTasks();
                case COMPLETED -> baseTasks = taskService.getCompletedTasks();
                default -> baseTasks = taskService.getAllTasks();
            }

            // Apply filter criteria
            String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            String pStr = priorityFilter.getValue();
            com.smarttodo.model.Priority pFilter = (pStr != null && !pStr.startsWith("All")) ? com.smarttodo.model.Priority.fromString(pStr) : null;

            Category cSel = categoryFilter.getValue();
            Long cFilter = (cSel != null && cSel.getId() != null) ? cSel.getId() : null;

            String sStr = statusFilter.getValue();
            TaskStatus sFilter = (sStr != null && !sStr.startsWith("All")) ? TaskStatus.fromString(sStr) : null;

            List<Task> filtered = new ArrayList<>();
            for (Task t : baseTasks) {
                if (!q.isEmpty()) {
                    boolean titleMatch = t.getTitle() != null && t.getTitle().toLowerCase().contains(q);
                    boolean descMatch = t.getDescription() != null && t.getDescription().toLowerCase().contains(q);
                    boolean notesMatch = t.getNotes() != null && t.getNotes().toLowerCase().contains(q);
                    if (!titleMatch && !descMatch && !notesMatch) continue;
                }
                if (pFilter != null && t.getPriority() != pFilter) continue;
                if (cFilter != null && (t.getCategoryId() == null || !t.getCategoryId().equals(cFilter))) continue;
                if (sFilter != null && t.getStatus() != sFilter) continue;
                filtered.add(t);
            }

            // Apply sorting
            int sortIndex = sortSelector.getSelectionModel().getSelectedIndex();
            filtered.sort((t1, t2) -> {
                return switch (sortIndex) {
                    case 0 -> { // Due Date Earliest
                        if (t1.getDueDate() != null && t2.getDueDate() != null) yield t1.getDueDate().compareTo(t2.getDueDate());
                        if (t1.getDueDate() != null) yield -1;
                        if (t2.getDueDate() != null) yield 1;
                        yield 0;
                    }
                    case 1 -> { // Priority Highest
                        int l1 = t1.getPriority() != null ? t1.getPriority().getLevel() : 0;
                        int l2 = t2.getPriority() != null ? t2.getPriority().getLevel() : 0;
                        yield Integer.compare(l2, l1);
                    }
                    case 2 -> { // Created Newest
                        if (t1.getCreatedAt() != null && t2.getCreatedAt() != null) yield t2.getCreatedAt().compareTo(t1.getCreatedAt());
                        yield 0;
                    }
                    case 3 -> { // Title A-Z
                        yield t1.getTitle().compareToIgnoreCase(t2.getTitle());
                    }
                    default -> 0;
                };
            });

            countLabel.setText(filtered.size() + " task" + (filtered.size() == 1 ? "" : "s"));

            tasksContainer.getChildren().clear();
            if (filtered.isEmpty()) {
                VBox emptyBox = new VBox(12);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(60, 20, 60, 20));

                Label icon = new Label("📋");
                icon.setStyle("-fx-font-size: 36px;");

                Label emptyText = new Label("No tasks found in " + viewType.getTitle());
                emptyText.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -color-text-secondary;");

                Label subText = new Label("Use the quick add bar above or click '+ New Task' to get organized.");
                subText.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");

                emptyBox.getChildren().addAll(icon, emptyText, subText);
                tasksContainer.getChildren().add(emptyBox);
            } else {
                for (Task t : filtered) {
                    tasksContainer.getChildren().add(new TaskCard(
                            t, onSelectTask, onToggleComplete, onToggleImportant, onToggleMyDay, onDeleteTask
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
