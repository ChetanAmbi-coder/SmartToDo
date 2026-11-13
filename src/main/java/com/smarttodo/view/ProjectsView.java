package com.smarttodo.view;

import com.smarttodo.components.TaskCard;
import com.smarttodo.model.Category;
import com.smarttodo.model.Project;
import com.smarttodo.model.Task;
import com.smarttodo.repository.TaskRepository;
import com.smarttodo.service.CategoryService;
import com.smarttodo.service.ProjectService;
import com.smarttodo.service.TaskService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ProjectsView extends BorderPane {
    private final ProjectService projectService;
    private final CategoryService categoryService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;

    private final Consumer<Task> onSelectTask;
    private final Consumer<Task> onToggleComplete;
    private final Consumer<Task> onToggleImportant;
    private final Consumer<Task> onToggleMyDay;
    private final Consumer<Task> onDeleteTask;

    private final VBox projectsListContainer = new VBox(8);
    private final VBox categoriesListContainer = new VBox(8);
    private final VBox projectTasksContainer = new VBox(8);

    private Project selectedProject;
    private final Label projectDetailTitle = new Label("Select a project");
    private final Label projectDetailDesc = new Label();
    private final ProgressBar projectProgressBar = new ProgressBar(0.0);
    private final Label projectProgressText = new Label();

    public ProjectsView(ProjectService projectService,
                        CategoryService categoryService,
                        TaskService taskService,
                        TaskRepository taskRepository,
                        Consumer<Task> onSelectTask,
                        Consumer<Task> onToggleComplete,
                        Consumer<Task> onToggleImportant,
                        Consumer<Task> onToggleMyDay,
                        Consumer<Task> onDeleteTask) {
        this.projectService = projectService;
        this.categoryService = categoryService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.onSelectTask = onSelectTask;
        this.onToggleComplete = onToggleComplete;
        this.onToggleImportant = onToggleImportant;
        this.onToggleMyDay = onToggleMyDay;
        this.onDeleteTask = onDeleteTask;

        setPadding(new Insets(24));

        // Left sidebar: Projects & Categories lists
        VBox leftPanel = new VBox(20);
        leftPanel.setPrefWidth(320);
        leftPanel.setMinWidth(280);
        leftPanel.setStyle("-fx-border-color: transparent -color-border transparent transparent; -fx-padding: 0 16 0 0;");

        // Projects Section
        HBox projHeader = new HBox(8);
        projHeader.setAlignment(Pos.CENTER_LEFT);
        Label projLabel = new Label("Projects");
        projLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        HBox.setHgrow(projLabel, Priority.ALWAYS);

        Button newProjBtn = new Button("+ New");
        newProjBtn.getStyleClass().add("btn-primary");
        newProjBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10 4 10;");
        newProjBtn.setOnAction(e -> showCreateProjectDialog());
        projHeader.getChildren().addAll(projLabel, newProjBtn);

        ScrollPane projScroll = new ScrollPane(projectsListContainer);
        projScroll.setFitToWidth(true);
        projScroll.setPrefHeight(260);
        projScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Categories Section
        HBox catHeader = new HBox(8);
        catHeader.setAlignment(Pos.CENTER_LEFT);
        Label catLabel = new Label("Categories");
        catLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        HBox.setHgrow(catLabel, Priority.ALWAYS);

        Button newCatBtn = new Button("+ New");
        newCatBtn.getStyleClass().add("btn-secondary");
        newCatBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10 4 10;");
        newCatBtn.setOnAction(e -> showCreateCategoryDialog());
        catHeader.getChildren().addAll(catLabel, newCatBtn);

        ScrollPane catScroll = new ScrollPane(categoriesListContainer);
        catScroll.setFitToWidth(true);
        catScroll.setPrefHeight(220);
        catScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        leftPanel.getChildren().addAll(projHeader, projScroll, new Separator(), catHeader, catScroll);
        setLeft(leftPanel);

        // Center: Selected Project Details & Associated Tasks
        VBox rightPanel = new VBox(16);
        rightPanel.setPadding(new Insets(0, 0, 0, 20));

        // Project Header Card
        VBox headerCard = new VBox(10);
        headerCard.getStyleClass().add("card");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        projectDetailTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        HBox.setHgrow(projectDetailTitle, Priority.ALWAYS);

        Button editProjBtn = new Button("Edit");
        editProjBtn.getStyleClass().add("btn-secondary");
        editProjBtn.setOnAction(e -> {
            if (selectedProject != null) showEditProjectDialog(selectedProject);
        });

        Button deleteProjBtn = new Button("Delete");
        deleteProjBtn.getStyleClass().add("btn-danger");
        deleteProjBtn.setOnAction(e -> {
            if (selectedProject != null) handleDeleteProject(selectedProject);
        });

        titleRow.getChildren().addAll(projectDetailTitle, editProjBtn, deleteProjBtn);

        projectDetailDesc.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");

        projectProgressBar.setMaxWidth(Double.MAX_VALUE);
        projectProgressBar.setPrefHeight(8);
        projectProgressBar.setStyle("-fx-accent: -color-accent;");
        projectProgressText.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-secondary; -fx-font-weight: bold;");

        headerCard.getChildren().addAll(titleRow, projectDetailDesc, projectProgressText, projectProgressBar);

        // Project Tasks List
        ScrollPane tasksScroll = new ScrollPane(projectTasksContainer);
        tasksScroll.setFitToWidth(true);
        tasksScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(tasksScroll, Priority.ALWAYS);

        rightPanel.getChildren().addAll(headerCard, tasksScroll);
        setCenter(rightPanel);

        refresh();
    }

    public void refresh() {
        try {
            List<Project> projects = projectService.getAllProjects();
            projectsListContainer.getChildren().clear();

            if (projects.isEmpty()) {
                Label empty = new Label("No projects created yet.");
                empty.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 8;");
                projectsListContainer.getChildren().add(empty);
            } else {
                for (Project p : projects) {
                    HBox card = new HBox(8);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.getStyleClass().add("card");
                    card.setStyle("-fx-padding: 10 12 10 12; -fx-cursor: hand;");

                    Region colorDot = new Region();
                    colorDot.setPrefSize(10, 10);
                    colorDot.setStyle("-fx-background-color: " + p.getColor() + "; -fx-background-radius: 5px;");

                    VBox textCol = new VBox(2);
                    HBox.setHgrow(textCol, Priority.ALWAYS);

                    Label nameLbl = new Label(p.getName());
                    nameLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-primary;");

                    Label countLbl = new Label(p.getCompletedTasks() + " / " + p.getTotalTasks() + " completed");
                    countLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
                    textCol.getChildren().addAll(nameLbl, countLbl);

                    card.getChildren().addAll(colorDot, textCol);

                    if (selectedProject != null && selectedProject.getId().equals(p.getId())) {
                        card.setStyle(card.getStyle() + "; -fx-border-color: -color-accent;");
                    }

                    card.setOnMouseClicked(e -> {
                        selectedProject = p;
                        refresh();
                    });

                    projectsListContainer.getChildren().add(card);
                }

                if (selectedProject == null && !projects.isEmpty()) {
                    selectedProject = projects.get(0);
                }
            }

            // Populate categories
            List<Category> categories = categoryService.getAllCategories();
            categoriesListContainer.getChildren().clear();
            for (Category c : categories) {
                HBox catRow = new HBox(8);
                catRow.setAlignment(Pos.CENTER_LEFT);
                catRow.setPadding(new Insets(6, 8, 6, 8));

                Region colorDot = new Region();
                colorDot.setPrefSize(8, 8);
                colorDot.setStyle("-fx-background-color: " + c.getColor() + "; -fx-background-radius: 4px;");

                Label nameLbl = new Label(c.getName());
                nameLbl.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 12px;");
                HBox.setHgrow(nameLbl, Priority.ALWAYS);

                catRow.getChildren().addAll(colorDot, nameLbl);

                if (!c.isSystem()) {
                    Button delBtn = new Button("✕");
                    delBtn.getStyleClass().add("btn-icon");
                    delBtn.setStyle("-fx-font-size: 9px;");
                    delBtn.setOnAction(e -> {
                        try {
                            categoryService.deleteCategory(c.getId());
                            refresh();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });
                    catRow.getChildren().add(delBtn);
                }

                categoriesListContainer.getChildren().add(catRow);
            }

            // Update Right Panel Project Details
            if (selectedProject != null) {
                projectDetailTitle.setText("📁 " + selectedProject.getName());
                projectDetailDesc.setText(selectedProject.getDescription() != null && !selectedProject.getDescription().trim().isEmpty() ?
                        selectedProject.getDescription() : "No description provided.");
                projectProgressBar.setProgress(selectedProject.getProgress());
                projectProgressText.setText(String.format("%d of %d tasks completed (%.0f%%)",
                        selectedProject.getCompletedTasks(), selectedProject.getTotalTasks(), selectedProject.getProgress() * 100.0));

                List<Task> projectTasks = taskRepository.findByProject(selectedProject.getId());
                projectTasksContainer.getChildren().clear();
                if (projectTasks.isEmpty()) {
                    Label noTasks = new Label("No tasks assigned to this project yet. Assign tasks in the task editor.");
                    noTasks.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 16;");
                    projectTasksContainer.getChildren().add(noTasks);
                } else {
                    for (Task t : projectTasks) {
                        projectTasksContainer.getChildren().add(new TaskCard(
                                t, onSelectTask, onToggleComplete, onToggleImportant, onToggleMyDay, onDeleteTask
                        ));
                    }
                }
            } else {
                projectDetailTitle.setText("No Project Selected");
                projectDetailDesc.setText("Create or select a project on the left to see details and tasks.");
                projectProgressBar.setProgress(0.0);
                projectProgressText.setText("");
                projectTasksContainer.getChildren().clear();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showCreateProjectDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a new project");
        dialog.setContentText("Project Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    Project p = new Project(name.trim(), "", "#3B82F6", "folder");
                    Project created = projectService.createProject(p);
                    selectedProject = created;
                    refresh();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showEditProjectDialog(Project project) {
        TextInputDialog dialog = new TextInputDialog(project.getName());
        dialog.setTitle("Edit Project");
        dialog.setHeaderText("Rename project: " + project.getName());
        dialog.setContentText("New Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                try {
                    project.setName(newName.trim());
                    projectService.updateProject(project);
                    refresh();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleDeleteProject(Project project) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Project");
        alert.setHeaderText("Delete project \"" + project.getName() + "\"?");
        alert.setContentText("Associated tasks will NOT be deleted. They will be safely moved to Inbox.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                projectService.deleteProject(project.getId());
                selectedProject = null;
                refresh();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showCreateCategoryDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Category");
        dialog.setHeaderText("Create a custom task category");
        dialog.setContentText("Category Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    Category c = new Category(name.trim(), "#6366F1", "tag", false);
                    categoryService.createCategory(c);
                    refresh();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
