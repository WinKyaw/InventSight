package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for SalesOrderItem entities
 */
@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, UUID> {
    
    /**
     * Find all items for a specific order
     */
    List<SalesOrderItem> findByOrderId(UUID orderId);
    
    /**
     * Find all items for a specific warehouse
     */
    List<SalesOrderItem> findByWarehouseId(UUID warehouseId);
    
    /**
     * Find all items for a specific product
     */
    List<SalesOrderItem> findByProductId(UUID productId);
    
    /**
     * Count items for a specific order
     */
    long countByOrderId(UUID orderId);
}
