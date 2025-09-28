package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Warehouse entities
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    /**
     * Find all active warehouses
     */
    List<Warehouse> findByIsActiveTrue();

    /**
     * Find warehouses by type
     */
    List<Warehouse> findByWarehouseTypeAndIsActiveTrue(Warehouse.WarehouseType type);

    /**
     * Find warehouse by name (case insensitive)
     */
    Optional<Warehouse> findByNameIgnoreCaseAndIsActiveTrue(String name);

    /**
     * Find warehouses by city
     */
    List<Warehouse> findByCityIgnoreCaseAndIsActiveTrue(String city);

    /**
     * Find warehouses by state
     */
    List<Warehouse> findByStateIgnoreCaseAndIsActiveTrue(String state);

    /**
     * Find warehouses by country
     */
    List<Warehouse> findByCountryIgnoreCaseAndIsActiveTrue(String country);

    /**
     * Search warehouses by name or location containing the search term
     */
    @Query("SELECT w FROM Warehouse w WHERE w.isActive = true AND " +
           "(LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(w.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Warehouse> searchByNameOrLocation(@Param("searchTerm") String searchTerm);

    /**
     * Find warehouses with manager
     */
    List<Warehouse> findByManagerNameIsNotNullAndIsActiveTrue();

    /**
     * Check if warehouse name already exists (for validation)
     */
    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);

    /**
     * Count active warehouses
     */
    long countByIsActiveTrue();

    /**
     * Find warehouses created by user
     */
    List<Warehouse> findByCreatedByAndIsActiveTrue(String createdBy);
}