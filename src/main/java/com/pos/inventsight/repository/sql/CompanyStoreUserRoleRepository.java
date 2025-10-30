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

@Repository
public interface CompanyStoreUserRoleRepository extends JpaRepository<CompanyStoreUserRole, UUID> {
    
    /**
     * Find all active roles for a company-store-user membership
     */
    List<CompanyStoreUserRole> findByCompanyStoreUserAndIsActiveTrue(CompanyStoreUser companyStoreUser);
    
    /**
     * Find specific active role for a membership
     */
    Optional<CompanyStoreUserRole> findByCompanyStoreUserAndRoleAndIsActiveTrue(CompanyStoreUser companyStoreUser, CompanyRole role);
    
    /**
     * Find all roles (active and inactive) for a membership
     */
    List<CompanyStoreUserRole> findByCompanyStoreUser(CompanyStoreUser companyStoreUser);
    
    /**
     * Check if a specific role exists for a membership
     */
    boolean existsByCompanyStoreUserAndRoleAndIsActiveTrue(CompanyStoreUser companyStoreUser, CompanyRole role);
    
    /**
     * Get list of active roles for a membership
     */
    @Query("SELECT csr.role FROM CompanyStoreUserRole csr WHERE csr.companyStoreUser = :companyStoreUser AND csr.isActive = true")
    List<CompanyRole> findRolesByCompanyStoreUser(@Param("companyStoreUser") CompanyStoreUser companyStoreUser);
    
    /**
     * Delete all roles for a membership (for cleanup)
     */
    void deleteByCompanyStoreUser(CompanyStoreUser companyStoreUser);
}
