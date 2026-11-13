package com.smarttodo.service;

import com.smarttodo.model.Category;
import com.smarttodo.repository.CategoryRepository;
import com.smarttodo.util.ValidationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class CategoryService {
    public interface CategoryChangeListener {
        void onCategoriesChanged();
    }

    private final CategoryRepository categoryRepository;
    private final List<CategoryChangeListener> listeners = new CopyOnWriteArrayList<>();

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void addListener(CategoryChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CategoryChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyChange() {
        for (CategoryChangeListener listener : listeners) {
            try {
                listener.onCategoriesChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Category createCategory(Category category) throws SQLException {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            throw new ValidationException("Category name is required.");
        }
        Category created = categoryRepository.create(category);
        notifyChange();
        return created;
    }

    public void updateCategory(Category category) throws SQLException {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            throw new ValidationException("Category name is required.");
        }
        categoryRepository.update(category);
        notifyChange();
    }

    public void deleteCategory(long id) throws SQLException {
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isPresent() && opt.get().isSystem()) {
            throw new ValidationException("Default system categories cannot be deleted.");
        }
        categoryRepository.delete(id);
        notifyChange();
    }

    public List<Category> getAllCategories() throws SQLException {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(long id) throws SQLException {
        return categoryRepository.findById(id);
    }
}
