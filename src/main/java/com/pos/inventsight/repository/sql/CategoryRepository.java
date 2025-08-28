package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    List<Category> findByIsActiveTrue();
    
    @Query("SELECT COUNT(c) FROM Category c WHERE c.isActive = true")
    long countActiveCategories();
    
    @Query("SELECT c FROM Category c WHERE c.name LIKE %:searchTerm% OR c.description LIKE %:searchTerm%")
    List<Category> searchCategories(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT c FROM Category c WHERE c.createdBy = :createdBy AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Category> findByCreatedBy(@Param("createdBy") String createdBy);
    
    boolean existsByName(String name);
    
    boolean existsByNameAndIdNot(String name, Long id);
}