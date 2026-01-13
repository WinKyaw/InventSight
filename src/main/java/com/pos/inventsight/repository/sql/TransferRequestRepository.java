package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.TransferRequest;
import com.pos.inventsight.model.sql.TransferRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, UUID> {
    
    /**
     * Find all transfer requests for a specific company
     */
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.company.id = :companyId ORDER BY tr.createdAt DESC")
    List<TransferRequest> findByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find transfer requests by status
     */
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.company.id = :companyId AND tr.status = :status ORDER BY tr.createdAt DESC")
    List<TransferRequest> findByCompanyIdAndStatus(@Param("companyId") UUID companyId, @Param("status") TransferRequestStatus status);
    
    /**
     * Find transfer requests for a specific store
     */
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.toStore.id = :storeId ORDER BY tr.createdAt DESC")
    List<TransferRequest> findByStoreId(@Param("storeId") UUID storeId);
    
    /**
     * Find transfer requests for a specific warehouse
     */
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.fromWarehouse.id = :warehouseId ORDER BY tr.createdAt DESC")
    List<TransferRequest> findByWarehouseId(@Param("warehouseId") UUID warehouseId);
    
    /**
     * Find transfer requests by product
     */
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.productId = :productId AND tr.company.id = :companyId ORDER BY tr.createdAt DESC")
    List<TransferRequest> findByProductIdAndCompanyId(@Param("productId") UUID productId, @Param("companyId") UUID companyId);
    
    /**
     * Find pending transfer requests for approval
     */
    @Query("SELECT tr FROM TransferRequest tr WHERE tr.company.id = :companyId AND tr.status = 'PENDING' ORDER BY tr.priority DESC, tr.createdAt ASC")
    List<TransferRequest> findPendingRequestsByCompanyId(@Param("companyId") UUID companyId);
}
