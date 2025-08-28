package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Category;
import com.pos.inventsight.repository.sql.CategoryRepository;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    // CRUD Operations
    public Category createCategory(Category category, String createdBy) {
        System.out.println("📦 InventSight - Creating new category: " + category.getName());
        System.out.println("👤 Created by: " + createdBy);
        
        // Check for duplicate category name
        if (categoryRepository.existsByName(category.getName())) {
            throw new DuplicateResourceException("Category with name '" + category.getName() + "' already exists");
        }
        
        category.setCreatedBy(createdBy);
        category.setUpdatedBy(createdBy);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        
        Category savedCategory = categoryRepository.save(category);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            createdBy != null ? createdBy : "WinKyaw",
            "CATEGORY_CREATED", 
            "CATEGORY", 
            "Created new category: " + savedCategory.getName()
        );
        
        System.out.println("✅ InventSight - Category created successfully with ID: " + savedCategory.getId());
        return savedCategory;
    }
    
    public Category updateCategory(Long categoryId, Category categoryUpdates, String updatedBy) {
        System.out.println("🔄 InventSight - Updating category ID: " + categoryId);
        System.out.println("👤 Updated by: " + updatedBy);
        
        Category existingCategory = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        
        // Check for duplicate name (excluding current category)
        if (categoryUpdates.getName() != null && 
            !categoryUpdates.getName().equals(existingCategory.getName()) && 
            categoryRepository.existsByNameAndIdNot(categoryUpdates.getName(), categoryId)) {
            throw new DuplicateResourceException("Category with name '" + categoryUpdates.getName() + "' already exists");
        }
        
        // Update fields
        if (categoryUpdates.getName() != null) {
            existingCategory.setName(categoryUpdates.getName());
        }
        if (categoryUpdates.getDescription() != null) {
            existingCategory.setDescription(categoryUpdates.getDescription());
        }
        if (categoryUpdates.getIsActive() != null) {
            existingCategory.setIsActive(categoryUpdates.getIsActive());
        }
        
        existingCategory.setUpdatedBy(updatedBy);
        existingCategory.setUpdatedAt(LocalDateTime.now());
        
        Category savedCategory = categoryRepository.save(existingCategory);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            updatedBy != null ? updatedBy : "WinKyaw",
            "CATEGORY_UPDATED", 
            "CATEGORY", 
            "Updated category: " + savedCategory.getName()
        );
        
        System.out.println("✅ InventSight - Category updated successfully");
        return savedCategory;
    }
    
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
    }
    
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public List<Category> searchCategories(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveCategories();
        }
        return categoryRepository.searchCategories(searchTerm.trim());
    }
    
    public void deleteCategory(Long categoryId, String deletedBy) {
        System.out.println("🗑️ InventSight - Deleting category ID: " + categoryId);
        System.out.println("👤 Deleted by: " + deletedBy);
        
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        
        // Soft delete - mark as inactive
        category.setIsActive(false);
        category.setUpdatedBy(deletedBy);
        category.setUpdatedAt(LocalDateTime.now());
        
        categoryRepository.save(category);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            deletedBy != null ? deletedBy : "WinKyaw",
            "CATEGORY_DELETED", 
            "CATEGORY", 
            "Deleted category: " + category.getName()
        );
        
        System.out.println("✅ InventSight - Category deleted successfully (soft delete)");
    }
    
    // Analytics
    public long getTotalCategoryCount() {
        return categoryRepository.countActiveCategories();
    }
    
    public List<Category> getCategoriesByCreatedBy(String createdBy) {
        return categoryRepository.findByCreatedBy(createdBy);
    }
}