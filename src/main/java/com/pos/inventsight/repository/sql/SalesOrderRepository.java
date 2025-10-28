package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.OrderStatus;
import com.pos.inventsight.model.sql.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SalesOrder entities
 */
@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {
    
    /**
     * Find order by ID and tenant
     */
    Optional<SalesOrder> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Find all orders for a tenant
     */
    List<SalesOrder> findByTenantId(UUID tenantId);
    
    /**
     * Find all orders for a tenant with pagination
     */
    Page<SalesOrder> findByTenantId(UUID tenantId, Pageable pageable);
    
    /**
     * Find orders by tenant and status
     */
    List<SalesOrder> findByTenantIdAndStatus(UUID tenantId, OrderStatus status);
    
    /**
     * Find orders by tenant and status with pagination
     */
    Page<SalesOrder> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable);
    
    /**
     * Find orders requiring manager approval
     */
    List<SalesOrder> findByTenantIdAndRequiresManagerApprovalTrue(UUID tenantId);
    
    /**
     * Find orders by status requiring manager action
     */
    List<SalesOrder> findByTenantIdAndStatusIn(UUID tenantId, List<OrderStatus> statuses);
    
    /**
     * Count orders by tenant and status
     */
    long countByTenantIdAndStatus(UUID tenantId, OrderStatus status);
}
