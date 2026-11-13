package com.smarttodo.components;

import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

public class TaskCard extends HBox {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd");

    private final Task task;
    private final Consumer<Task> onSelect;
    private final Consumer<Task> onToggleComplete;
    private final Consumer<Task> onToggleImportant;
    private final Consumer<Task> onToggleMyDay;
    private final Consumer<Task> onDelete;

    public TaskCard(Task task,
                    Consumer<Task> onSelect,
                    Consumer<Task> onToggleComplete,
                    Consumer<Task> onToggleImportant,
                    Consumer<Task> onToggleMyDay,
                    Consumer<Task> onDelete) {
        this.task = task;
        this.onSelect = onSelect;
        this.onToggleComplete = onToggleComplete;
        this.onToggleImportant = onToggleImportant;
        this.onToggleMyDay = onToggleMyDay;
        this.onDelete = onDelete;

        getStyleClass().add("task-item-card");
        if (task.getStatus() == TaskStatus.COMPLETED) {
            getStyleClass().add("task-item-card-completed");
        }

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        // Priority left stripe
        Region priorityStripe = new Region();
        priorityStripe.setPrefWidth(4);
        priorityStripe.setMinHeight(36);
        priorityStripe.setStyle("-fx-background-color: " +
                (task.getPriority() != null ? task.getPriority().getColorHex() : "#3B82F6") +
                "; -fx-background-radius: 2px;");

        // Completion Checkbox
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(task.getStatus() == TaskStatus.COMPLETED);
        checkBox.setOnAction(e -> {
            if (onToggleComplete != null) {
                onToggleComplete.accept(task);
            }
        });

        // Center Content Box (Title, Description preview, Badges)
        VBox centerBox = new VBox(4);
        HBox.setHgrow(centerBox, javafx.scene.layout.Priority.ALWAYS);

        // Title and Recommendation Row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add(task.getStatus() == TaskStatus.COMPLETED ? "task-title-completed" : "task-title");

        titleRow.getChildren().add(titleLabel);

        // If recommendation exists
        if (task.getRecommendationScore() > 0 && task.getStatus() != TaskStatus.COMPLETED) {
            Label recBadge = new Label(String.format("★ %.0f • %s", task.getRecommendationScore(),
                    task.getRecommendationReason() != null ? task.getRecommendationReason() : "Smart Pick"));
            recBadge.getStyleClass().addAll("badge", "badge-recommendation");
            titleRow.getChildren().add(recBadge);
        }

        // Subtitle / Notes preview if any
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(task.getDescription().trim());
            descLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px;");
            descLabel.setMaxWidth(500);
            centerBox.getChildren().addAll(titleRow, descLabel);
        } else {
            centerBox.getChildren().add(titleRow);
        }

        // Badges Row (Due date, Priority, Category, Project, Subtasks)
        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // Due date badge
        if (task.getDueDate() != null) {
            Label dueBadge = new Label();
            dueBadge.getStyleClass().add("badge");
            String dueText = task.getDueDate().format(DATE_FMT);
            if (task.getDueTime() != null) {
                dueText += " " + task.getDueTime().format(TIME_FMT);
            }

            if (task.isOverdue()) {
                dueBadge.setText("⚠ Overdue: " + dueText);
                dueBadge.getStyleClass().add("badge-overdue");
            } else if (task.isDueToday()) {
                dueBadge.setText("📅 Today" + (task.getDueTime() != null ? " " + task.getDueTime().format(TIME_FMT) : ""));
                dueBadge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #3B82F6;");
            } else {
                dueBadge.setText("📅 " + dueText);
                dueBadge.setStyle("-fx-background-color: -color-bg-input; -fx-text-fill: -color-text-secondary;");
            }
            badgeRow.getChildren().add(dueBadge);
        }

        // Priority Badge
        if (task.getPriority() != null) {
            Label prioBadge = new Label(task.getPriority().getDisplayName());
            prioBadge.getStyleClass().add("badge");
            switch (task.getPriority()) {
                case CRITICAL -> prioBadge.getStyleClass().add("badge-priority-critical");
                case HIGH -> prioBadge.getStyleClass().add("badge-priority-high");
                case MEDIUM -> prioBadge.getStyleClass().add("badge-priority-medium");
                case LOW -> prioBadge.getStyleClass().add("badge-priority-low");
            }
            badgeRow.getChildren().add(prioBadge);
        }

        // Project Badge
        if (task.getProject() != null) {
            Label projBadge = new Label("📁 " + task.getProject().getName());
            projBadge.getStyleClass().add("badge");
            projBadge.setStyle("-fx-background-color: rgba(99, 102, 241, 0.15); -fx-text-fill: #818CF8;");
            badgeRow.getChildren().add(projBadge);
        }

        // Category Badge
        if (task.getCategory() != null) {
            Label catBadge = new Label("🏷 " + task.getCategory().getName());
            catBadge.getStyleClass().add("badge");
            catBadge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.15); -fx-text-fill: #2DD4BF;");
            badgeRow.getChildren().add(catBadge);
        }

        // Subtasks indicator
        if (task.getTotalSubtasksCount() > 0) {
            Label subBadge = new Label(String.format("☑ %d/%d", task.getCompletedSubtasksCount(), task.getTotalSubtasksCount()));
            subBadge.getStyleClass().add("badge");
            subBadge.setStyle("-fx-background-color: -color-bg-input; -fx-text-fill: -color-text-muted;");
            badgeRow.getChildren().add(subBadge);
        }

        // Duration indicator
        if (task.getEstimatedDuration() > 0) {
            Label durBadge = new Label("⏱ " + task.getEstimatedDuration() + "m");
            durBadge.getStyleClass().add("badge");
            durBadge.setStyle("-fx-background-color: -color-bg-input; -fx-text-fill: -color-text-muted;");
            badgeRow.getChildren().add(durBadge);
        }

        if (!badgeRow.getChildren().isEmpty()) {
            centerBox.getChildren().add(badgeRow);
        }

        // Action Buttons Row (My Day, Star Important, Delete)
        HBox actionsRow = new HBox(4);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        // My Day button
        Button myDayBtn = new Button(task.isInMyDay() ? "☀ In Day" : "☀");
        myDayBtn.getStyleClass().add("btn-icon");
        myDayBtn.setStyle(task.isInMyDay() ?
                "-fx-text-fill: #F59E0B; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-background-radius: 6px;" :
                "-fx-text-fill: -color-text-muted;");
        myDayBtn.setOnAction(e -> {
            e.consume();
            if (onToggleMyDay != null) onToggleMyDay.accept(task);
        });

        // Star Important button
        Button starBtn = new Button(task.isImportant() ? "★" : "☆");
        starBtn.getStyleClass().add("btn-icon");
        starBtn.setStyle(task.isImportant() ?
                "-fx-text-fill: #F59E0B; -fx-font-size: 15px;" :
                "-fx-text-fill: -color-text-muted; -fx-font-size: 15px;");
        starBtn.setOnAction(e -> {
            e.consume();
            if (onToggleImportant != null) onToggleImportant.accept(task);
        });

        // Delete button
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-icon");
        deleteBtn.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 13px;");
        deleteBtn.setOnAction(e -> {
            e.consume();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete task: \"" + task.getTitle() + "\"?");
            alert.setContentText("This action cannot be undone.");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (onDelete != null) onDelete.accept(task);
            }
        });

        actionsRow.getChildren().addAll(myDayBtn, starBtn, deleteBtn);

        // On card click (outside action buttons)
        setOnMouseClicked(e -> {
            if (onSelect != null) {
                onSelect.accept(task);
            }
        });

        getChildren().addAll(priorityStripe, checkBox, centerBox, actionsRow);
    }

    public Task getTask() {
        return task;
    }
}
