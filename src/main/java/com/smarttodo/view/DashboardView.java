package com.smarttodo.view;

import com.smarttodo.components.StatCard;
import com.smarttodo.components.TaskCard;
import com.smarttodo.model.Task;
import com.smarttodo.service.AnalyticsService;
import com.smarttodo.service.TaskService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class DashboardView extends ScrollPane {
    private final TaskService taskService;
    private final AnalyticsService analyticsService;
    private final Consumer<Task> onSelectTask;
    private final Consumer<Task> onToggleComplete;
    private final Consumer<Task> onToggleImportant;
    private final Consumer<Task> onToggleMyDay;
    private final Consumer<Task> onDeleteTask;

    // Stat cards
    private final StatCard activeCard = new StatCard("ACTIVE TASKS", "0", "In progress or pending", "#3B82F6");
    private final StatCard completedCard = new StatCard("COMPLETED", "0", "Finished all-time", "#10B981");
    private final StatCard todayCard = new StatCard("DUE TODAY", "0", "Scheduled for today", "#F59E0B");
    private final StatCard overdueCard = new StatCard("OVERDUE", "0", "Needs attention", "#EF4444");
    private final StatCard focusCard = new StatCard("FOCUS TODAY", "0 min", "Pomodoro tracked", "#8B5CF6");
    private final StatCard weeklyRateCard = new StatCard("COMPLETION", "0%", "Active vs completed", "#06B6D4");

    // Sections
    private final VBox todayTasksContainer = new VBox(8);
    private final VBox recommendedTasksContainer = new VBox(8);
    private final VBox upcomingDeadlinesContainer = new VBox(8);
    private final VBox recentCompletedContainer = new VBox(8);
    private final Label productivitySummaryLabel = new Label();

    // Charts
    private final BarChart<String, Number> weeklyBarChart;
    private final PieChart distributionPieChart;

    public DashboardView(TaskService taskService,
                         AnalyticsService analyticsService,
                         Consumer<Task> onSelectTask,
                         Consumer<Task> onToggleComplete,
                         Consumer<Task> onToggleImportant,
                         Consumer<Task> onToggleMyDay,
                         Consumer<Task> onDeleteTask) {
        this.taskService = taskService;
        this.analyticsService = analyticsService;
        this.onSelectTask = onSelectTask;
        this.onToggleComplete = onToggleComplete;
        this.onToggleImportant = onToggleImportant;
        this.onToggleMyDay = onToggleMyDay;
        this.onDeleteTask = onDeleteTask;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));

        // Header
        Label headerTitle = new Label("Productivity Dashboard");
        headerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        Label headerSubtitle = new Label("Real-time intelligent overview of your daily workload, deadlines, and focus.");
        headerSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");
        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);

        // Section A: Overview Stat Cards (FlowPane or TilePane)
        FlowPane statsPane = new FlowPane();
        statsPane.setHgap(16);
        statsPane.setVgap(16);
        statsPane.getChildren().addAll(activeCard, completedCard, todayCard, overdueCard, focusCard, weeklyRateCard);

        // Section H: Productivity Summary Banner
        VBox summaryCard = new VBox(8);
        summaryCard.getStyleClass().add("card");
        Label summaryTitle = new Label("📊 Daily Productivity Pulse");
        summaryTitle.getStyleClass().add("card-title");
        productivitySummaryLabel.setWrapText(true);
        productivitySummaryLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 13px;");
        summaryCard.getChildren().addAll(summaryTitle, productivitySummaryLabel);

        // Section D: Smart Recommended Tasks
        VBox recCard = new VBox(12);
        recCard.getStyleClass().add("card");
        Label recTitle = new Label("⚡ Smart Priority Recommendations");
        recTitle.getStyleClass().add("card-title");
        Label recSub = new Label("Algorithmic prioritization combining importance, urgency, proximity, and overdue status.");
        recSub.getStyleClass().add("card-subtitle");
        recCard.getChildren().addAll(recTitle, recSub, recommendedTasksContainer);

        // Two-column layout for Today & Upcoming
        HBox middleRow = new HBox(16);
        HBox.setHgrow(middleRow, Priority.ALWAYS);

        // Section B: Today's Tasks
        VBox todayCardBox = new VBox(12);
        todayCardBox.getStyleClass().add("card");
        HBox.setHgrow(todayCardBox, Priority.ALWAYS);
        Label todayTitle = new Label("📅 Today's Focus List");
        todayTitle.getStyleClass().add("card-title");
        todayCardBox.getChildren().addAll(todayTitle, todayTasksContainer);

        // Section C: Upcoming Deadlines
        VBox upcomingCardBox = new VBox(12);
        upcomingCardBox.getStyleClass().add("card");
        HBox.setHgrow(upcomingCardBox, Priority.ALWAYS);
        Label upcomingTitle = new Label("⏰ Upcoming Deadlines (Next 7 Days)");
        upcomingTitle.getStyleClass().add("card-title");
        upcomingCardBox.getChildren().addAll(upcomingTitle, upcomingDeadlinesContainer);

        middleRow.getChildren().addAll(todayCardBox, upcomingCardBox);

        // Charts Section (Weekly Productivity & Distribution)
        HBox chartsRow = new HBox(16);
        HBox.setHgrow(chartsRow, Priority.ALWAYS);

        // Weekly Bar Chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        weeklyBarChart = new BarChart<>(xAxis, yAxis);
        weeklyBarChart.setTitle("Completed Tasks (Last 7 Days)");
        weeklyBarChart.setLegendVisible(false);
        weeklyBarChart.setPrefHeight(260);

        VBox weeklyChartBox = new VBox(8, weeklyBarChart);
        weeklyChartBox.getStyleClass().add("card");
        HBox.setHgrow(weeklyChartBox, Priority.ALWAYS);

        // Task Distribution Pie Chart
        distributionPieChart = new PieChart();
        distributionPieChart.setTitle("Active Tasks by Project");
        distributionPieChart.setPrefHeight(260);

        VBox pieChartBox = new VBox(8, distributionPieChart);
        pieChartBox.getStyleClass().add("card");
        HBox.setHgrow(pieChartBox, Priority.ALWAYS);

        chartsRow.getChildren().addAll(weeklyChartBox, pieChartBox);

        // Section G: Recent Completed Tasks
        VBox recentCard = new VBox(12);
        recentCard.getStyleClass().add("card");
        Label recentTitle = new Label("✓ Recently Completed");
        recentTitle.getStyleClass().add("card-title");
        recentCard.getChildren().addAll(recentTitle, recentCompletedContainer);

        content.getChildren().addAll(
                headerBox,
                statsPane,
                summaryCard,
                recCard,
                middleRow,
                chartsRow,
                recentCard
        );

        setContent(content);
        refresh();
    }

    public void refresh() {
        try {
            AnalyticsService.AnalyticsData data = analyticsService.getAnalyticsData();

            // Update Stat Cards
            activeCard.setValue(String.valueOf(data.activeTasks));
            completedCard.setValue(String.valueOf(data.completedTasks));
            todayCard.setValue(String.valueOf(data.dueTodayTasks));
            overdueCard.setValue(String.valueOf(data.overdueTasks));
            focusCard.setValue(data.todayFocusMinutes + " min");
            weeklyRateCard.setValue(String.format("%.0f%%", data.completionPercentage));

            // Productivity Summary
            int totalActive = data.activeTasks;
            int overdue = data.overdueTasks;
            int completedToday = data.dailyCompleted;
            String summaryText = String.format(
                    "You have %d active task%s (%d due today%s). " +
                    "So far today, you have completed %d task%s and logged %d minutes in Focus Mode. " +
                    "Weekly completion rate is at %.1f%% across projects.",
                    totalActive, totalActive == 1 ? "" : "s",
                    data.dueTodayTasks, overdue > 0 ? ", " + overdue + " overdue" : "",
                    completedToday, completedToday == 1 ? "" : "s",
                    data.todayFocusMinutes,
                    data.completionPercentage
            );
            productivitySummaryLabel.setText(summaryText);

            // Populate Smart Recommendations (Section D)
            List<Task> recommendations = taskService.getRecommendedTasks(5);
            recommendedTasksContainer.getChildren().clear();
            if (recommendations.isEmpty()) {
                Label emptyLabel = new Label("✨ No active tasks require immediate action. Enjoy your clear schedule or add a new task!");
                emptyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 8;");
                recommendedTasksContainer.getChildren().add(emptyLabel);
            } else {
                for (Task t : recommendations) {
                    recommendedTasksContainer.getChildren().add(new TaskCard(
                            t, onSelectTask, onToggleComplete, onToggleImportant, onToggleMyDay, onDeleteTask
                    ));
                }
            }

            // Populate Today's Tasks (Section B)
            List<Task> todayTasks = taskService.getTodayTasks();
            todayTasksContainer.getChildren().clear();
            if (todayTasks.isEmpty()) {
                Label empty = new Label("No tasks scheduled for today. Click '+ New Task' or add from your Inbox.");
                empty.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 8;");
                todayTasksContainer.getChildren().add(empty);
            } else {
                for (Task t : todayTasks) {
                    todayTasksContainer.getChildren().add(new TaskCard(
                            t, onSelectTask, onToggleComplete, onToggleImportant, onToggleMyDay, onDeleteTask
                    ));
                }
            }

            // Populate Upcoming Deadlines (Section C)
            List<Task> upcomingTasks = taskService.getUpcomingTasks();
            upcomingDeadlinesContainer.getChildren().clear();
            if (upcomingTasks.isEmpty()) {
                Label empty = new Label("No upcoming deadlines in the next 7 days.");
                empty.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 8;");
                upcomingDeadlinesContainer.getChildren().add(empty);
            } else {
                for (Task t : upcomingTasks) {
                    upcomingDeadlinesContainer.getChildren().add(new TaskCard(
                            t, onSelectTask, onToggleComplete, onToggleImportant, onToggleMyDay, onDeleteTask
                    ));
                }
            }

            // Populate Recent Completed (Section G)
            List<Task> completed = taskService.getRecentCompleted(4);
            recentCompletedContainer.getChildren().clear();
            if (completed.isEmpty()) {
                Label empty = new Label("Completed tasks will appear here.");
                empty.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 8;");
                recentCompletedContainer.getChildren().add(empty);
            } else {
                for (Task t : completed) {
                    recentCompletedContainer.getChildren().add(new TaskCard(
                            t, onSelectTask, onToggleComplete, onToggleImportant, onToggleMyDay, onDeleteTask
                    ));
                }
            }

            // Weekly Bar Chart
            weeklyBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE dd");
            data.dailyCompletionHistory.forEach((date, count) -> {
                series.getData().add(new XYChart.Data<>(date.format(dayFmt), count));
            });
            weeklyBarChart.getData().add(series);

            // Distribution Pie Chart
            distributionPieChart.getData().clear();
            if (data.projectDistribution.isEmpty()) {
                distributionPieChart.getData().add(new PieChart.Data("No Projects", 1));
            } else {
                data.projectDistribution.forEach((name, count) -> {
                    if (count > 0) {
                        distributionPieChart.getData().add(new PieChart.Data(name + " (" + count + ")", count));
                    }
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
