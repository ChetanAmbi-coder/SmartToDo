package com.smarttodo.view;

import com.smarttodo.components.StatCard;
import com.smarttodo.service.AnalyticsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class AnalyticsView extends ScrollPane {
    private final AnalyticsService analyticsService;

    // Metric Cards
    private final StatCard dailyCard = new StatCard("TODAY COMPLETED", "0", "Finished today", "#10B981");
    private final StatCard weeklyCard = new StatCard("WEEKLY COMPLETED", "0", "Since Monday", "#3B82F6");
    private final StatCard monthlyCard = new StatCard("MONTHLY COMPLETED", "0", "Current month", "#8B5CF6");
    private final StatCard avgDailyCard = new StatCard("7-DAY AVERAGE", "0.0", "Tasks / day", "#06B6D4");
    private final StatCard totalFocusCard = new StatCard("TOTAL FOCUS TIME", "0 hrs", "Pomodoro tracked", "#F59E0B");
    private final StatCard overdueCard = new StatCard("OVERDUE TASKS", "0", "Attention required", "#EF4444");

    // Charts
    private final BarChart<String, Number> trendBarChart;
    private final PieChart projectPieChart;
    private final PieChart priorityPieChart;

    public AnalyticsView(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));

        // Header
        Label headerTitle = new Label("Productivity Analytics");
        headerTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        Label headerSubtitle = new Label("Deep quantitative insights into execution velocity, completion rates, and focus dedication.");
        headerSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");
        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);

        // Metrics Grid
        FlowPane metricsPane = new FlowPane();
        metricsPane.setHgap(16);
        metricsPane.setVgap(16);
        metricsPane.getChildren().addAll(dailyCard, weeklyCard, monthlyCard, avgDailyCard, totalFocusCard, overdueCard);

        // Chart 1: 7-Day Completion Trend Bar Chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        trendBarChart = new BarChart<>(xAxis, yAxis);
        trendBarChart.setTitle("Task Completions Over Past 7 Days");
        trendBarChart.setLegendVisible(false);
        trendBarChart.setPrefHeight(280);

        VBox trendBox = new VBox(8, trendBarChart);
        trendBox.getStyleClass().add("card");

        // Charts Row: Project Distribution & Priority Breakdown
        HBox pieRow = new HBox(16);
        HBox.setHgrow(pieRow, Priority.ALWAYS);

        projectPieChart = new PieChart();
        projectPieChart.setTitle("Task Distribution by Project");
        projectPieChart.setPrefHeight(280);
        VBox projBox = new VBox(8, projectPieChart);
        projBox.getStyleClass().add("card");
        HBox.setHgrow(projBox, Priority.ALWAYS);

        priorityPieChart = new PieChart();
        priorityPieChart.setTitle("Active Tasks by Priority");
        priorityPieChart.setPrefHeight(280);
        VBox prioBox = new VBox(8, priorityPieChart);
        prioBox.getStyleClass().add("card");
        HBox.setHgrow(prioBox, Priority.ALWAYS);

        pieRow.getChildren().addAll(projBox, prioBox);

        content.getChildren().addAll(headerBox, metricsPane, trendBox, pieRow);
        setContent(content);

        refresh();
    }

    public void refresh() {
        try {
            AnalyticsService.AnalyticsData data = analyticsService.getAnalyticsData();

            dailyCard.setValue(String.valueOf(data.dailyCompleted));
            weeklyCard.setValue(String.valueOf(data.weeklyCompleted));
            monthlyCard.setValue(String.valueOf(data.monthlyCompleted));
            avgDailyCard.setValue(String.format("%.1f", data.averageDailyCompleted));

            int focusHours = data.totalFocusMinutes / 60;
            int focusMins = data.totalFocusMinutes % 60;
            totalFocusCard.setValue(focusHours > 0 ? focusHours + "h " + focusMins + "m" : focusMins + "m");
            overdueCard.setValue(String.valueOf(data.overdueTasks));

            // Trend Chart
            trendBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE (MMM dd)");
            data.dailyCompletionHistory.forEach((date, count) -> {
                series.getData().add(new XYChart.Data<>(date.format(dayFmt), count));
            });
            trendBarChart.getData().add(series);

            // Project Pie Chart
            projectPieChart.getData().clear();
            if (data.projectDistribution.isEmpty()) {
                projectPieChart.getData().add(new PieChart.Data("No projects", 1));
            } else {
                data.projectDistribution.forEach((name, count) -> {
                    if (count > 0) {
                        projectPieChart.getData().add(new PieChart.Data(name, count));
                    }
                });
            }

            // Priority Pie Chart
            priorityPieChart.getData().clear();
            if (data.priorityDistribution.isEmpty()) {
                priorityPieChart.getData().add(new PieChart.Data("No active tasks", 1));
            } else {
                data.priorityDistribution.forEach((prio, count) -> {
                    if (count > 0) {
                        priorityPieChart.getData().add(new PieChart.Data(prio, count));
                    }
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
