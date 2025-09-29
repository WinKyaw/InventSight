package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for WarehouseInventoryWithdrawal entities
 */
@Repository
public interface WarehouseInventoryWithdrawalRepository extends JpaRepository<WarehouseInventoryWithdrawal, UUID> {

    /**
     * Find withdrawals by warehouse
     */
    List<WarehouseInventoryWithdrawal> findByWarehouse(Warehouse warehouse);

    /**
     * Find withdrawals by warehouse ID
     */
    List<WarehouseInventoryWithdrawal> findByWarehouseId(UUID warehouseId);

    /**
     * Find withdrawals by product
     */
    List<WarehouseInventoryWithdrawal> findByProduct(Product product);

    /**
     * Find withdrawals by product ID
     */
    List<WarehouseInventoryWithdrawal> findByProductId(UUID productId);

    /**
     * Find withdrawals by warehouse and product
     */
    List<WarehouseInventoryWithdrawal> findByWarehouseAndProduct(Warehouse warehouse, Product product);

    /**
     * Find withdrawals by warehouse ID and product ID
     */
    List<WarehouseInventoryWithdrawal> findByWarehouseIdAndProductId(UUID warehouseId, UUID productId);

    /**
     * Find withdrawals by date range
     */
    List<WarehouseInventoryWithdrawal> findByWithdrawalDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find withdrawals by date range in specific warehouse
     */
    List<WarehouseInventoryWithdrawal> findByWarehouseIdAndWithdrawalDateBetween(
        UUID warehouseId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find withdrawals by transaction type
     */
    List<WarehouseInventoryWithdrawal> findByTransactionType(WarehouseInventoryWithdrawal.TransactionType transactionType);

    /**
     * Find withdrawals by status
     */
    List<WarehouseInventoryWithdrawal> findByStatus(WarehouseInventoryWithdrawal.TransactionStatus status);

    /**
     * Find withdrawals by reason
     */
    List<WarehouseInventoryWithdrawal> findByReasonContainingIgnoreCase(String reason);

    /**
     * Find withdrawals by destination
     */
    List<WarehouseInventoryWithdrawal> findByDestinationContainingIgnoreCase(String destination);

    /**
     * Find withdrawals by reference number
     */
    List<WarehouseInventoryWithdrawal> findByReferenceNumber(String referenceNumber);

    /**
     * Find withdrawals created by user
     */
    List<WarehouseInventoryWithdrawal> findByCreatedBy(String createdBy);

    /**
     * Get total quantity withdrawn for a product from a warehouse
     */
    @Query("SELECT SUM(wiw.quantity) FROM WarehouseInventoryWithdrawal wiw WHERE wiw.warehouse.id = :warehouseId AND " +
           "wiw.product.id = :productId AND wiw.status = 'COMPLETED'")
    Long getTotalQuantityWithdrawnByWarehouseAndProduct(@Param("warehouseId") UUID warehouseId, 
                                                       @Param("productId") UUID productId);

    /**
     * Get total value of withdrawals for a warehouse
     */
    @Query("SELECT SUM(wiw.totalCost) FROM WarehouseInventoryWithdrawal wiw WHERE wiw.warehouse.id = :warehouseId AND " +
           "wiw.status = 'COMPLETED'")
    Double getTotalWithdrawalValueByWarehouse(@Param("warehouseId") UUID warehouseId);

    /**
     * Get total value of withdrawals for a date range
     */
    @Query("SELECT SUM(wiw.totalCost) FROM WarehouseInventoryWithdrawal wiw WHERE " +
           "wiw.withdrawalDate BETWEEN :startDate AND :endDate AND wiw.status = 'COMPLETED'")
    Double getTotalWithdrawalValueByDateRange(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);

    /**
     * Find withdrawals by transaction type and warehouse
     */
    List<WarehouseInventoryWithdrawal> findByWarehouseIdAndTransactionType(
        UUID warehouseId, WarehouseInventoryWithdrawal.TransactionType transactionType
    );

    /**
     * Find recent withdrawals (created within last N days)
     */
    @Query("SELECT wiw FROM WarehouseInventoryWithdrawal wiw WHERE wiw.createdAt >= :sinceDate ORDER BY wiw.createdAt DESC")
    List<WarehouseInventoryWithdrawal> findRecentWithdrawals(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find withdrawals ordered by withdrawal date descending
     */
    List<WarehouseInventoryWithdrawal> findAllByOrderByWithdrawalDateDesc();

    /**
     * Find withdrawals by warehouse ordered by withdrawal date descending
     */
    List<WarehouseInventoryWithdrawal> findByWarehouseIdOrderByWithdrawalDateDesc(UUID warehouseId);

    /**
     * Find damage/loss withdrawals
     */
    @Query("SELECT wiw FROM WarehouseInventoryWithdrawal wiw WHERE wiw.transactionType IN ('DAMAGE', 'EXPIRED', 'THEFT')")
    List<WarehouseInventoryWithdrawal> findDamageAndLossWithdrawals();

    /**
     * Find sale-related withdrawals
     */
    List<WarehouseInventoryWithdrawal> findByTransactionTypeAndDestinationContainingIgnoreCase(
        WarehouseInventoryWithdrawal.TransactionType transactionType, String destination
    );

    /**
     * Get withdrawal statistics by reason
     */
    @Query("SELECT wiw.reason, COUNT(wiw), SUM(wiw.quantity) FROM WarehouseInventoryWithdrawal wiw WHERE " +
           "wiw.status = 'COMPLETED' GROUP BY wiw.reason")
    List<Object[]> getWithdrawalStatsByReason();

    /**
     * Get withdrawal statistics by transaction type
     */
    @Query("SELECT wiw.transactionType, COUNT(wiw), SUM(wiw.quantity) FROM WarehouseInventoryWithdrawal wiw WHERE " +
           "wiw.status = 'COMPLETED' GROUP BY wiw.transactionType")
    List<Object[]> getWithdrawalStatsByType();

    /**
     * Count withdrawals for a warehouse
     */
    long countByWarehouseId(UUID warehouseId);

    /**
     * Count completed withdrawals
     */
    long countByStatus(WarehouseInventoryWithdrawal.TransactionStatus status);

    /**
     * Count withdrawals by transaction type
     */
    long countByTransactionType(WarehouseInventoryWithdrawal.TransactionType transactionType);
}