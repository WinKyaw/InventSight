package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.PredefinedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PredefinedItemRepository extends JpaRepository<PredefinedItem, UUID> {
    
    /**
     * Find all active items for a company
     */
    Page<PredefinedItem> findByCompanyAndIsActiveTrueOrderByNameAsc(Company company, Pageable pageable);
    
    /**
     * Find all items (including inactive) for a company
     */
    Page<PredefinedItem> findByCompanyOrderByNameAsc(Company company, Pageable pageable);
    
    /**
     * Find active items by category
     */
    Page<PredefinedItem> findByCompanyAndCategoryAndIsActiveTrueOrderByNameAsc(
        Company company, String category, Pageable pageable);
    
    /**
     * Find active items by name containing (search)
     */
    Page<PredefinedItem> findByCompanyAndNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(
        Company company, String name, Pageable pageable);
    
    /**
     * Find item by company, name and unit type
     */
    Optional<PredefinedItem> findByCompanyAndNameAndUnitType(Company company, String name, String unitType);
    
    /**
     * Check if item exists by company, name and unit type
     */
    boolean existsByCompanyAndNameAndUnitType(Company company, String name, String unitType);
    
    /**
     * Find all active items for a company
     */
    List<PredefinedItem> findByCompanyAndIsActiveTrueOrderByNameAsc(Company company);
    
    /**
     * Count active items for a company
     */
    long countByCompanyAndIsActiveTrue(Company company);
    
    /**
     * Find all distinct categories for a company
     */
    @Query("SELECT DISTINCT p.category FROM PredefinedItem p WHERE p.company = :company AND p.isActive = true AND p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategoriesByCompany(@Param("company") Company company);
    
    /**
     * Find all distinct unit types for a company
     */
    @Query("SELECT DISTINCT p.unitType FROM PredefinedItem p WHERE p.company = :company AND p.isActive = true ORDER BY p.unitType")
    List<String> findDistinctUnitTypesByCompany(@Param("company") Company company);
    
    /**
     * Check if a SKU already exists
     */
    boolean existsBySku(String sku);
}
