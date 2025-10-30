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
     * Find all active roles for a given membership
     */
    List<CompanyStoreUserRole> findByCompanyStoreUserAndIsActiveTrue(CompanyStoreUser companyStoreUser);
    
    /**
     * Find all roles (active and inactive) for a given membership
     */
    List<CompanyStoreUserRole> findByCompanyStoreUser(CompanyStoreUser companyStoreUser);
    
    /**
     * Find specific role for a membership
     */
    Optional<CompanyStoreUserRole> findByCompanyStoreUserAndRole(CompanyStoreUser companyStoreUser, CompanyRole role);
    
    /**
     * Find specific active role for a membership
     */
    Optional<CompanyStoreUserRole> findByCompanyStoreUserAndRoleAndIsActiveTrue(CompanyStoreUser companyStoreUser, CompanyRole role);
    
    /**
     * Check if a membership has a specific active role
     */
    boolean existsByCompanyStoreUserAndRoleAndIsActiveTrue(CompanyStoreUser companyStoreUser, CompanyRole role);
    
    /**
     * Get all distinct active roles for a membership
     */
    @Query("SELECT DISTINCT r.role FROM CompanyStoreUserRole r WHERE r.companyStoreUser = :membership AND r.isActive = true ORDER BY r.role")
    List<CompanyRole> findActiveRolesByMembership(@Param("membership") CompanyStoreUser membership);
    
    /**
     * Get highest priority active role for a membership (based on enum order)
     */
    @Query("SELECT r.role FROM CompanyStoreUserRole r WHERE r.companyStoreUser = :membership AND r.isActive = true ORDER BY r.role ASC")
    List<CompanyRole> findRolesByMembershipOrderedByPriority(@Param("membership") CompanyStoreUser membership);
    
    /**
     * Count active roles for a membership
     */
    long countByCompanyStoreUserAndIsActiveTrue(CompanyStoreUser companyStoreUser);
    
    /**
     * Delete all roles for a membership (for cleanup)
     */
    void deleteByCompanyStoreUser(CompanyStoreUser companyStoreUser);
}
