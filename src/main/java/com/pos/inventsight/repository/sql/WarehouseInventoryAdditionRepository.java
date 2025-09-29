package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventoryAddition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for WarehouseInventoryAddition entities
 */
@Repository
public interface WarehouseInventoryAdditionRepository extends JpaRepository<WarehouseInventoryAddition, UUID> {

    /**
     * Find additions by warehouse
     */
    List<WarehouseInventoryAddition> findByWarehouse(Warehouse warehouse);

    /**
     * Find additions by warehouse ID
     */
    List<WarehouseInventoryAddition> findByWarehouseId(UUID warehouseId);

    /**
     * Find additions by product
     */
    List<WarehouseInventoryAddition> findByProduct(Product product);

    /**
     * Find additions by product ID
     */
    List<WarehouseInventoryAddition> findByProductId(UUID productId);

    /**
     * Find additions by warehouse and product
     */
    List<WarehouseInventoryAddition> findByWarehouseAndProduct(Warehouse warehouse, Product product);

    /**
     * Find additions by warehouse ID and product ID
     */
    List<WarehouseInventoryAddition> findByWarehouseIdAndProductId(UUID warehouseId, UUID productId);

    /**
     * Find additions by date range
     */
    List<WarehouseInventoryAddition> findByReceiptDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find additions by date range in specific warehouse
     */
    List<WarehouseInventoryAddition> findByWarehouseIdAndReceiptDateBetween(
        UUID warehouseId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find additions by transaction type
     */
    List<WarehouseInventoryAddition> findByTransactionType(WarehouseInventoryAddition.TransactionType transactionType);

    /**
     * Find additions by status
     */
    List<WarehouseInventoryAddition> findByStatus(WarehouseInventoryAddition.TransactionStatus status);

    /**
     * Find completed additions
     */
    List<WarehouseInventoryAddition> findByStatus(WarehouseInventoryAddition.TransactionStatus status, UUID warehouseId);

    /**
     * Find additions by supplier
     */
    List<WarehouseInventoryAddition> findBySupplierNameContainingIgnoreCase(String supplierName);

    /**
     * Find additions by reference number
     */
    List<WarehouseInventoryAddition> findByReferenceNumber(String referenceNumber);

    /**
     * Find additions by batch number
     */
    List<WarehouseInventoryAddition> findByBatchNumber(String batchNumber);

    /**
     * Find additions with expiry date
     */
    List<WarehouseInventoryAddition> findByExpiryDateIsNotNull();

    /**
     * Find additions expiring soon
     */
    @Query("SELECT wia FROM WarehouseInventoryAddition wia WHERE wia.expiryDate IS NOT NULL AND " +
           "wia.expiryDate BETWEEN :startDate AND :endDate")
    List<WarehouseInventoryAddition> findExpiringSoon(@Param("startDate") LocalDate startDate, 
                                                     @Param("endDate") LocalDate endDate);

    /**
     * Find expired additions
     */
    @Query("SELECT wia FROM WarehouseInventoryAddition wia WHERE wia.expiryDate IS NOT NULL AND " +
           "wia.expiryDate < :currentDate")
    List<WarehouseInventoryAddition> findExpired(@Param("currentDate") LocalDate currentDate);

    /**
     * Find additions created by user
     */
    List<WarehouseInventoryAddition> findByCreatedBy(String createdBy);

    /**
     * Get total quantity added for a product in a warehouse
     */
    @Query("SELECT SUM(wia.quantity) FROM WarehouseInventoryAddition wia WHERE wia.warehouse.id = :warehouseId AND " +
           "wia.product.id = :productId AND wia.status = 'COMPLETED'")
    Long getTotalQuantityAddedByWarehouseAndProduct(@Param("warehouseId") UUID warehouseId, 
                                                   @Param("productId") UUID productId);

    /**
     * Get total value of additions for a warehouse
     */
    @Query("SELECT SUM(wia.totalCost) FROM WarehouseInventoryAddition wia WHERE wia.warehouse.id = :warehouseId AND " +
           "wia.status = 'COMPLETED'")
    Double getTotalAdditionValueByWarehouse(@Param("warehouseId") UUID warehouseId);

    /**
     * Get total value of additions for a date range
     */
    @Query("SELECT SUM(wia.totalCost) FROM WarehouseInventoryAddition wia WHERE " +
           "wia.receiptDate BETWEEN :startDate AND :endDate AND wia.status = 'COMPLETED'")
    Double getTotalAdditionValueByDateRange(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);

    /**
     * Find recent additions (created within last N days)
     */
    @Query("SELECT wia FROM WarehouseInventoryAddition wia WHERE wia.createdAt >= :sinceDate ORDER BY wia.createdAt DESC")
    List<WarehouseInventoryAddition> findRecentAdditions(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find additions ordered by receipt date descending
     */
    List<WarehouseInventoryAddition> findAllByOrderByReceiptDateDesc();

    /**
     * Find additions by warehouse ordered by receipt date descending
     */
    List<WarehouseInventoryAddition> findByWarehouseIdOrderByReceiptDateDesc(UUID warehouseId);

    /**
     * Count additions for a warehouse
     */
    long countByWarehouseId(UUID warehouseId);

    /**
     * Count completed additions
     */
    long countByStatus(WarehouseInventoryAddition.TransactionStatus status);
}