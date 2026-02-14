package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.TransferReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferReceiptRepository extends JpaRepository<TransferReceipt, UUID> {
    
    Optional<TransferReceipt> findByTransferId(UUID transferId);
    
    Optional<TransferReceipt> findByReceiptId(UUID receiptId);
}
