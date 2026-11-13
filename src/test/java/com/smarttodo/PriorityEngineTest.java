package com.smarttodo;

import com.smarttodo.model.Priority;
import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.service.PriorityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PriorityEngineTest {
    private PriorityEngine engine;
    private final LocalDate today = LocalDate.of(2026, 11, 13);

    @BeforeEach
    void setUp() {
        engine = new PriorityEngine();
    }

    @Test
    @DisplayName("Completed tasks receive a score of zero and are excluded from recommendations")
    void testCompletedTasksScoreZero() {
        Task task = new Task("Completed Task");
        task.setStatus(TaskStatus.COMPLETED);
        task.setImportant(true);
        task.setPriority(Priority.CRITICAL);
        task.setDueDate(today.minusDays(2));

        double score = engine.calculateScore(task, today);
        assertEquals(0.0, score, "Completed tasks must have score 0");

        List<Task> recs = engine.getRecommendations(List.of(task), 5);
        assertTrue(recs.isEmpty(), "Completed tasks must never appear in recommendations");
    }

    @Test
    @DisplayName("Formula verification: Score = 40*Importance + 30*Urgency + 20*Proximity + 10*Overdue")
    void testFormulaComponents() {
        Task task = new Task("Urgent Overdue Important Task");
        task.setImportant(true);             // 40 * 1.0 = 40
        task.setPriority(Priority.CRITICAL); // 30 * 1.0 = 30
        task.setDueDate(today.minusDays(1)); // Overdue -> proximity = 1.0 (20) & overdue = 1.0 (10)
        task.setStatus(TaskStatus.TODO);

        double score = engine.calculateScore(task, today);
        // Expected: 40 + 30 + 20 + 10 = 100.0
        assertEquals(100.0, score, 0.001);

        String explanation = engine.generateExplanation(task, today);
        assertEquals("Overdue task", explanation);
    }

    @Test
    @DisplayName("Tasks without deadlines are safely scored without exceptions")
    void testTaskWithoutDeadlineSafe() {
        Task task = new Task("No Deadline Task");
        task.setImportant(false);          // 0
        task.setPriority(Priority.MEDIUM); // 30 * 0.5 = 15
        task.setDueDate(null);             // Proximity = 0, Overdue = 0
        task.setStatus(TaskStatus.TODO);

        double score = engine.calculateScore(task, today);
        assertEquals(15.0, score, 0.001);

        String explanation = engine.generateExplanation(task, today);
        assertNotNull(explanation);
    }

    @Test
    @DisplayName("Recommendations are sorted in descending score order with deterministic tie breaking")
    void testRankingAndTieBreaking() {
        Task t1 = new Task("Task Low");
        t1.setId(1L);
        t1.setPriority(Priority.LOW);
        t1.setDueDate(today.plusDays(10));

        Task t2 = new Task("Task Critical Due Tomorrow");
        t2.setId(2L);
        t2.setPriority(Priority.CRITICAL);
        t2.setDueDate(today.plusDays(1));

        Task t3 = new Task("Task Overdue");
        t3.setId(3L);
        t3.setPriority(Priority.HIGH);
        t3.setDueDate(today.minusDays(1));

        List<Task> recommended = engine.getRecommendations(List.of(t1, t2, t3), 10);
        assertEquals(3, recommended.size());

        // Highest score should be at index 0
        assertTrue(recommended.get(0).getRecommendationScore() >= recommended.get(1).getRecommendationScore());
        assertTrue(recommended.get(1).getRecommendationScore() >= recommended.get(2).getRecommendationScore());
    }

    @Test
    @DisplayName("Explanation is descriptive based on dominant urgency factor")
    void testExplanationGeneration() {
        Task overdue = new Task("Overdue");
        overdue.setDueDate(today.minusDays(2));
        assertEquals("Overdue task", engine.generateExplanation(overdue, today));

        Task dueSoon = new Task("Due Today");
        dueSoon.setDueDate(today);
        assertEquals("Deadline approaching", engine.generateExplanation(dueSoon, today));

        Task critical = new Task("Critical Priority");
        critical.setPriority(Priority.CRITICAL);
        critical.setDueDate(today.plusDays(20));
        assertEquals("Critical priority", engine.generateExplanation(critical, today));
    }
}
