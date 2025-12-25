package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.SupplyManagementPermission;
import com.pos.inventsight.model.sql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplyManagementPermissionRepository extends JpaRepository<SupplyManagementPermission, UUID> {
    
    /**
     * Find active permission for user and company
     */
    Optional<SupplyManagementPermission> findByUserAndCompanyAndIsActiveTrueAndRevokedAtIsNull(User user, Company company);
    
    /**
     * Find all active permissions for a user
     */
    List<SupplyManagementPermission> findByUserAndIsActiveTrueAndRevokedAtIsNull(User user);
    
    /**
     * Find all permissions for a company
     */
    List<SupplyManagementPermission> findByCompanyOrderByGrantedAtDesc(Company company);
    
    /**
     * Find all active permissions for a company
     */
    List<SupplyManagementPermission> findByCompanyAndIsActiveTrueAndRevokedAtIsNullOrderByGrantedAtDesc(Company company);
    
    /**
     * Check if user has active permission for company
     */
    boolean existsByUserAndCompanyAndIsActiveTrueAndRevokedAtIsNull(User user, Company company);
    
    /**
     * Find permissions that have expired
     */
    @Query("SELECT p FROM SupplyManagementPermission p WHERE p.isActive = true AND p.revokedAt IS NULL " +
           "AND p.isPermanent = false AND p.expiresAt < :now")
    List<SupplyManagementPermission> findExpiredPermissions(@Param("now") LocalDateTime now);
    
    /**
     * Count active permissions for a user across all companies
     */
    long countByUserAndIsActiveTrueAndRevokedAtIsNull(User user);
    
    /**
     * Count active permissions for a company
     */
    long countByCompanyAndIsActiveTrueAndRevokedAtIsNull(Company company);
}
