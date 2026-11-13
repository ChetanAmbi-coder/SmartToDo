package com.smarttodo.service;

import com.smarttodo.model.Priority;
import com.smarttodo.model.Task;
import com.smarttodo.model.TaskStatus;
import com.smarttodo.repository.TaskRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SearchFilterService {
    public static class FilterCriteria {
        public String query;
        public Priority priority;
        public Long categoryId;
        public Long projectId;
        public TaskStatus status;
        public LocalDate dueDate;
        public Boolean isImportant;
        public Boolean isOverdue;
        public String sortBy = "created_at"; // created_at, deadline, priority, title
        public boolean ascending = false;

        public void reset() {
            query = null;
            priority = null;
            categoryId = null;
            projectId = null;
            status = null;
            dueDate = null;
            isImportant = null;
            isOverdue = null;
            sortBy = "created_at";
            ascending = false;
        }

        public boolean hasActiveFilters() {
            return (query != null && !query.trim().isEmpty())
                    || priority != null
                    || categoryId != null
                    || projectId != null
                    || status != null
                    || dueDate != null
                    || (isImportant != null && isImportant)
                    || (isOverdue != null && isOverdue);
        }
    }

    private final TaskRepository taskRepository;

    public SearchFilterService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> executeSearch(FilterCriteria criteria) throws SQLException {
        if (criteria == null) {
            criteria = new FilterCriteria();
        }
        return taskRepository.searchAndFilter(
                criteria.query,
                criteria.priority,
                criteria.categoryId,
                criteria.projectId,
                criteria.status,
                criteria.dueDate,
                criteria.isImportant,
                criteria.isOverdue,
                criteria.sortBy,
                criteria.ascending
        );
    }
}
