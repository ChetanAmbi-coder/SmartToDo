package com.smarttodo.service;

import com.smarttodo.model.Reminder;
import com.smarttodo.repository.ReminderRepository;
import javafx.application.Platform;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ReminderSchedulerService {
    private final ReminderRepository reminderRepository;
    private final ScheduledExecutorService scheduler;
    private final Set<Long> processedReminderIds = Collections.synchronizedSet(new HashSet<>());
    private Consumer<Reminder> reminderNotificationHandler;
    private ScheduledFuture<?> handle;

    public ReminderSchedulerService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ReminderSchedulerThread");
            t.setDaemon(true);
            return t;
        });
    }

    public void setNotificationHandler(Consumer<Reminder> handler) {
        this.reminderNotificationHandler = handler;
    }

    public void start() {
        if (handle == null || handle.isCancelled()) {
            // Check every 5 seconds
            handle = scheduler.scheduleAtFixedRate(this::checkReminders, 1, 5, TimeUnit.SECONDS);
        }
    }

    public void checkReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Reminder> dueReminders = reminderRepository.findPendingReminders(now);

            for (Reminder reminder : dueReminders) {
                if (!processedReminderIds.contains(reminder.getId())) {
                    processedReminderIds.add(reminder.getId());

                    // Mark as triggered in database
                    reminderRepository.markTriggered(reminder.getId());

                    // Trigger notification on JavaFX thread
                    if (reminderNotificationHandler != null) {
                        try {
                            Platform.runLater(() -> reminderNotificationHandler.accept(reminder));
                        } catch (IllegalStateException e) {
                            reminderNotificationHandler.accept(reminder);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (handle != null) {
            handle.cancel(false);
        }
        scheduler.shutdownNow();
    }
}
