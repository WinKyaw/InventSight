package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.TransferRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransferRequestService {
    
    @Autowired
    private TransferRequestRepository transferRequestRepository;
    
    /**
     * Create a new transfer request
     */
    public TransferRequest createTransferRequest(TransferRequest request, Company company, 
                                                 Warehouse warehouse, Store store, 
                                                 User requestedBy) {
        request.setCompany(company);
        request.setFromWarehouse(warehouse);
        request.setToStore(store);
        request.setRequestedBy(requestedBy);
        request.setRequestedAt(LocalDateTime.now());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setStatus(TransferRequestStatus.PENDING);
        
        return transferRequestRepository.save(request);
    }
    
    /**
     * Get all transfer requests for a company
     */
    public List<TransferRequest> getTransferRequestsByCompany(UUID companyId) {
        return transferRequestRepository.findByCompanyId(companyId);
    }
    
    /**
     * Get transfer requests by status
     */
    public List<TransferRequest> getTransferRequestsByStatus(UUID companyId, TransferRequestStatus status) {
        return transferRequestRepository.findByCompanyIdAndStatus(companyId, status);
    }
    
    /**
     * Get pending transfer requests for approval
     */
    public List<TransferRequest> getPendingTransferRequests(UUID companyId) {
        return transferRequestRepository.findPendingRequestsByCompanyId(companyId);
    }
    
    /**
     * Get transfer requests for a store
     */
    public List<TransferRequest> getTransferRequestsByStore(UUID storeId) {
        return transferRequestRepository.findByStoreId(storeId);
    }
    
    /**
     * Get transfer requests for a warehouse
     */
    public List<TransferRequest> getTransferRequestsByWarehouse(UUID warehouseId) {
        return transferRequestRepository.findByWarehouseId(warehouseId);
    }
    
    /**
     * Get transfer request by ID
     */
    public TransferRequest getTransferRequestById(UUID id) {
        return transferRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer request not found with id: " + id));
    }
    
    /**
     * Approve transfer request
     */
    public TransferRequest approveTransferRequest(UUID id, Integer approvedQuantity, User approvedBy) {
        TransferRequest request = getTransferRequestById(id);
        
        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved");
        }
        
        request.setStatus(TransferRequestStatus.APPROVED);
        request.setApprovedQuantity(approvedQuantity);
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        return transferRequestRepository.save(request);
    }
    
    /**
     * Reject transfer request
     */
    public TransferRequest rejectTransferRequest(UUID id, User rejectedBy, String reason) {
        TransferRequest request = getTransferRequestById(id);
        
        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }
        
        request.setStatus(TransferRequestStatus.REJECTED);
        request.setApprovedBy(rejectedBy);
        request.setApprovedAt(LocalDateTime.now());
        if (reason != null) {
            request.setNotes((request.getNotes() != null ? request.getNotes() + "\n" : "") + 
                           "Rejection reason: " + reason);
        }
        request.setUpdatedAt(LocalDateTime.now());
        
        return transferRequestRepository.save(request);
    }
    
    /**
     * Complete transfer request
     */
    public TransferRequest completeTransferRequest(UUID id) {
        TransferRequest request = getTransferRequestById(id);
        
        if (request.getStatus() != TransferRequestStatus.APPROVED) {
            throw new IllegalStateException("Only approved requests can be completed");
        }
        
        request.setStatus(TransferRequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        return transferRequestRepository.save(request);
    }
    
    /**
     * Add notes to transfer request
     */
    public TransferRequest addNotes(UUID id, String notes) {
        TransferRequest request = getTransferRequestById(id);
        request.setNotes((request.getNotes() != null ? request.getNotes() + "\n" : "") + notes);
        request.setUpdatedAt(LocalDateTime.now());
        return transferRequestRepository.save(request);
    }
}
