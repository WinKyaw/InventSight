package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.StoreInventoryWithdrawal;
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
 * Repository interface for StoreInventoryWithdrawal entities
 */
@Repository
public interface StoreInventoryWithdrawalRepository extends JpaRepository<StoreInventoryWithdrawal, UUID> {

    /**
     * Find withdrawals by store
     */
    List<StoreInventoryWithdrawal> findByStore(Store store);

    /**
     * Find withdrawals by store ID
     */
    List<StoreInventoryWithdrawal> findByStoreId(UUID storeId);

    /**
     * Find withdrawals by store ID with pagination
     */
    Page<StoreInventoryWithdrawal> findByStoreId(UUID storeId, Pageable pageable);

    /**
     * Find withdrawals by store ID and creator with pagination
     */
    Page<StoreInventoryWithdrawal> findByStoreIdAndCreatedBy(
        UUID storeId, 
        String createdBy, 
        Pageable pageable
    );

    /**
     * Find withdrawals by product
     */
    List<StoreInventoryWithdrawal> findByProduct(Product product);

    /**
     * Find withdrawals by product ID
     */
    List<StoreInventoryWithdrawal> findByProductId(UUID productId);

    /**
     * Find withdrawals by store and product
     */
    List<StoreInventoryWithdrawal> findByStoreAndProduct(Store store, Product product);

    /**
     * Find withdrawals by store ID and product ID
     */
    List<StoreInventoryWithdrawal> findByStoreIdAndProductId(UUID storeId, UUID productId);

    /**
     * Find withdrawals by date range
     */
    List<StoreInventoryWithdrawal> findByWithdrawalDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find withdrawals by date range in specific store
     */
    List<StoreInventoryWithdrawal> findByStoreIdAndWithdrawalDateBetween(
        UUID storeId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find withdrawals by transaction type
     */
    List<StoreInventoryWithdrawal> findByTransactionType(StoreInventoryWithdrawal.TransactionType transactionType);

    /**
     * Find withdrawals by status
     */
    List<StoreInventoryWithdrawal> findByStatus(StoreInventoryWithdrawal.TransactionStatus status);

    /**
     * Find withdrawals by reason
     */
    List<StoreInventoryWithdrawal> findByReasonContainingIgnoreCase(String reason);

    /**
     * Find withdrawals by reference number
     */
    List<StoreInventoryWithdrawal> findByReferenceNumber(String referenceNumber);

    /**
     * Find withdrawals created by user
     */
    List<StoreInventoryWithdrawal> findByCreatedBy(String createdBy);

    /**
     * Get total quantity withdrawn for a product from a store
     */
    @Query("SELECT SUM(siw.quantity) FROM StoreInventoryWithdrawal siw WHERE siw.store.id = :storeId AND " +
           "siw.product.id = :productId AND siw.status = com.pos.inventsight.model.sql.StoreInventoryWithdrawal$TransactionStatus.COMPLETED")
    Long getTotalQuantityWithdrawnByStoreAndProduct(@Param("storeId") UUID storeId, 
                                                     @Param("productId") UUID productId);

    /**
     * Find withdrawals by transaction type and store
     */
    List<StoreInventoryWithdrawal> findByStoreIdAndTransactionType(
        UUID storeId, StoreInventoryWithdrawal.TransactionType transactionType
    );

    /**
     * Find recent withdrawals (created within last N days)
     */
    @Query("SELECT siw FROM StoreInventoryWithdrawal siw WHERE siw.createdAt >= :sinceDate ORDER BY siw.createdAt DESC")
    List<StoreInventoryWithdrawal> findRecentWithdrawals(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find withdrawals ordered by withdrawal date descending
     */
    List<StoreInventoryWithdrawal> findAllByOrderByWithdrawalDateDesc();

    /**
     * Find withdrawals by store ordered by withdrawal date descending
     */
    List<StoreInventoryWithdrawal> findByStoreIdOrderByWithdrawalDateDesc(UUID storeId);

    /**
     * Find damage/loss withdrawals
     */
    @Query("SELECT siw FROM StoreInventoryWithdrawal siw WHERE siw.transactionType IN " +
           "(com.pos.inventsight.model.sql.StoreInventoryWithdrawal$TransactionType.DAMAGE, " +
           "com.pos.inventsight.model.sql.StoreInventoryWithdrawal$TransactionType.EXPIRED, " +
           "com.pos.inventsight.model.sql.StoreInventoryWithdrawal$TransactionType.STOLEN)")
    List<StoreInventoryWithdrawal> findDamageAndLossWithdrawals();

    /**
     * Get withdrawal statistics by transaction type
     */
    @Query("SELECT siw.transactionType, COUNT(siw), SUM(siw.quantity) FROM StoreInventoryWithdrawal siw WHERE " +
           "siw.status = com.pos.inventsight.model.sql.StoreInventoryWithdrawal$TransactionStatus.COMPLETED GROUP BY siw.transactionType")
    List<Object[]> getWithdrawalStatsByType();

    /**
     * Count withdrawals for a store
     */
    long countByStoreId(UUID storeId);

    /**
     * Count completed withdrawals
     */
    long countByStatus(StoreInventoryWithdrawal.TransactionStatus status);

    /**
     * Count withdrawals by transaction type
     */
    long countByTransactionType(StoreInventoryWithdrawal.TransactionType transactionType);
}
