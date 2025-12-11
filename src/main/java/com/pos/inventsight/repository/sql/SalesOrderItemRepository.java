package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    
    /**
     * Find best performing products by revenue in a date range
     */
    @Query("SELECT soi.product.id, soi.product.name, soi.product.sku, soi.product.category, " +
           "SUM(soi.quantity) as unitsSold, " +
           "SUM(soi.quantity * soi.unitPrice * (1 - soi.discountPercent / 100)) as revenue " +
           "FROM SalesOrderItem soi " +
           "WHERE soi.order.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY soi.product.id, soi.product.name, soi.product.sku, soi.product.category " +
           "ORDER BY revenue DESC")
    List<Object[]> findBestPerformingProducts(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate total revenue for date range
     */
    @Query("SELECT SUM(soi.quantity * soi.unitPrice * (1 - soi.discountPercent / 100)) " +
           "FROM SalesOrderItem soi " +
           "WHERE soi.order.createdAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal calculateRevenueForPeriod(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get sales data grouped by date for chart
     */
    @Query("SELECT DATE(soi.order.createdAt) as date, " +
           "SUM(soi.quantity * soi.unitPrice * (1 - soi.discountPercent / 100)) as revenue, " +
           "COUNT(DISTINCT soi.order.id) as orders " +
           "FROM SalesOrderItem soi " +
           "WHERE soi.order.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(soi.order.createdAt) " +
           "ORDER BY date")
    List<Object[]> getSalesChartData(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
}
