package com.smarttodo;

import com.smarttodo.database.DatabaseManager;
import com.smarttodo.model.FocusSession;
import com.smarttodo.repository.FocusSessionRepository;
import com.smarttodo.service.PomodoroService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PomodoroServiceTest {
    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private FocusSessionRepository repository;
    private PomodoroService pomodoroService;

    @BeforeEach
    void setUp() {
        File dbFile = tempDir.resolve("test_pomodoro.db").toFile();
        dbManager = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath());
        repository = new FocusSessionRepository(dbManager);
        pomodoroService = new PomodoroService(repository);
    }

    @AfterEach
    void tearDown() {
        pomodoroService.shutdown();
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Pomodoro timer initializes in IDLE state with default 25 minutes")
    void testInitialState() {
        assertEquals(PomodoroService.State.IDLE, pomodoroService.getCurrentState());
        assertEquals(FocusSession.SessionType.FOCUS, pomodoroService.getCurrentType());
        assertEquals(25 * 60, pomodoroService.getRemainingSeconds());
    }

    @Test
    @DisplayName("Durations can be customized and reset correctly")
    void testCustomDurations() {
        pomodoroService.setDurations(50, 10, 30);
        assertEquals(50 * 60, pomodoroService.getRemainingSeconds());
    }

    @Test
    @DisplayName("Skip break transitions back to FOCUS session")
    void testSkipBreak() {
        pomodoroService.skipBreak();
        assertEquals(FocusSession.SessionType.FOCUS, pomodoroService.getCurrentType());
        assertEquals(PomodoroService.State.IDLE, pomodoroService.getCurrentState());
    }
}
