package com.smarttodo.view;

import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.service.TaskService;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class CalendarView extends BorderPane {
    public enum CalendarMode {
        MONTH,
        WEEK,
        DAY
    }

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter WEEK_FMT = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final TaskService taskService;
    private final Consumer<Task> onSelectTask;

    private CalendarMode currentMode = CalendarMode.MONTH;
    private LocalDate selectedDate = LocalDate.now();

    private final Label currentPeriodLabel = new Label();
    private final ToggleGroup modeToggleGroup = new ToggleGroup();
    private final ToggleButton monthToggle = new ToggleButton("Month");
    private final ToggleButton weekToggle = new ToggleButton("Week");
    private final ToggleButton dayToggle = new ToggleButton("Day");

    private final StackPane calendarContent = new StackPane();

    public CalendarView(TaskService taskService, Consumer<Task> onSelectTask) {
        this.taskService = taskService;
        this.onSelectTask = onSelectTask;

        setPadding(new Insets(24));

        // Top Navigation Bar
        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(0, 0, 16, 0));

        // Left: Prev, Next, Today, Period Label
        HBox navBox = new HBox(10);
        navBox.setAlignment(Pos.CENTER_LEFT);

        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("btn-secondary");
        prevBtn.setOnAction(e -> navigate(-1));

        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("btn-secondary");
        nextBtn.setOnAction(e -> navigate(1));

        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("btn-secondary");
        todayBtn.setOnAction(e -> {
            selectedDate = LocalDate.now();
            refresh();
        });

        currentPeriodLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");

        navBox.getChildren().addAll(prevBtn, nextBtn, todayBtn, currentPeriodLabel);
        topBar.setLeft(navBox);

        // Right: Mode Switcher (Month / Week / Day)
        HBox modeBox = new HBox(0);
        modeBox.setAlignment(Pos.CENTER_RIGHT);

        monthToggle.setToggleGroup(modeToggleGroup);
        weekToggle.setToggleGroup(modeToggleGroup);
        dayToggle.setToggleGroup(modeToggleGroup);
        monthToggle.setSelected(true);

        monthToggle.getStyleClass().add("btn-secondary");
        weekToggle.getStyleClass().add("btn-secondary");
        dayToggle.getStyleClass().add("btn-secondary");

        modeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == monthToggle) currentMode = CalendarMode.MONTH;
            else if (newVal == weekToggle) currentMode = CalendarMode.WEEK;
            else if (newVal == dayToggle) currentMode = CalendarMode.DAY;
            refresh();
        });

        modeBox.getChildren().addAll(monthToggle, weekToggle, dayToggle);
        topBar.setRight(modeBox);

        setTop(topBar);
        setCenter(calendarContent);

        refresh();
    }

    private void navigate(int delta) {
        selectedDate = switch (currentMode) {
            case MONTH -> selectedDate.plusMonths(delta);
            case WEEK -> selectedDate.plusWeeks(delta);
            case DAY -> selectedDate.plusDays(delta);
        };
        refresh();
    }

    public void refresh() {
        calendarContent.getChildren().clear();

        try {
            List<Task> allTasks = taskService.getAllTasks();

            switch (currentMode) {
                case MONTH -> renderMonthView(allTasks);
                case WEEK -> renderWeekView(allTasks);
                case DAY -> renderDayView(allTasks);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderMonthView(List<Task> allTasks) {
        YearMonth ym = YearMonth.from(selectedDate);
        currentPeriodLabel.setText(ym.format(MONTH_FMT));

        VBox monthBox = new VBox(8);

        // Days of week header
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(8);
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label dayLbl = new Label(dayNames[i]);
            dayLbl.setAlignment(Pos.CENTER);
            dayLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-padding: 4;");
            GridPane.setHgrow(dayLbl, Priority.ALWAYS);
            GridPane.setHalignment(dayLbl, HPos.CENTER);
            headerGrid.add(dayLbl, i, 0);
        }

        // Days Grid
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        VBox.setVgrow(grid, Priority.ALWAYS);

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }

        LocalDate firstOfMonth = ym.atDay(1);
        int dayOfWeekVal = firstOfMonth.getDayOfWeek().getValue(); // 1 = Mon, 7 = Sun
        int startOffset = dayOfWeekVal - 1;
        int daysInMonth = ym.lengthOfMonth();

        LocalDate startCalendarDate = firstOfMonth.minusDays(startOffset);

        for (int row = 0; row < 6; row++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / 6.0);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);

            for (int col = 0; col < 7; col++) {
                int dayIndex = (row * 7) + col;
                LocalDate cellDate = startCalendarDate.plusDays(dayIndex);

                VBox cell = createMonthDayCell(cellDate, ym.getMonthValue(), allTasks);
                grid.add(cell, col, row);
            }
        }

        monthBox.getChildren().addAll(headerGrid, grid);
        calendarContent.getChildren().add(monthBox);
    }

    private VBox createMonthDayCell(LocalDate date, int currentMonth, List<Task> allTasks) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("card");
        cell.setPadding(new Insets(6));
        cell.setMinHeight(85);

        boolean isToday = date.isEqual(LocalDate.now());
        boolean isCurrentMonth = date.getMonthValue() == currentMonth;

        HBox topRow = new HBox();
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        if (isToday) {
            dayNum.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-background-radius: 10px; -fx-padding: 1 6 1 6; -fx-font-weight: bold;");
        } else if (isCurrentMonth) {
            dayNum.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        } else {
            dayNum.setStyle("-fx-text-fill: -color-text-muted;");
        }
        topRow.getChildren().add(dayNum);
        cell.getChildren().add(topRow);

        // Add tasks for this date
        ScrollPane taskScroll = new ScrollPane();
        taskScroll.setFitToWidth(true);
        taskScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox taskList = new VBox(3);
        for (Task t : allTasks) {
            if (t.getDueDate() != null && t.getDueDate().isEqual(date)) {
                Label taskPill = new Label((t.getDueTime() != null ? t.getDueTime().format(TIME_FMT) + " " : "") + t.getTitle());
                taskPill.setMaxWidth(Double.MAX_VALUE);
                taskPill.setStyle(getCalendarPillStyle(t));
                taskPill.setOnMouseClicked(e -> {
                    e.consume();
                    if (onSelectTask != null) onSelectTask.accept(t);
                });
                taskList.getChildren().add(taskPill);
            }
        }
        taskScroll.setContent(taskList);
        VBox.setVgrow(taskScroll, Priority.ALWAYS);
        cell.getChildren().add(taskScroll);

        return cell;
    }

    private void renderWeekView(List<Task> allTasks) {
        LocalDate startOfWeek = selectedDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        currentPeriodLabel.setText(startOfWeek.format(WEEK_FMT) + " – " + endOfWeek.format(WEEK_FMT) + ", " + startOfWeek.getYear());

        HBox weekBox = new HBox(8);
        HBox.setHgrow(weekBox, Priority.ALWAYS);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            VBox dayColumn = new VBox(8);
            dayColumn.getStyleClass().add("card");
            dayColumn.setPadding(new Insets(10));
            HBox.setHgrow(dayColumn, Priority.ALWAYS);

            boolean isToday = date.isEqual(LocalDate.now());

            Label dayTitle = new Label(date.format(DateTimeFormatter.ofPattern("EEE dd")));
            if (isToday) {
                dayTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");
            } else {
                dayTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
            }

            VBox taskList = new VBox(6);
            for (Task t : allTasks) {
                if (t.getDueDate() != null && t.getDueDate().isEqual(date)) {
                    VBox taskItem = new VBox(2);
                    taskItem.setStyle(getCalendarPillStyle(t) + "; -fx-padding: 6 8 6 8;");
                    Label title = new Label(t.getTitle());
                    title.setStyle("-fx-font-weight: 600; -fx-text-fill: inherit;");
                    Label time = new Label(t.getDueTime() != null ? "⏱ " + t.getDueTime().format(TIME_FMT) : "All Day");
                    time.setStyle("-fx-font-size: 10px; -fx-text-fill: inherit; -fx-opacity: 0.8;");
                    taskItem.getChildren().addAll(title, time);
                    taskItem.setOnMouseClicked(e -> {
                        if (onSelectTask != null) onSelectTask.accept(t);
                    });
                    taskList.getChildren().add(taskItem);
                }
            }

            ScrollPane sp = new ScrollPane(taskList);
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            VBox.setVgrow(sp, Priority.ALWAYS);

            dayColumn.getChildren().addAll(dayTitle, new Separator(), sp);
            weekBox.getChildren().add(dayColumn);
        }

        calendarContent.getChildren().add(weekBox);
    }

    private void renderDayView(List<Task> allTasks) {
        currentPeriodLabel.setText(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));

        VBox dayBox = new VBox(12);
        dayBox.getStyleClass().add("card");
        dayBox.setPadding(new Insets(16));

        Label header = new Label("Tasks Scheduled For " + selectedDate.format(DateTimeFormatter.ofPattern("MMM dd")));
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");

        VBox taskList = new VBox(8);
        int count = 0;
        for (Task t : allTasks) {
            if (t.getDueDate() != null && t.getDueDate().isEqual(selectedDate)) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("card");
                row.setStyle("-fx-padding: 10 14 10 14; -fx-cursor: hand;");

                Label timeLbl = new Label(t.getDueTime() != null ? t.getDueTime().format(TIME_FMT) : "No Time");
                timeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-accent; -fx-min-width: 70px;");

                Label titleLbl = new Label(t.getTitle());
                titleLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-primary;");
                HBox.setHgrow(titleLbl, Priority.ALWAYS);

                Label statusBadge = new Label(t.getStatus().getDisplayName());
                statusBadge.getStyleClass().add("badge");
                statusBadge.setStyle("-fx-background-color: -color-bg-input; -fx-text-fill: -color-text-secondary;");

                row.getChildren().addAll(timeLbl, titleLbl, statusBadge);
                row.setOnMouseClicked(e -> {
                    if (onSelectTask != null) onSelectTask.accept(t);
                });
                taskList.getChildren().add(row);
                count++;
            }
        }

        if (count == 0) {
            Label noTasks = new Label("No tasks scheduled for this day.");
            noTasks.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 20;");
            taskList.getChildren().add(noTasks);
        }

        ScrollPane sp = new ScrollPane(taskList);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dayBox.getChildren().addAll(header, sp);
        calendarContent.getChildren().add(dayBox);
    }

    private String getCalendarPillStyle(Task t) {
        if (t.getStatus() == TaskStatus.COMPLETED) {
            return "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10B981; -fx-background-radius: 4px; -fx-padding: 2 4 2 4; -fx-font-size: 11px; -fx-cursor: hand;";
        } else if (t.isOverdue()) {
            return "-fx-background-color: rgba(239, 68, 68, 0.25); -fx-text-fill: #EF4444; -fx-background-radius: 4px; -fx-padding: 2 4 2 4; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;";
        } else {
            String color = t.getPriority() != null ? t.getPriority().getColorHex() : "#3B82F6";
            return "-fx-background-color: " + color + "33; -fx-text-fill: " + color + "; -fx-background-radius: 4px; -fx-padding: 2 4 2 4; -fx-font-size: 11px; -fx-cursor: hand;";
        }
    }
}
