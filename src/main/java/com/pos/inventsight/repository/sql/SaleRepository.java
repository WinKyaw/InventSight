package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.SaleStatus;
import com.pos.inventsight.model.sql.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    Optional<Sale> findByReceiptNumber(String receiptNumber);
    
    // Multi-tenant aware queries
    List<Sale> findByStore(Store store);
    List<Sale> findByStoreAndStatus(Store store, SaleStatus status);
    List<Sale> findByStoreAndProcessedByIdAndStatus(Store store, UUID userId, SaleStatus status);
    
    Optional<Sale> findByReceiptNumberAndStore(String receiptNumber, Store store);
    
    List<Sale> findByStatus(SaleStatus status);
    List<Sale> findByProcessedByIdAndStatus(Long userId, SaleStatus status);
    
    @Query("SELECT s FROM Sale s WHERE s.createdAt >= :startDate AND s.createdAt <= :endDate")
    List<Sale> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE s.store = :store AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    List<Sale> findByStoreAndDateRange(@Param("store") Store store, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE s.createdAt >= :startDate AND s.createdAt <= :endDate AND s.status = :status")
    List<Sale> findByDateRangeAndStatus(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("status") SaleStatus status);
    
    @Query("SELECT s FROM Sale s WHERE s.store = :store AND s.createdAt >= :startDate AND s.createdAt <= :endDate AND s.status = :status")
    List<Sale> findByStoreAndDateRangeAndStatus(@Param("store") Store store, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("status") SaleStatus status);
    
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.status = 'COMPLETED' AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.store = :store AND s.status = 'COMPLETED' AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    BigDecimal getTotalRevenueByStoreAndDateRange(@Param("store") Store store, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.status = 'COMPLETED' AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    long getSalesCountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.store = :store AND s.status = 'COMPLETED' AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    long getSalesCountByStoreAndDateRange(@Param("store") Store store, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(s.totalAmount) FROM Sale s WHERE s.status = 'COMPLETED' AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    BigDecimal getAverageOrderValueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(s.totalAmount) FROM Sale s WHERE s.store = :store AND s.status = 'COMPLETED' AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    BigDecimal getAverageOrderValueByStoreAndDateRange(@Param("store") Store store, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE s.customerEmail = :email ORDER BY s.createdAt DESC")
    Page<Sale> findByCustomerEmail(@Param("email") String email, Pageable pageable);
    
    @Query("SELECT s FROM Sale s WHERE s.store = :store AND s.customerEmail = :email ORDER BY s.createdAt DESC")
    Page<Sale> findByStoreAndCustomerEmail(@Param("store") Store store, @Param("email") String email, Pageable pageable);
    
    @Query("SELECT s FROM Sale s WHERE s.processedBy.id = :userId ORDER BY s.createdAt DESC")
    Page<Sale> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT s FROM Sale s WHERE s.store = :store AND s.processedBy.id = :userId ORDER BY s.createdAt DESC")
    Page<Sale> findByStoreAndUserId(@Param("store") Store store, @Param("userId") UUID userId, Pageable pageable);
    
    // Today's sales - using YEAR, MONTH, DAY functions like other working queries
    @Query("SELECT s FROM Sale s WHERE YEAR(s.createdAt) = YEAR(CURRENT_DATE) AND MONTH(s.createdAt) = MONTH(CURRENT_DATE) AND DAY(s.createdAt) = DAY(CURRENT_DATE) AND s.status = 'COMPLETED'")
    List<Sale> findTodaySales();
    
    @Query("SELECT s FROM Sale s WHERE s.store = :store AND YEAR(s.createdAt) = YEAR(CURRENT_DATE) AND MONTH(s.createdAt) = MONTH(CURRENT_DATE) AND DAY(s.createdAt) = DAY(CURRENT_DATE) AND s.status = 'COMPLETED'")
    List<Sale> findTodaySalesByStore(@Param("store") Store store);
    
    // Monthly revenue
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE YEAR(s.createdAt) = :year AND MONTH(s.createdAt) = :month AND s.status = 'COMPLETED'")
    BigDecimal getMonthlyRevenue(@Param("year") int year, @Param("month") int month);
    
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.store = :store AND YEAR(s.createdAt) = :year AND MONTH(s.createdAt) = :month AND s.status = 'COMPLETED'")
    BigDecimal getMonthlyRevenueByStore(@Param("store") Store store, @Param("year") int year, @Param("month") int month);
    
    // Top customers by revenue
    @Query("SELECT s.customerEmail, SUM(s.totalAmount) as totalSpent FROM Sale s WHERE s.status = 'COMPLETED' AND s.customerEmail IS NOT NULL GROUP BY s.customerEmail ORDER BY totalSpent DESC")
    List<Object[]> findTopCustomersByRevenue();
    
    @Query("SELECT s.customerEmail, SUM(s.totalAmount) as totalSpent FROM Sale s WHERE s.store = :store AND s.status = 'COMPLETED' AND s.customerEmail IS NOT NULL GROUP BY s.customerEmail ORDER BY totalSpent DESC")
    List<Object[]> findTopCustomersByRevenueByStore(@Param("store") Store store);
}