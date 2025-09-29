package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
    /**
     * Find active companies
     */
    List<Company> findByIsActiveTrue();
    
    /**
     * Find company by name (case-insensitive)
     */
    Optional<Company> findByNameIgnoreCase(String name);
    
    /**
     * Find company by email
     */
    Optional<Company> findByEmail(String email);
    
    /**
     * Find companies by name containing (case-insensitive search)
     */
    List<Company> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    /**
     * Find companies by creation user
     */
    List<Company> findByCreatedByAndIsActiveTrue(String createdBy);
    
    /**
     * Check if company exists by name (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Check if company exists by email
     */
    boolean existsByEmail(String email);
    
    /**
     * Count active companies
     */
    @Query("SELECT COUNT(c) FROM Company c WHERE c.isActive = true")
    long countActiveCompanies();
    
    /**
     * Find companies with stores
     */
    @Query("SELECT DISTINCT c FROM Company c JOIN c.stores s WHERE c.isActive = true AND s.isActive = true")
    List<Company> findCompaniesWithActiveStores();
    
    /**
     * Find companies with warehouses
     */
    @Query("SELECT DISTINCT c FROM Company c JOIN c.warehouses w WHERE c.isActive = true AND w.isActive = true")
    List<Company> findCompaniesWithActiveWarehouses();
}