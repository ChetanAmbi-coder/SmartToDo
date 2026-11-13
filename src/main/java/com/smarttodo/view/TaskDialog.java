package com.smarttodo.view;

import com.smarttodo.model.Category;
import com.smarttodo.model.Project;
import com.smarttodo.model.RepeatFrequency;
import com.smarttodo.model.Subtask;
import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.model.Priority;
import com.smarttodo.service.CategoryService;
import com.smarttodo.service.ProjectService;
import com.smarttodo.util.ValidationException;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskDialog extends Stage {
    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ComboBox<Project> projectCombo = new ComboBox<>();
    private final ComboBox<Category> categoryCombo = new ComboBox<>();
    private final ComboBox<Priority> priorityCombo = new ComboBox<>();
    private final ComboBox<TaskStatus> statusCombo = new ComboBox<>();
    private final CheckBox importantCheck = new CheckBox("Mark as Important");
    private final CheckBox myDayCheck = new CheckBox("Add to My Day");
    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker dueDatePicker = new DatePicker();
    private final TextField dueTimeField = new TextField();
    private final Spinner<Integer> durationSpinner = new Spinner<>(0, 1440, 30, 5);
    private final DatePicker reminderDatePicker = new DatePicker();
    private final TextField reminderTimeField = new TextField();
    private final ComboBox<RepeatFrequency> repeatCombo = new ComboBox<>();
    private final TextArea notesArea = new TextArea();

    // Subtasks management in dialog
    private final List<Subtask> workingSubtasks = new ArrayList<>();
    private final VBox subtasksListContainer = new VBox(6);
    private final TextField newSubtaskField = new TextField();

    private Task resultTask = null;
    private boolean saved = false;

    public TaskDialog(Stage owner, Task existingTask, ProjectService projectService, CategoryService categoryService) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(existingTask == null ? "Create New Task" : "Edit Task");

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: -color-bg-primary;");
        root.getStyleClass().add("card");

        // Header
        Label headerLabel = new Label(existingTask == null ? "Create New Task" : "Edit Task");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");

        // Load project and category choices
        try {
            List<Project> projects = new ArrayList<>();
            Project noneProject = new Project();
            noneProject.setId(null);
            noneProject.setName("Inbox (No Project)");
            projects.add(noneProject);
            projects.addAll(projectService.getAllProjects());
            projectCombo.setItems(FXCollections.observableArrayList(projects));
            projectCombo.getSelectionModel().select(0);

            List<Category> categories = new ArrayList<>();
            Category noneCategory = new Category();
            noneCategory.setId(null);
            noneCategory.setName("None (No Category)");
            categories.add(noneCategory);
            categories.addAll(categoryService.getAllCategories());
            categoryCombo.setItems(FXCollections.observableArrayList(categories));
            categoryCombo.getSelectionModel().select(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        priorityCombo.setItems(FXCollections.observableArrayList(Priority.values()));
        priorityCombo.getSelectionModel().select(Priority.MEDIUM);

        statusCombo.setItems(FXCollections.observableArrayList(TaskStatus.values()));
        statusCombo.getSelectionModel().select(TaskStatus.TODO);

        repeatCombo.setItems(FXCollections.observableArrayList(RepeatFrequency.values()));
        repeatCombo.getSelectionModel().select(RepeatFrequency.NONE);

        titleField.setPromptText("What needs to be done?");
        titleField.getStyleClass().add("search-field");
        titleField.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        descriptionArea.setPromptText("Add a detailed description (optional)...");
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setWrapText(true);

        notesArea.setPromptText("Additional notes, links, or context...");
        notesArea.setPrefRowCount(2);
        notesArea.setWrapText(true);

        dueTimeField.setPromptText("HH:MM (e.g. 17:00)");
        dueTimeField.setPrefWidth(120);

        reminderTimeField.setPromptText("HH:MM (e.g. 09:00)");
        reminderTimeField.setPrefWidth(120);

        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(100);

        // Subtasks section
        VBox subtasksSection = new VBox(8);
        Label subtasksHeader = new Label("Subtasks Checklist");
        subtasksHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-secondary; -fx-font-size: 12px;");

        HBox addSubtaskRow = new HBox(8);
        newSubtaskField.setPromptText("Add a step / subtask...");
        HBox.setHgrow(newSubtaskField, javafx.scene.layout.Priority.ALWAYS);
        Button addSubtaskBtn = new Button("+ Add");
        addSubtaskBtn.getStyleClass().add("btn-secondary");
        addSubtaskBtn.setOnAction(e -> handleAddSubtask());
        newSubtaskField.setOnAction(e -> handleAddSubtask());
        addSubtaskRow.getChildren().addAll(newSubtaskField, addSubtaskBtn);

        subtasksSection.getChildren().addAll(subtasksHeader, subtasksListContainer, addSubtaskRow);

        // Populate existing task values if editing
        if (existingTask != null) {
            titleField.setText(existingTask.getTitle());
            descriptionArea.setText(existingTask.getDescription() != null ? existingTask.getDescription() : "");
            priorityCombo.getSelectionModel().select(existingTask.getPriority());
            statusCombo.getSelectionModel().select(existingTask.getStatus());
            importantCheck.setSelected(existingTask.isImportant());
            myDayCheck.setSelected(existingTask.isInMyDay());
            startDatePicker.setValue(existingTask.getStartDate());
            dueDatePicker.setValue(existingTask.getDueDate());
            if (existingTask.getDueTime() != null) {
                dueTimeField.setText(existingTask.getDueTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            durationSpinner.getValueFactory().setValue(existingTask.getEstimatedDuration());
            if (existingTask.getReminderDateTime() != null) {
                reminderDatePicker.setValue(existingTask.getReminderDateTime().toLocalDate());
                reminderTimeField.setText(existingTask.getReminderDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            repeatCombo.getSelectionModel().select(existingTask.getRepeatFrequency());
            notesArea.setText(existingTask.getNotes() != null ? existingTask.getNotes() : "");

            // Select project
            if (existingTask.getProjectId() != null) {
                for (Project p : projectCombo.getItems()) {
                    if (p.getId() != null && p.getId().equals(existingTask.getProjectId())) {
                        projectCombo.getSelectionModel().select(p);
                        break;
                    }
                }
            }

            // Select category
            if (existingTask.getCategoryId() != null) {
                for (Category c : categoryCombo.getItems()) {
                    if (c.getId() != null && c.getId().equals(existingTask.getCategoryId())) {
                        categoryCombo.getSelectionModel().select(c);
                        break;
                    }
                }
            }

            // Copy subtasks
            if (existingTask.getSubtasks() != null) {
                for (Subtask s : existingTask.getSubtasks()) {
                    workingSubtasks.add(new Subtask(s.getId(), s.getTaskId(), s.getTitle(), s.isCompleted(), s.getCreatedAt()));
                }
            }
        }

        renderSubtaskList();

        // Form Layout Grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        int row = 0;
        grid.add(new Label("Project:"), 0, row);
        grid.add(projectCombo, 1, row);
        grid.add(new Label("Category:"), 2, row);
        grid.add(categoryCombo, 3, row);

        row++;
        grid.add(new Label("Priority:"), 0, row);
        grid.add(priorityCombo, 1, row);
        grid.add(new Label("Status:"), 2, row);
        grid.add(statusCombo, 3, row);

        row++;
        grid.add(new Label("Start Date:"), 0, row);
        grid.add(startDatePicker, 1, row);
        grid.add(new Label("Due Date:"), 2, row);
        HBox dueRow = new HBox(6, dueDatePicker, dueTimeField);
        grid.add(dueRow, 3, row);

        row++;
        grid.add(new Label("Estimated Time:"), 0, row);
        HBox durBox = new HBox(6, durationSpinner, new Label("mins"));
        durBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(durBox, 1, row);
        grid.add(new Label("Repeat:"), 2, row);
        grid.add(repeatCombo, 3, row);

        row++;
        grid.add(new Label("Reminder:"), 0, row);
        HBox remBox = new HBox(6, reminderDatePicker, reminderTimeField);
        grid.add(remBox, 1, row, 3, 1);

        row++;
        HBox togglesBox = new HBox(20, importantCheck, myDayCheck);
        grid.add(togglesBox, 0, row, 4, 1);

        for (var node : grid.getChildren()) {
            if (node instanceof Label) {
                node.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-weight: 500;");
            }
        }

        // Action Buttons Row (Cancel, Save)
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> close());

        Button saveBtn = new Button(existingTask == null ? "Create Task" : "Save Changes");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> handleSave(existingTask));

        actionsRow.getChildren().addAll(cancelBtn, saveBtn);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox contentBox = new VBox(14);
        contentBox.getChildren().addAll(
                new Label("Task Title:"),
                titleField,
                new Label("Description:"),
                descriptionArea,
                grid,
                new Separator(),
                subtasksSection,
                new Separator(),
                new Label("Notes:"),
                notesArea
        );
        scroll.setContent(contentBox);
        scroll.setPrefViewportHeight(460);

        root.getChildren().addAll(headerLabel, scroll, actionsRow);

        Scene scene = new Scene(root, 650, 680);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        setScene(scene);
    }

    private void handleAddSubtask() {
        String text = newSubtaskField.getText();
        if (text != null && !text.trim().isEmpty()) {
            Subtask s = new Subtask();
            s.setTitle(text.trim());
            s.setCompleted(false);
            workingSubtasks.add(s);
            newSubtaskField.clear();
            renderSubtaskList();
        }
    }

    private void renderSubtaskList() {
        subtasksListContainer.getChildren().clear();
        for (int i = 0; i < workingSubtasks.size(); i++) {
            Subtask s = workingSubtasks.get(i);
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            CheckBox check = new CheckBox();
            check.setSelected(s.isCompleted());
            check.setOnAction(e -> s.setCompleted(check.isSelected()));

            Label titleLbl = new Label(s.getTitle());
            titleLbl.setStyle(s.isCompleted() ? "-fx-text-fill: -color-text-muted; -fx-strikethrough: true;" : "-fx-text-fill: -color-text-primary;");
            HBox.setHgrow(titleLbl, javafx.scene.layout.Priority.ALWAYS);

            Button delBtn = new Button("✕");
            delBtn.getStyleClass().add("btn-icon");
            delBtn.setStyle("-fx-font-size: 10px;");
            delBtn.setOnAction(e -> {
                workingSubtasks.remove(s);
                renderSubtaskList();
            });

            itemRow.getChildren().addAll(check, titleLbl, delBtn);
            subtasksListContainer.getChildren().add(itemRow);
        }
    }

    private void handleSave(Task existingTask) {
        try {
            String title = titleField.getText();
            if (title == null || title.trim().isEmpty()) {
                throw new ValidationException("Task title cannot be empty.");
            }

            LocalDate start = startDatePicker.getValue();
            LocalDate due = dueDatePicker.getValue();
            if (start != null && due != null && due.isBefore(start)) {
                throw new ValidationException("Due date cannot be earlier than start date.");
            }

            LocalTime dueTime = null;
            String dueTimeStr = dueTimeField.getText();
            if (dueTimeStr != null && !dueTimeStr.trim().isEmpty()) {
                try {
                    dueTime = LocalTime.parse(dueTimeStr.trim());
                } catch (DateTimeParseException ex) {
                    throw new ValidationException("Invalid due time format. Please use HH:mm (e.g. 14:30).");
                }
            }

            LocalDateTime reminder = null;
            LocalDate remDate = reminderDatePicker.getValue();
            String remTimeStr = reminderTimeField.getText();
            if (remDate != null) {
                LocalTime remTime = LocalTime.of(9, 0);
                if (remTimeStr != null && !remTimeStr.trim().isEmpty()) {
                    try {
                        remTime = LocalTime.parse(remTimeStr.trim());
                    } catch (DateTimeParseException ex) {
                        throw new ValidationException("Invalid reminder time format. Please use HH:mm.");
                    }
                }
                reminder = LocalDateTime.of(remDate, remTime);
                if (due != null && remDate.isAfter(due)) {
                    throw new ValidationException("Reminder date cannot be after task due date.");
                }
            }

            Task task = existingTask != null ? existingTask : new Task();
            task.setTitle(title.trim());
            task.setDescription(descriptionArea.getText());

            Project selProject = projectCombo.getValue();
            task.setProjectId(selProject != null ? selProject.getId() : null);

            Category selCat = categoryCombo.getValue();
            task.setCategoryId(selCat != null ? selCat.getId() : null);

            task.setPriority(priorityCombo.getValue());
            task.setStatus(statusCombo.getValue());
            task.setImportant(importantCheck.isSelected());
            task.setInMyDay(myDayCheck.isSelected());
            task.setStartDate(start);
            task.setDueDate(due);
            task.setDueTime(dueTime);
            task.setEstimatedDuration(durationSpinner.getValue());
            task.setReminderDateTime(reminder);
            task.setRepeatFrequency(repeatCombo.getValue());
            task.setNotes(notesArea.getText());
            task.setSubtasks(new ArrayList<>(workingSubtasks));

            this.resultTask = task;
            this.saved = true;
            close();

        } catch (ValidationException ve) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Please check the form input");
            alert.setContentText(ve.getMessage());
            alert.showAndWait();
        }
    }

    public Optional<Task> showDialogAndWait() {
        showAndWait();
        return saved ? Optional.ofNullable(resultTask) : Optional.empty();
    }
}
