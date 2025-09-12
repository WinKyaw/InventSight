package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.DiscountAuditLog;
import com.pos.inventsight.model.sql.DiscountResult;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiscountAuditLogRepository extends JpaRepository<DiscountAuditLog, Long> {
    
    List<DiscountAuditLog> findByUserOrderByTimestampDesc(User user);
    
    List<DiscountAuditLog> findByStoreOrderByTimestampDesc(Store store);
    
    List<DiscountAuditLog> findByProductOrderByTimestampDesc(Product product);
    
    List<DiscountAuditLog> findByResult(DiscountResult result);
    
    List<DiscountAuditLog> findByStoreAndResult(Store store, DiscountResult result);
    
    @Query("SELECT dal FROM DiscountAuditLog dal WHERE dal.store = :store AND dal.timestamp BETWEEN :startDate AND :endDate ORDER BY dal.timestamp DESC")
    List<DiscountAuditLog> findByStoreAndTimestampBetween(@Param("store") Store store, 
                                                          @Param("startDate") LocalDateTime startDate, 
                                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT dal FROM DiscountAuditLog dal WHERE dal.user = :user AND dal.timestamp BETWEEN :startDate AND :endDate ORDER BY dal.timestamp DESC")
    List<DiscountAuditLog> findByUserAndTimestampBetween(@Param("user") User user, 
                                                         @Param("startDate") LocalDateTime startDate, 
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT dal FROM DiscountAuditLog dal WHERE dal.store = :store AND dal.role = :role ORDER BY dal.timestamp DESC")
    List<DiscountAuditLog> findByStoreAndRole(@Param("store") Store store, @Param("role") UserRole role);
    
    @Query("SELECT COUNT(dal) FROM DiscountAuditLog dal WHERE dal.store = :store AND dal.result = :result")
    long countByStoreAndResult(@Param("store") Store store, @Param("result") DiscountResult result);
    
    @Query("SELECT COUNT(dal) FROM DiscountAuditLog dal WHERE dal.user = :user AND dal.result = :result")
    long countByUserAndResult(@Param("user") User user, @Param("result") DiscountResult result);
    
    @Query("SELECT dal FROM DiscountAuditLog dal WHERE dal.sessionId = :sessionId ORDER BY dal.timestamp")
    List<DiscountAuditLog> findBySessionId(@Param("sessionId") String sessionId);
    
    @Query("SELECT COUNT(dal) FROM DiscountAuditLog dal WHERE dal.store = :store AND dal.timestamp >= :startDate")
    long countRecentDiscountAttempts(@Param("store") Store store, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT dal FROM DiscountAuditLog dal WHERE dal.result = 'PENDING_APPROVAL' ORDER BY dal.timestamp ASC")
    List<DiscountAuditLog> findPendingApprovals();
    
    @Query("SELECT dal FROM DiscountAuditLog dal WHERE dal.store = :store AND dal.result = 'PENDING_APPROVAL' ORDER BY dal.timestamp ASC")
    List<DiscountAuditLog> findPendingApprovalsByStore(@Param("store") Store store);
}