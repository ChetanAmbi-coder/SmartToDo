package com.smarttodo.service;

import com.smarttodo.model.FocusSession;
import com.smarttodo.repository.FocusSessionRepository;
import javafx.application.Platform;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class PomodoroService {
    public enum State {
        IDLE,
        RUNNING,
        PAUSED
    }

    public interface PomodoroListener {
        void onTick(int remainingSeconds, double progress);
        void onStateChanged(State state, FocusSession.SessionType sessionType);
        void onSessionCompleted(FocusSession session);
    }

    private final FocusSessionRepository repository;
    private final List<PomodoroListener> listeners = new CopyOnWriteArrayList<>();

    private int focusDuration = 25 * 60;
    private int shortBreakDuration = 5 * 60;
    private int longBreakDuration = 15 * 60;
    private int longBreakInterval = 4;

    private int completedFocusCount = 0;
    private FocusSession.SessionType currentType = FocusSession.SessionType.FOCUS;
    private State currentState = State.IDLE;
    private int remainingSeconds = focusDuration;

    private Long selectedTaskId;
    private String currentNotes;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerHandle;

    public PomodoroService(FocusSessionRepository repository) {
        this.repository = repository;
    }

    public void addListener(PomodoroListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PomodoroListener listener) {
        listeners.remove(listener);
    }

    public void setDurations(int focusMins, int shortBreakMins, int longBreakMins) {
        this.focusDuration = focusMins * 60;
        this.shortBreakDuration = shortBreakMins * 60;
        this.longBreakDuration = longBreakMins * 60;
        if (currentState == State.IDLE) {
            reset();
        }
    }

    public synchronized void start() {
        if (currentState == State.RUNNING) return;

        if (currentState == State.IDLE) {
            remainingSeconds = getTargetDuration();
        }

        currentState = State.RUNNING;
        notifyState();

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PomodoroTimerThread");
                t.setDaemon(true);
                return t;
            });
        }

        timerHandle = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized void pause() {
        if (currentState != State.RUNNING) return;

        if (timerHandle != null) {
            timerHandle.cancel(false);
        }
        currentState = State.PAUSED;
        notifyState();
    }

    public synchronized void resume() {
        if (currentState == State.PAUSED) {
            start();
        }
    }

    public synchronized void reset() {
        if (timerHandle != null) {
            timerHandle.cancel(false);
        }
        currentState = State.IDLE;
        remainingSeconds = getTargetDuration();
        notifyState();
        notifyTick();
    }

    public synchronized void skipBreak() {
        if (timerHandle != null) {
            timerHandle.cancel(false);
        }
        currentType = FocusSession.SessionType.FOCUS;
        currentState = State.IDLE;
        remainingSeconds = focusDuration;
        notifyState();
        notifyTick();
    }

    private void tick() {
        synchronized (this) {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                notifyTick();
            } else {
                handleSessionComplete();
            }
        }
    }

    private void handleSessionComplete() {
        if (timerHandle != null) {
            timerHandle.cancel(false);
        }

        int durationMins = getTargetDuration() / 60;
        FocusSession session = new FocusSession(
                selectedTaskId,
                durationMins,
                currentType
        );
        session.setCompletedAt(LocalDateTime.now());
        session.setNotes(currentNotes);

        try {
            repository.create(session);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (currentType == FocusSession.SessionType.FOCUS) {
            completedFocusCount++;
            if (completedFocusCount % longBreakInterval == 0) {
                currentType = FocusSession.SessionType.LONG_BREAK;
            } else {
                currentType = FocusSession.SessionType.SHORT_BREAK;
            }
        } else {
            currentType = FocusSession.SessionType.FOCUS;
        }

        currentState = State.IDLE;
        remainingSeconds = getTargetDuration();

        safeRunLater(() -> {
            for (PomodoroListener l : listeners) {
                l.onSessionCompleted(session);
                l.onStateChanged(currentState, currentType);
                l.onTick(remainingSeconds, 1.0);
            }
        });
    }

    private int getTargetDuration() {
        return switch (currentType) {
            case FOCUS -> focusDuration;
            case SHORT_BREAK -> shortBreakDuration;
            case LONG_BREAK -> longBreakDuration;
        };
    }

    private void notifyTick() {
        int target = getTargetDuration();
        double progress = target > 0 ? (double) (target - remainingSeconds) / target : 0.0;
        int currentRemaining = remainingSeconds;
        safeRunLater(() -> {
            for (PomodoroListener l : listeners) {
                l.onTick(currentRemaining, progress);
            }
        });
    }

    private void notifyState() {
        State state = currentState;
        FocusSession.SessionType type = currentType;
        safeRunLater(() -> {
            for (PomodoroListener l : listeners) {
                l.onStateChanged(state, type);
            }
        });
    }

    private void safeRunLater(Runnable action) {
        try {
            Platform.runLater(action);
        } catch (IllegalStateException e) {
            action.run();
        }
    }

    public synchronized void setSelectedTaskId(Long taskId) {
        this.selectedTaskId = taskId;
    }

    public Long getSelectedTaskId() {
        return selectedTaskId;
    }

    public synchronized void setCurrentNotes(String currentNotes) {
        this.currentNotes = currentNotes;
    }

    public State getCurrentState() {
        return currentState;
    }

    public FocusSession.SessionType getCurrentType() {
        return currentType;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getCompletedFocusCount() {
        return completedFocusCount;
    }

    public void shutdown() {
        if (timerHandle != null) {
            timerHandle.cancel(true);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
