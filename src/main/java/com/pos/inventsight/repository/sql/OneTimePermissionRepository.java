package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.OneTimePermission;
import com.pos.inventsight.model.sql.PermissionType;
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
public interface OneTimePermissionRepository extends JpaRepository<OneTimePermission, UUID> {
    
    /**
     * Find active (valid) permissions for a user and permission type
     */
    @Query("SELECT p FROM OneTimePermission p WHERE p.grantedToUser.id = :userId " +
           "AND p.permissionType = :permissionType " +
           "AND p.isUsed = false " +
           "AND p.isExpired = false " +
           "AND p.expiresAt > :now")
    List<OneTimePermission> findActivePermissions(@Param("userId") UUID userId, 
                                                   @Param("permissionType") PermissionType permissionType,
                                                   @Param("now") LocalDateTime now);
    
    /**
     * Find all active permissions for a user
     */
    @Query("SELECT p FROM OneTimePermission p WHERE p.grantedToUser.id = :userId " +
           "AND p.isUsed = false " +
           "AND p.isExpired = false " +
           "AND p.expiresAt > :now")
    List<OneTimePermission> findAllActivePermissionsForUser(@Param("userId") UUID userId,
                                                             @Param("now") LocalDateTime now);
    
    /**
     * Find permissions that should be expired
     */
    @Query("SELECT p FROM OneTimePermission p WHERE p.isUsed = false " +
           "AND p.isExpired = false " +
           "AND p.expiresAt <= :now")
    List<OneTimePermission> findPermissionsToExpire(@Param("now") LocalDateTime now);
    
    /**
     * Find all permissions granted to a user
     */
    List<OneTimePermission> findByGrantedToUserOrderByGrantedAtDesc(User user);
    
    /**
     * Find all permissions granted by a user
     */
    List<OneTimePermission> findByGrantedByUserOrderByGrantedAtDesc(User user);
    
    /**
     * Check if user has active permission of a specific type
     */
    @Query("SELECT COUNT(p) > 0 FROM OneTimePermission p WHERE p.grantedToUser.id = :userId " +
           "AND p.permissionType = :permissionType " +
           "AND p.isUsed = false " +
           "AND p.isExpired = false " +
           "AND p.expiresAt > :now")
    boolean hasActivePermission(@Param("userId") UUID userId,
                               @Param("permissionType") PermissionType permissionType,
                               @Param("now") LocalDateTime now);
}
