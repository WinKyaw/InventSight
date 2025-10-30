package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.CompanyStoreUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing many-to-many role mappings for company/store users
 */
@Repository
public interface CompanyStoreUserRoleRepository extends JpaRepository<CompanyStoreUserRole, UUID> {
    
    /**
     * Find all active roles for a company store user
     */
    List<CompanyStoreUserRole> findByCompanyStoreUserAndIsActiveTrue(CompanyStoreUser companyStoreUser);
    
    /**
     * Find specific role for a company store user
     */
    Optional<CompanyStoreUserRole> findByCompanyStoreUserAndRoleAndIsActiveTrue(CompanyStoreUser companyStoreUser, CompanyRole role);
    
    /**
     * Check if a role exists for a company store user
     */
    boolean existsByCompanyStoreUserAndRoleAndIsActiveTrue(CompanyStoreUser companyStoreUser, CompanyRole role);
    
    /**
     * Get all active role values for a company store user
     */
    @Query("SELECT csr.role FROM CompanyStoreUserRole csr WHERE csr.companyStoreUser = :companyStoreUser AND csr.isActive = true")
    List<CompanyRole> findRolesByCompanyStoreUser(@Param("companyStoreUser") CompanyStoreUser companyStoreUser);
    
    /**
     * Get highest role for a company store user (based on privilege level)
     */
    @Query("SELECT csr.role FROM CompanyStoreUserRole csr WHERE csr.companyStoreUser = :companyStoreUser AND csr.isActive = true ORDER BY " +
           "CASE csr.role " +
           "WHEN 'FOUNDER' THEN 1 " +
           "WHEN 'CEO' THEN 2 " +
           "WHEN 'GENERAL_MANAGER' THEN 3 " +
           "WHEN 'STORE_MANAGER' THEN 4 " +
           "WHEN 'EMPLOYEE' THEN 5 " +
           "END")
    List<CompanyRole> findRolesByCompanyStoreUserOrderedByPrivilege(@Param("companyStoreUser") CompanyStoreUser companyStoreUser);
    
    /**
     * Find all role mappings for a specific role (e.g., all CEOs)
     */
    List<CompanyStoreUserRole> findByRoleAndIsActiveTrue(CompanyRole role);
    
    /**
     * Count active roles for a company store user
     */
    long countByCompanyStoreUserAndIsActiveTrue(CompanyStoreUser companyStoreUser);
    
    /**
     * Delete all role mappings for a company store user (for cleanup)
     */
    void deleteByCompanyStoreUser(CompanyStoreUser companyStoreUser);
}
