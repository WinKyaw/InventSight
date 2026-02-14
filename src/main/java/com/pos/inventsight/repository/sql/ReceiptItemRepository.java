package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {
    
    List<ReceiptItem> findByReceiptId(UUID receiptId);
    
    List<ReceiptItem> findByProductId(UUID productId);
}
