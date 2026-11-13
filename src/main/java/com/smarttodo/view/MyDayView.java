package com.smarttodo.view;

import com.smarttodo.components.TaskCard;
import com.smarttodo.model.Task;
import com.smarttodo.service.MyDayService;
import com.smarttodo.service.TaskService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class MyDayView extends ScrollPane {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");

    private final MyDayService myDayService;
    private final TaskService taskService;
    private final Consumer<Task> onSelectTask;
    private final Consumer<Task> onToggleComplete;
    private final Consumer<Task> onToggleImportant;
    private final Consumer<Task> onToggleMyDay;
    private final Consumer<Task> onDeleteTask;
    private final Runnable onQuickAddNewTask;

    private final ProgressBar progressBar = new ProgressBar(0.0);
    private final Label progressText = new Label("0% Completed");
    private final Label workloadLabel = new Label("Total workload: 0 min");
    private final Label taskSummaryLabel = new Label("0 remaining • 0 completed");

    private final VBox tasksContainer = new VBox(8);
    private final VBox suggestionsContainer = new VBox(8);
    private final TextField quickAddInput = new TextField();

    public MyDayView(MyDayService myDayService,
                     TaskService taskService,
                     Consumer<Task> onSelectTask,
                     Consumer<Task> onToggleComplete,
                     Consumer<Task> onToggleImportant,
                     Consumer<Task> onToggleMyDay,
                     Consumer<Task> onDeleteTask,
                     Runnable onQuickAddNewTask) {
        this.myDayService = myDayService;
        this.taskService = taskService;
        this.onSelectTask = onSelectTask;
        this.onToggleComplete = onToggleComplete;
        this.onToggleImportant = onToggleImportant;
        this.onToggleMyDay = onToggleMyDay;
        this.onDeleteTask = onDeleteTask;
        this.onQuickAddNewTask = onQuickAddNewTask;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        // Header
        Label headerTitle = new Label("☀ My Day");
        headerTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");

        Label dateSubtitle = new Label(LocalDate.now().format(DATE_FMT));
        dateSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-accent; -fx-font-weight: 600;");

        // Progress Card
        VBox progressCard = new VBox(10);
        progressCard.getStyleClass().add("card");

        HBox progHeader = new HBox(12, progressText, taskSummaryLabel);
        progHeader.setAlignment(Pos.CENTER_LEFT);
        progressText.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-primary;");
        taskSummaryLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
        HBox.setHgrow(taskSummaryLabel, Priority.ALWAYS);
        progHeader.getChildren().add(workloadLabel);
        workloadLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-accent; -fx-font-size: 12px;");

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: -color-accent;");

        progressCard.getChildren().addAll(progHeader, progressBar);

        // Quick Add Box
        HBox quickAddBox = new HBox(8);
        quickAddBox.setAlignment(Pos.CENTER_LEFT);
        quickAddBox.getStyleClass().add("card");
        quickAddBox.setPadding(new Insets(8, 12, 8, 12));

        Label plusIcon = new Label("+");
        plusIcon.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");

        quickAddInput.setPromptText("Add a task to My Day and press Enter...");
        quickAddInput.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 13px; -fx-text-fill: -color-text-primary;");
        HBox.setHgrow(quickAddInput, Priority.ALWAYS);
        quickAddInput.setOnAction(e -> handleQuickAdd());

        Button addBtn = new Button("Add to Day");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> handleQuickAdd());

        quickAddBox.getChildren().addAll(plusIcon, quickAddInput, addBtn);

        // Today's list
        VBox listCard = new VBox(12);
        listCard.getStyleClass().add("card");
        Label listHeader = new Label("Planned For Today");
        listHeader.getStyleClass().add("card-title");
        listCard.getChildren().addAll(listHeader, tasksContainer);

        // Suggested Tasks Section (Overdue or Upcoming tasks ready to be added to My Day)
        VBox suggestionsCard = new VBox(12);
        suggestionsCard.getStyleClass().add("card");
        Label suggTitle = new Label("💡 Suggested Tasks for Today");
        suggTitle.getStyleClass().add("card-title");
        Label suggSub = new Label("Quickly pull in overdue, upcoming, or high-priority tasks into your day plan.");
        suggSub.getStyleClass().add("card-subtitle");
        suggestionsCard.getChildren().addAll(suggTitle, suggSub, suggestionsContainer);

        content.getChildren().addAll(headerTitle, dateSubtitle, progressCard, quickAddBox, listCard, suggestionsCard);
        setContent(content);

        refresh();
    }

    private void handleQuickAdd() {
        String text = quickAddInput.getText();
        if (text == null || text.trim().isEmpty()) return;

        try {
            Task task = new Task(text.trim());
            task.setDueDate(LocalDate.now());
            task.setInMyDay(true);
            taskService.createTask(task);
            quickAddInput.clear();
            refresh();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refresh() {
        try {
            LocalDate today = LocalDate.now();
            List<Task> myDayTasks = myDayService.getMyDayTasks(today);

            MyDayService.MyDaySummary summary = myDayService.calculateSummary(myDayTasks);
            progressBar.setProgress(summary.getProgress());
            progressText.setText(String.format("%.0f%% Completed", summary.getProgress() * 100.0));
            taskSummaryLabel.setText(String.format("%d remaining • %d completed of %d tasks",
                    summary.getRemainingTasks(), summary.getCompletedTasks(), summary.getTotalTasks()));

            int hours = summary.getTotalWorkloadMinutes() / 60;
            int mins = summary.getTotalWorkloadMinutes() % 60;
            String workloadText = hours > 0 ? String.format("Est. workload: %dh %dm", hours, mins) :
                    String.format("Est. workload: %d min", mins);
            workloadLabel.setText(workloadText);

            tasksContainer.getChildren().clear();
            if (myDayTasks.isEmpty()) {
                VBox empty = new VBox(8);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(30));
                Label emptyTitle = new Label("Your day is empty");
                emptyTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-secondary; -fx-font-size: 14px;");
                Label emptySub = new Label("Plan your day by adding tasks above or choosing from suggestions below.");
                emptySub.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
                empty.getChildren().addAll(emptyTitle, emptySub);
                tasksContainer.getChildren().add(empty);
            } else {
                for (Task t : myDayTasks) {
                    tasksContainer.getChildren().add(new TaskCard(
                            t, onSelectTask, onToggleComplete, onToggleImportant,
                            task -> {
                                // Remove from My Day action
                                try {
                                    myDayService.removeFromMyDay(task.getId(), today);
                                    taskService.notifyChange();
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                            },
                            onDeleteTask
                    ));
                }
            }

            // Suggestions: Overdue tasks + Important tasks not yet in My Day
            List<Task> active = taskService.getActiveTasks();
            suggestionsContainer.getChildren().clear();

            int suggCount = 0;
            for (Task t : active) {
                if (!t.isInMyDay() && (t.isOverdue() || t.isImportant() || t.isDueToday())) {
                    HBox suggRow = new HBox(12);
                    suggRow.setAlignment(Pos.CENTER_LEFT);
                    suggRow.getStyleClass().add("card");
                    suggRow.setPadding(new Insets(8, 12, 8, 12));

                    Label title = new Label(t.getTitle());
                    title.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-primary;");
                    HBox.setHgrow(title, Priority.ALWAYS);

                    if (t.isOverdue()) {
                        Label odBadge = new Label("Overdue");
                        odBadge.getStyleClass().addAll("badge", "badge-overdue");
                        suggRow.getChildren().addAll(title, odBadge);
                    } else if (t.isImportant()) {
                        Label impBadge = new Label("Important");
                        impBadge.getStyleClass().addAll("badge", "badge-priority-high");
                        suggRow.getChildren().addAll(title, impBadge);
                    } else {
                        suggRow.getChildren().add(title);
                    }

                    Button addBtn = new Button("+ Add to My Day");
                    addBtn.getStyleClass().add("btn-secondary");
                    addBtn.setOnAction(e -> {
                        try {
                            myDayService.addToMyDay(t.getId(), today);
                            taskService.notifyChange();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

                    suggRow.getChildren().add(addBtn);
                    suggestionsContainer.getChildren().add(suggRow);
                    suggCount++;
                    if (suggCount >= 5) break;
                }
            }

            if (suggCount == 0) {
                Label noSugg = new Label("No pending suggestions. All urgent tasks are already in your day!");
                noSugg.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 8;");
                suggestionsContainer.getChildren().add(noSugg);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
