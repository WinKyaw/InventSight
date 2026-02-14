package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Receipt;
import com.pos.inventsight.model.sql.ReceiptStatus;
import com.pos.inventsight.model.sql.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    
    Page<Receipt> findByCompanyId(UUID companyId, Pageable pageable);
    
    Page<Receipt> findByStoreId(UUID storeId, Pageable pageable);
    
    Page<Receipt> findByStatus(ReceiptStatus status, Pageable pageable);
    
    Page<Receipt> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);
    
    Page<Receipt> findByCompanyIdAndStatus(UUID companyId, ReceiptStatus status, Pageable pageable);
    
    Page<Receipt> findByCompanyIdAndPaymentStatus(UUID companyId, PaymentStatus paymentStatus, Pageable pageable);
    
    @Query("SELECT r FROM Receipt r WHERE r.company.id = :companyId AND r.paymentStatus = 'UNPAID'")
    List<Receipt> findUnpaidReceiptsByCompany(@Param("companyId") UUID companyId);
    
    @Query("SELECT r FROM Receipt r WHERE r.company.id = :companyId AND r.status = 'PENDING'")
    List<Receipt> findPendingReceiptsByCompany(@Param("companyId") UUID companyId);
}
