package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.SaleReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleReceiptRepository extends JpaRepository<SaleReceipt, UUID> {
    
    Optional<SaleReceipt> findBySaleId(Long saleId);
    
    Optional<SaleReceipt> findByReceiptId(UUID receiptId);
}
