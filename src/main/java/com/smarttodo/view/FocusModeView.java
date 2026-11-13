package com.smarttodo.view;

import com.smarttodo.components.CircularTimerView;
import com.smarttodo.model.FocusSession;
import com.smarttodo.model.Task;
import com.smarttodo.repository.FocusSessionRepository;
import com.smarttodo.service.PomodoroService;
import com.smarttodo.service.TaskService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FocusModeView extends ScrollPane implements PomodoroService.PomodoroListener {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final PomodoroService pomodoroService;
    private final FocusSessionRepository focusSessionRepository;
    private final TaskService taskService;

    private final CircularTimerView circularTimer = new CircularTimerView(240);
    private final Button startBtn = new Button("Start Focus");
    private final Button pauseBtn = new Button("Pause");
    private final Button resetBtn = new Button("Reset");
    private final Button skipBtn = new Button("Skip Break");

    private final ComboBox<Task> taskSelector = new ComboBox<>();
    private final TextField sessionNotes = new TextField();

    private final Label todayFocusMinutesLabel = new Label("0 mins");
    private final Label sessionCountDots = new Label("Cycle: 0 / 4 completed");
    private final VBox sessionHistoryContainer = new VBox(8);

    public FocusModeView(PomodoroService pomodoroService,
                         FocusSessionRepository focusSessionRepository,
                         TaskService taskService) {
        this.pomodoroService = pomodoroService;
        this.focusSessionRepository = focusSessionRepository;
        this.taskService = taskService;

        pomodoroService.addListener(this);

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);

        // Header
        Label headerTitle = new Label("Focus Mode — Pomodoro Timer");
        headerTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");
        Label headerSubtitle = new Label("Structure your deep work with 25-minute focus intervals and restorative breaks.");
        headerSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");
        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);
        headerBox.setAlignment(Pos.CENTER);

        // Main Timer Card
        VBox timerCard = new VBox(20);
        timerCard.getStyleClass().add("card");
        timerCard.setAlignment(Pos.CENTER);
        timerCard.setMaxWidth(560);
        timerCard.setPadding(new Insets(30));

        sessionCountDots.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-font-size: 13px;");

        // Action Buttons Row
        HBox controlsRow = new HBox(12);
        controlsRow.setAlignment(Pos.CENTER);

        startBtn.getStyleClass().add("btn-primary");
        startBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 24 10 24;");
        startBtn.setOnAction(e -> {
            if (pomodoroService.getCurrentState() == PomodoroService.State.PAUSED) {
                pomodoroService.resume();
            } else {
                pomodoroService.start();
            }
        });

        pauseBtn.getStyleClass().add("btn-secondary");
        pauseBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20 10 20;");
        pauseBtn.setOnAction(e -> pomodoroService.pause());

        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20 10 20;");
        resetBtn.setOnAction(e -> pomodoroService.reset());

        skipBtn.getStyleClass().add("btn-secondary");
        skipBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20 10 20;");
        skipBtn.setOnAction(e -> pomodoroService.skipBreak());

        controlsRow.getChildren().addAll(startBtn, pauseBtn, resetBtn, skipBtn);

        // Task association & notes row
        VBox taskAssocBox = new VBox(10);
        taskAssocBox.setAlignment(Pos.CENTER_LEFT);
        taskAssocBox.setStyle("-fx-background-color: -color-bg-input; -fx-padding: 14; -fx-background-radius: 8px;");

        Label taskLabel = new Label("Target Task (Optional):");
        taskLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        taskSelector.setMaxWidth(Double.MAX_VALUE);
        taskSelector.setPromptText("Select a task to associate focus time...");
        taskSelector.setOnAction(e -> {
            Task selected = taskSelector.getValue();
            pomodoroService.setSelectedTaskId(selected != null && selected.getId() != null ? selected.getId() : null);
        });

        sessionNotes.setPromptText("What are you working on in this session?");
        sessionNotes.textProperty().addListener((obs, oldVal, newVal) -> pomodoroService.setCurrentNotes(newVal));

        taskAssocBox.getChildren().addAll(taskLabel, taskSelector, sessionNotes);

        timerCard.getChildren().addAll(sessionCountDots, circularTimer, controlsRow, taskAssocBox);

        // History Card (Today's Completed Focus Sessions)
        VBox historyCard = new VBox(12);
        historyCard.getStyleClass().add("card");
        historyCard.setMaxWidth(560);

        HBox histHeader = new HBox(12);
        histHeader.setAlignment(Pos.CENTER_LEFT);
        Label histTitle = new Label("Today's Focus Log");
        histTitle.getStyleClass().add("card-title");
        HBox.setHgrow(histTitle, Priority.ALWAYS);

        todayFocusMinutesLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-accent; -fx-font-size: 13px;");
        histHeader.getChildren().addAll(histTitle, todayFocusMinutesLabel);

        historyCard.getChildren().addAll(histHeader, sessionHistoryContainer);

        content.getChildren().addAll(headerBox, timerCard, historyCard);
        setContent(content);

        refresh();
        updateButtonStates(pomodoroService.getCurrentState());
    }

    public void refresh() {
        populateActiveTasks();
        refreshHistory();
    }

    private void populateActiveTasks() {
        try {
            List<Task> tasks = new ArrayList<>();
            Task none = new Task("No specific task");
            none.setId(null);
            tasks.add(none);
            tasks.addAll(taskService.getActiveTasks());
            taskSelector.setItems(FXCollections.observableArrayList(tasks));
            taskSelector.getSelectionModel().select(0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshHistory() {
        try {
            int mins = focusSessionRepository.getTodayFocusMinutes();
            todayFocusMinutesLabel.setText("Total: " + mins + " minutes");

            sessionCountDots.setText(String.format("Focus Cycle: %d / 4 completed",
                    pomodoroService.getCompletedFocusCount() % 4));

            List<FocusSession> recent = focusSessionRepository.findRecent(6);
            sessionHistoryContainer.getChildren().clear();

            if (recent.isEmpty()) {
                Label empty = new Label("No focus sessions completed today. Start your first session above!");
                empty.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 8;");
                sessionHistoryContainer.getChildren().add(empty);
            } else {
                for (FocusSession s : recent) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 8, 6, 8));

                    Label typePill = new Label(s.getSessionType().getDisplayName());
                    typePill.getStyleClass().add("badge");
                    typePill.setStyle("-fx-background-color: " + s.getSessionType().getColorHex() + "33; -fx-text-fill: " + s.getSessionType().getColorHex() + ";");

                    Label timeLbl = new Label(s.getDurationMinutes() + " mins • " +
                            (s.getCompletedAt() != null ? s.getCompletedAt().format(TIME_FMT) : ""));
                    timeLbl.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px;");

                    Label taskLbl = new Label(s.getTaskTitle() != null ? s.getTaskTitle() :
                            (s.getNotes() != null && !s.getNotes().isEmpty() ? s.getNotes() : "General Focus"));
                    taskLbl.setStyle("-fx-font-weight: 500; -fx-text-fill: -color-text-primary;");
                    HBox.setHgrow(taskLbl, Priority.ALWAYS);

                    row.getChildren().addAll(typePill, taskLbl, timeLbl);
                    sessionHistoryContainer.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateButtonStates(PomodoroService.State state) {
        switch (state) {
            case IDLE -> {
                startBtn.setText("Start Focus");
                startBtn.setDisable(false);
                pauseBtn.setDisable(true);
                resetBtn.setDisable(true);
            }
            case RUNNING -> {
                startBtn.setDisable(true);
                pauseBtn.setDisable(false);
                resetBtn.setDisable(false);
            }
            case PAUSED -> {
                startBtn.setText("Resume");
                startBtn.setDisable(false);
                pauseBtn.setDisable(true);
                resetBtn.setDisable(false);
            }
        }
    }

    @Override
    public void onTick(int remainingSeconds, double progress) {
        circularTimer.update(remainingSeconds, progress, pomodoroService.getCurrentType());
    }

    @Override
    public void onStateChanged(PomodoroService.State state, FocusSession.SessionType sessionType) {
        updateButtonStates(state);
        sessionCountDots.setText(String.format("Focus Cycle: %d / 4 completed",
                pomodoroService.getCompletedFocusCount() % 4));
    }

    @Override
    public void onSessionCompleted(FocusSession session) {
        refreshHistory();
    }
}
