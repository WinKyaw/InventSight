package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.WarehousePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehousePermissionRepository extends JpaRepository<WarehousePermission, UUID> {

    /**
     * Find active permission for user on warehouse
     */
    Optional<WarehousePermission> findByWarehouseIdAndUserIdAndIsActive(
        UUID warehouseId, 
        UUID userId, 
        Boolean isActive
    );

    /**
     * Find all active permissions for a warehouse
     */
    List<WarehousePermission> findByWarehouseIdAndIsActive(
        UUID warehouseId, 
        Boolean isActive
    );

    /**
     * Find all active permissions for a user
     */
    List<WarehousePermission> findByUserIdAndIsActive(
        UUID userId, 
        Boolean isActive
    );

    /**
     * Check if user has specific permission on warehouse
     */
    @Query("SELECT CASE WHEN COUNT(wp) > 0 THEN true ELSE false END " +
           "FROM WarehousePermission wp " +
           "WHERE wp.warehouse.id = :warehouseId " +
           "AND wp.user.id = :userId " +
           "AND wp.isActive = true " +
           "AND wp.permissionType = :permissionType")
    boolean hasPermission(
        @Param("warehouseId") UUID warehouseId,
        @Param("userId") UUID userId,
        @Param("permissionType") WarehousePermission.PermissionType permissionType
    );
}
