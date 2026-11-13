package com.smarttodo.service;

import com.smarttodo.model.Project;
import com.smarttodo.repository.ProjectRepository;
import com.smarttodo.util.ValidationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProjectService {
    public interface ProjectChangeListener {
        void onProjectsChanged();
    }

    private final ProjectRepository projectRepository;
    private final List<ProjectChangeListener> listeners = new CopyOnWriteArrayList<>();

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void addListener(ProjectChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ProjectChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyChange() {
        for (ProjectChangeListener listener : listeners) {
            try {
                listener.onProjectsChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Project createProject(Project project) throws SQLException {
        if (project == null || project.getName() == null || project.getName().trim().isEmpty()) {
            throw new ValidationException("Project name is required.");
        }
        Project created = projectRepository.create(project);
        notifyChange();
        return created;
    }

    public void updateProject(Project project) throws SQLException {
        if (project == null || project.getName() == null || project.getName().trim().isEmpty()) {
            throw new ValidationException("Project name is required.");
        }
        projectRepository.update(project);
        notifyChange();
    }

    public void deleteProject(long id) throws SQLException {
        projectRepository.delete(id);
        notifyChange();
    }

    public List<Project> getAllProjects() throws SQLException {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(long id) throws SQLException {
        return projectRepository.findById(id);
    }
}
