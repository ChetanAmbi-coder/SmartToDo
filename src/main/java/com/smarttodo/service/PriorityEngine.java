package com.smarttodo.service;

import com.smarttodo.model.Priority;
import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PriorityEngine {

    public static class ScoredTask {
        private final Task task;
        private final double score;
        private final String explanation;

        public ScoredTask(Task task, double score, String explanation) {
            this.task = task;
            this.score = score;
            this.explanation = explanation;
        }

        public Task getTask() {
            return task;
        }

        public double getScore() {
            return score;
        }

        public String getExplanation() {
            return explanation;
        }
    }

    /**
     * Calculates recommendation score:
     * Score = (40 * Importance) + (30 * Urgency) + (20 * DeadlineProximity) + (10 * OverdueStatus)
     * All components normalized between 0 and 1.
     */
    public double calculateScore(Task task, LocalDate referenceDate) {
        if (task == null || task.getStatus() == TaskStatus.COMPLETED) {
            return 0.0;
        }

        double importance = calculateImportanceFactor(task);
        double urgency = calculateUrgencyFactor(task);
        double deadlineProximity = calculateDeadlineProximityFactor(task, referenceDate);
        double overdueStatus = calculateOverdueFactor(task, referenceDate);

        return (40.0 * importance) + (30.0 * urgency) + (20.0 * deadlineProximity) + (10.0 * overdueStatus);
    }

    public double calculateImportanceFactor(Task task) {
        return task.isImportant() ? 1.0 : 0.0;
    }

    public double calculateUrgencyFactor(Task task) {
        Priority priority = task.getPriority() != null ? task.getPriority() : Priority.MEDIUM;
        return switch (priority) {
            case CRITICAL -> 1.0;
            case HIGH -> 0.75;
            case MEDIUM -> 0.50;
            case LOW -> 0.25;
        };
    }

    public double calculateDeadlineProximityFactor(Task task, LocalDate referenceDate) {
        if (task.getDueDate() == null) {
            return 0.0;
        }

        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(ref, task.getDueDate());

        if (daysUntilDue <= 0) {
            // Due today or overdue
            return 1.0;
        } else if (daysUntilDue == 1) {
            return 0.85;
        } else if (daysUntilDue == 2) {
            return 0.70;
        } else if (daysUntilDue <= 4) {
            return 0.50;
        } else if (daysUntilDue <= 7) {
            return 0.30;
        } else if (daysUntilDue <= 14) {
            return 0.15;
        } else {
            return 0.05;
        }
    }

    public double calculateOverdueFactor(Task task, LocalDate referenceDate) {
        if (task.getStatus() == TaskStatus.COMPLETED || task.getDueDate() == null) {
            return 0.0;
        }
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        return task.getDueDate().isBefore(ref) ? 1.0 : 0.0;
    }

    public String generateExplanation(Task task, LocalDate referenceDate) {
        if (calculateOverdueFactor(task, referenceDate) == 1.0) {
            return "Overdue task";
        }
        double proximity = calculateDeadlineProximityFactor(task, referenceDate);
        if (proximity >= 0.85) {
            return "Deadline approaching";
        }
        if (task.getPriority() == Priority.CRITICAL) {
            return "Critical priority";
        }
        if (task.isImportant()) {
            return "High importance";
        }
        if (task.getPriority() == Priority.HIGH) {
            return "High priority";
        }
        if (proximity >= 0.50) {
            return "Due soon";
        }
        return "Recommended task";
    }

    /**
     * Scores, annotates, and ranks active tasks in descending order.
     * Applies deterministic tie-breaking.
     */
    public List<Task> getRecommendations(List<Task> allTasks, int limit) {
        LocalDate today = LocalDate.now();
        List<Task> activeTasks = new ArrayList<>();

        for (Task t : allTasks) {
            if (t.getStatus() != TaskStatus.COMPLETED) {
                double score = calculateScore(t, today);
                String reason = generateExplanation(t, today);
                t.setRecommendationScore(score);
                t.setRecommendationReason(reason);
                activeTasks.add(t);
            }
        }

        // Deterministic comparator:
        // 1. Recommendation score descending
        // 2. Earliest due date first (nulls last)
        // 3. Highest priority level descending
        // 4. Earliest created_at / ID ascending
        activeTasks.sort((t1, t2) -> {
            int scoreCompare = Double.compare(t2.getRecommendationScore(), t1.getRecommendationScore());
            if (scoreCompare != 0) return scoreCompare;

            if (t1.getDueDate() != null && t2.getDueDate() != null) {
                int dateCompare = t1.getDueDate().compareTo(t2.getDueDate());
                if (dateCompare != 0) return dateCompare;
            } else if (t1.getDueDate() != null) {
                return -1;
            } else if (t2.getDueDate() != null) {
                return 1;
            }

            int prioCompare = Integer.compare(
                    t2.getPriority() != null ? t2.getPriority().getLevel() : 0,
                    t1.getPriority() != null ? t1.getPriority().getLevel() : 0
            );
            if (prioCompare != 0) return prioCompare;

            long id1 = t1.getId() != null ? t1.getId() : 0L;
            long id2 = t2.getId() != null ? t2.getId() : 0L;
            return Long.compare(id1, id2);
        });

        if (limit > 0 && activeTasks.size() > limit) {
            return new ArrayList<>(activeTasks.subList(0, limit));
        }
        return activeTasks;
    }
}
