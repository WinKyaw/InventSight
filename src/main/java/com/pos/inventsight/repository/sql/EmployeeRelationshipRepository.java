package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.model.sql.EmployeeRelationship;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRelationshipRepository extends JpaRepository<EmployeeRelationship, Long> {
    
    /**
     * Find the relationship record for a specific employee
     */
    Optional<EmployeeRelationship> findByEmployee(Employee employee);
    
    /**
     * Find all relationships for a specific employer (user who created employees)
     */
    List<EmployeeRelationship> findByEmployer(User employer);
    
    /**
     * Find all relationships for a specific store
     */
    List<EmployeeRelationship> findByStore(Store store);
    
    /**
     * Find all relationships for a specific company
     */
    List<EmployeeRelationship> findByCompany(Company company);
    
    /**
     * Find all active relationships for a specific company
     */
    List<EmployeeRelationship> findByCompanyAndIsActiveTrue(Company company);
    
    /**
     * Find all active relationships for a specific store
     */
    List<EmployeeRelationship> findByStoreAndIsActiveTrue(Store store);
    
    /**
     * Find all active relationships created by a specific employer
     */
    List<EmployeeRelationship> findByEmployerAndIsActiveTrue(User employer);
    
    /**
     * Check if an employee has an active relationship
     */
    boolean existsByEmployeeAndIsActiveTrue(Employee employee);
    
    /**
     * Count active employees for a company
     */
    @Query("SELECT COUNT(er) FROM EmployeeRelationship er WHERE er.company = :company AND er.isActive = true")
    long countActiveEmployeesByCompany(@Param("company") Company company);
    
    /**
     * Count active employees for a store
     */
    @Query("SELECT COUNT(er) FROM EmployeeRelationship er WHERE er.store = :store AND er.isActive = true")
    long countActiveEmployeesByStore(@Param("store") Store store);
}
