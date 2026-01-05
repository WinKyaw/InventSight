package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.StoreInventoryAddition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for StoreInventoryAddition entities
 */
@Repository
public interface StoreInventoryAdditionRepository extends JpaRepository<StoreInventoryAddition, UUID> {

    /**
     * Find additions by store
     */
    List<StoreInventoryAddition> findByStore(Store store);

    /**
     * Find additions by store ID
     */
    List<StoreInventoryAddition> findByStoreId(UUID storeId);

    /**
     * Find additions by store ID with pagination
     */
    Page<StoreInventoryAddition> findByStoreId(UUID storeId, Pageable pageable);

    /**
     * Find additions by store ID and creator with pagination
     */
    Page<StoreInventoryAddition> findByStoreIdAndCreatedBy(
        UUID storeId, 
        String createdBy, 
        Pageable pageable
    );

    /**
     * Find additions by product
     */
    List<StoreInventoryAddition> findByProduct(Product product);

    /**
     * Find additions by product ID
     */
    List<StoreInventoryAddition> findByProductId(UUID productId);

    /**
     * Find additions by store and product
     */
    List<StoreInventoryAddition> findByStoreAndProduct(Store store, Product product);

    /**
     * Find additions by store ID and product ID
     */
    List<StoreInventoryAddition> findByStoreIdAndProductId(UUID storeId, UUID productId);

    /**
     * Find additions by date range
     */
    List<StoreInventoryAddition> findByReceiptDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find additions by date range in specific store
     */
    List<StoreInventoryAddition> findByStoreIdAndReceiptDateBetween(
        UUID storeId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find additions by transaction type
     */
    List<StoreInventoryAddition> findByTransactionType(StoreInventoryAddition.TransactionType transactionType);

    /**
     * Find additions by status
     */
    List<StoreInventoryAddition> findByStatus(StoreInventoryAddition.TransactionStatus status);

    /**
     * Find completed additions by status and store
     */
    List<StoreInventoryAddition> findByStatusAndStoreId(StoreInventoryAddition.TransactionStatus status, UUID storeId);

    /**
     * Find additions by supplier
     */
    List<StoreInventoryAddition> findBySupplierNameContainingIgnoreCase(String supplierName);

    /**
     * Find additions by reference number
     */
    List<StoreInventoryAddition> findByReferenceNumber(String referenceNumber);

    /**
     * Find additions by batch number
     */
    List<StoreInventoryAddition> findByBatchNumber(String batchNumber);

    /**
     * Find additions with expiry date
     */
    List<StoreInventoryAddition> findByExpiryDateIsNotNull();

    /**
     * Find additions expiring soon
     */
    @Query("SELECT sia FROM StoreInventoryAddition sia WHERE sia.expiryDate IS NOT NULL AND " +
           "sia.expiryDate BETWEEN :startDate AND :endDate")
    List<StoreInventoryAddition> findExpiringSoon(@Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);

    /**
     * Find expired additions
     */
    @Query("SELECT sia FROM StoreInventoryAddition sia WHERE sia.expiryDate IS NOT NULL AND " +
           "sia.expiryDate < :currentDate")
    List<StoreInventoryAddition> findExpired(@Param("currentDate") LocalDate currentDate);

    /**
     * Find additions created by user
     */
    List<StoreInventoryAddition> findByCreatedBy(String createdBy);

    /**
     * Get total quantity added for a product in a store
     */
    @Query("SELECT SUM(sia.quantity) FROM StoreInventoryAddition sia WHERE sia.store.id = :storeId AND " +
           "sia.product.id = :productId AND sia.status = com.pos.inventsight.model.sql.StoreInventoryAddition$TransactionStatus.COMPLETED")
    Long getTotalQuantityAddedByStoreAndProduct(@Param("storeId") UUID storeId, 
                                                 @Param("productId") UUID productId);

    /**
     * Get total value of additions for a store
     */
    @Query("SELECT SUM(sia.totalCost) FROM StoreInventoryAddition sia WHERE sia.store.id = :storeId AND " +
           "sia.status = com.pos.inventsight.model.sql.StoreInventoryAddition$TransactionStatus.COMPLETED")
    Double getTotalAdditionValueByStore(@Param("storeId") UUID storeId);

    /**
     * Get total value of additions for a date range
     */
    @Query("SELECT SUM(sia.totalCost) FROM StoreInventoryAddition sia WHERE " +
           "sia.receiptDate BETWEEN :startDate AND :endDate AND sia.status = com.pos.inventsight.model.sql.StoreInventoryAddition$TransactionStatus.COMPLETED")
    Double getTotalAdditionValueByDateRange(@Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate);

    /**
     * Find recent additions (created within last N days)
     */
    @Query("SELECT sia FROM StoreInventoryAddition sia WHERE sia.createdAt >= :sinceDate ORDER BY sia.createdAt DESC")
    List<StoreInventoryAddition> findRecentAdditions(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find additions ordered by receipt date descending
     */
    List<StoreInventoryAddition> findAllByOrderByReceiptDateDesc();

    /**
     * Find additions by store ordered by receipt date descending
     */
    List<StoreInventoryAddition> findByStoreIdOrderByReceiptDateDesc(UUID storeId);

    /**
     * Count additions for a store
     */
    long countByStoreId(UUID storeId);

    /**
     * Count completed additions
     */
    long countByStatus(StoreInventoryAddition.TransactionStatus status);
}
