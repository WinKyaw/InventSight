package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.CategoryRequest;
import com.pos.inventsight.dto.CategoryResponse;
import com.pos.inventsight.model.sql.Category;
import com.pos.inventsight.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    // GET /api/categories - Get all categories
    @GetMapping
    public ResponseEntity<?> getAllCategories(
            @RequestParam(required = false) String search,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📦 InventSight - Fetching categories for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            List<Category> categories;
            if (search != null && !search.trim().isEmpty()) {
                categories = categoryService.searchCategories(search);
                System.out.println("🔍 InventSight - Search results: " + categories.size() + " categories found");
            } else {
                categories = categoryService.getAllActiveCategories();
                System.out.println("📋 InventSight - Retrieved " + categories.size() + " active categories");
            }
            
            List<CategoryResponse> categoryResponses = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("categories", categoryResponses);
            response.put("total", categoryResponses.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching categories: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch categories: " + e.getMessage()));
        }
    }
    
    // GET /api/categories/count - Get total categories count
    @GetMapping("/count")
    public ResponseEntity<?> getCategoriesCount(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔢 InventSight - Getting categories count for user: " + username);
            
            long count = categoryService.getTotalCategoryCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Categories count: " + count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error getting categories count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to get categories count: " + e.getMessage()));
        }
    }
    
    // GET /api/categories/{id} - Get category by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 InventSight - Fetching category ID: " + id + " for user: " + username);
            
            Category category = categoryService.getCategoryById(id);
            CategoryResponse categoryResponse = convertToResponse(category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("category", categoryResponse);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Category retrieved: " + category.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Category not found: " + e.getMessage()));
        }
    }
    
    // POST /api/categories - Create category
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryRequest categoryRequest, 
                                          Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("➕ InventSight - Creating category for user: " + username);
            System.out.println("📦 Category name: " + categoryRequest.getName());
            
            Category category = new Category();
            category.setName(categoryRequest.getName());
            category.setDescription(categoryRequest.getDescription());
            
            Category createdCategory = categoryService.createCategory(category, username);
            CategoryResponse categoryResponse = convertToResponse(createdCategory);
            
            Map<String, Object> response = new HashMap<>();
            response.put("category", categoryResponse);
            response.put("message", "Category created successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Category created successfully: " + createdCategory.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error creating category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to create category: " + e.getMessage()));
        }
    }
    
    // PUT /api/categories/{id} - Update category
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, 
                                          @Valid @RequestBody CategoryRequest categoryRequest, 
                                          Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔄 InventSight - Updating category ID: " + id + " for user: " + username);
            
            Category categoryUpdates = new Category();
            categoryUpdates.setName(categoryRequest.getName());
            categoryUpdates.setDescription(categoryRequest.getDescription());
            
            Category updatedCategory = categoryService.updateCategory(id, categoryUpdates, username);
            CategoryResponse categoryResponse = convertToResponse(updatedCategory);
            
            Map<String, Object> response = new HashMap<>();
            response.put("category", categoryResponse);
            response.put("message", "Category updated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Category updated successfully: " + updatedCategory.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error updating category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to update category: " + e.getMessage()));
        }
    }
    
    // DELETE /api/categories/{id} - Delete category
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ InventSight - Deleting category ID: " + id + " for user: " + username);
            
            categoryService.deleteCategory(id, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Category deleted successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Category deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error deleting category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to delete category: " + e.getMessage()));
        }
    }
    
    // Helper method to convert Category to CategoryResponse
    private CategoryResponse convertToResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getIsActive(),
            category.getCreatedAt(),
            category.getUpdatedAt(),
            category.getCreatedBy(),
            category.getUpdatedBy()
        );
    }
}