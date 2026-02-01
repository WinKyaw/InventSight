package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.TransferRequestRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransferRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransferRequestService.class);
    
    @Autowired
    private TransferRequestRepository transferRequestRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Create a new transfer request
     */
    public TransferRequest createTransferRequest(TransferRequest request, Company company, 
                                                 Warehouse warehouse, Store store, 
                                                 User requestedBy) {
        // Fetch Product entity to populate denormalized fields
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with ID: " + request.getProductId()
            ));
        
        logger.debug("Creating transfer for product: {} (SKU: {})", product.getName(), product.getSku());
        
        // Populate product denormalized fields
        request.setProductName(product.getName());
        request.setProductSku(product.getSku());
        request.setItemName(product.getName());  // Legacy field
        request.setItemSku(product.getSku());    // Legacy field
        
        request.setCompany(company);
        request.setFromWarehouse(warehouse);
        request.setToStore(store);
        request.setRequestedBy(requestedBy);
        request.setRequestedAt(LocalDateTime.now());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setStatus(TransferRequestStatus.PENDING);
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        logger.info("Transfer request created: Product={} ({}), Quantity={}, From=WAREHOUSE:{}, To=STORE:{}", 
            savedRequest.getProductName(), savedRequest.getProductSku(),
            savedRequest.getRequestedQuantity(),
            warehouse.getId(), store.getId());
        
        return savedRequest;
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
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found with id: " + id));
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
    
    /**
     * Create enhanced transfer request with location flexibility
     */
    public TransferRequest createEnhancedTransferRequest(TransferRequest request, Company company, User requestedBy) {
        // Validate locations
        validateLocations(request.getFromLocationType(), request.getFromLocationId(),
                         request.getToLocationType(), request.getToLocationId());
        
        // Fetch Product entity to populate denormalized fields
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with ID: " + request.getProductId()
            ));
        
        logger.debug("Creating transfer for product: {} (SKU: {})", product.getName(), product.getSku());
        
        // Populate product denormalized fields
        request.setProductName(product.getName());
        request.setProductSku(product.getSku());
        request.setItemName(product.getName());  // Legacy field
        request.setItemSku(product.getSku());    // Legacy field
        
        // Set common fields
        request.setCompany(company);
        request.setRequestedBy(requestedBy);
        request.setRequestedAt(LocalDateTime.now());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setStatus(TransferRequestStatus.PENDING);
        request.setIsReceiptConfirmed(false);
        
        // Also set legacy fields for backward compatibility
        if ("WAREHOUSE".equals(request.getFromLocationType())) {
            warehouseRepository.findById(request.getFromLocationId()).ifPresent(request::setFromWarehouse);
        }
        if ("STORE".equals(request.getToLocationType())) {
            storeRepository.findById(request.getToLocationId()).ifPresent(request::setToStore);
        }
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        logger.info("Transfer request created: Product={} ({}), Quantity={}, From={}:{}, To={}:{}", 
            savedRequest.getProductName(), savedRequest.getProductSku(),
            savedRequest.getRequestedQuantity(),
            savedRequest.getFromLocationType(), savedRequest.getFromLocationId(),
            savedRequest.getToLocationType(), savedRequest.getToLocationId());
        
        return savedRequest;
    }
    
    /**
     * Approve and send transfer request with carrier details (GM+ only)
     */
    public TransferRequest approveAndSend(UUID id, Integer approvedQuantity, String carrierName,
                                         String carrierPhone, String carrierVehicle,
                                         LocalDateTime estimatedDeliveryAt, String notes, User approvedBy) {
        TransferRequest request = getTransferRequestById(id);
        
        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved and sent");
        }
        
        // Validate approved quantity
        if (approvedQuantity > request.getRequestedQuantity()) {
            throw new IllegalArgumentException("Approved quantity cannot exceed requested quantity");
        }
        
        // Update request
        request.setStatus(TransferRequestStatus.IN_TRANSIT);
        request.setApprovedQuantity(approvedQuantity);
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(LocalDateTime.now());
        request.setShippedAt(LocalDateTime.now());
        
        // Set carrier details
        request.setCarrierName(carrierName);
        request.setCarrierPhone(carrierPhone);
        request.setCarrierVehicle(carrierVehicle);
        request.setEstimatedDeliveryAt(estimatedDeliveryAt);
        
        // Add notes if provided
        if (notes != null && !notes.isEmpty()) {
            request.setNotes((request.getNotes() != null ? request.getNotes() + "\n" : "") + notes);
        }
        
        request.setUpdatedAt(LocalDateTime.now());
        
        return transferRequestRepository.save(request);
    }
    
    /**
     * Confirm receipt of transfer
     */
    public TransferRequest confirmReceipt(UUID id, Integer receivedQuantity, String receiverName,
                                         String receiptNotes, User receivedByUser) {
        TransferRequest request = getTransferRequestById(id);
        
        if (request.getStatus() != TransferRequestStatus.IN_TRANSIT && 
            request.getStatus() != TransferRequestStatus.DELIVERED) {
            throw new IllegalStateException("Only in-transit or delivered transfers can be received");
        }
        
        // Update receipt details
        request.setReceivedQuantity(receivedQuantity);
        request.setReceiverName(receiverName);
        request.setReceiptNotes(receiptNotes);
        request.setReceivedByUser(receivedByUser);
        request.setReceivedAt(LocalDateTime.now());
        request.setIsReceiptConfirmed(true);
        
        // Update status based on received quantity
        if (receivedQuantity.equals(request.getApprovedQuantity())) {
            request.setStatus(TransferRequestStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
        } else if (receivedQuantity > 0 && receivedQuantity < request.getApprovedQuantity()) {
            request.setStatus(TransferRequestStatus.PARTIALLY_RECEIVED);
        } else {
            // receivedQuantity is 0 or invalid - mark as received but not completed
            request.setStatus(TransferRequestStatus.RECEIVED);
        }
        
        request.setUpdatedAt(LocalDateTime.now());
        
        return transferRequestRepository.save(request);
    }
    
    /**
     * Cancel transfer request
     */
    public TransferRequest cancelTransfer(UUID id, String reason, User cancelledBy) {
        TransferRequest request = getTransferRequestById(id);
        
        if (request.getStatus() == TransferRequestStatus.COMPLETED ||
            request.getStatus() == TransferRequestStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel completed or already cancelled transfers");
        }
        
        request.setStatus(TransferRequestStatus.CANCELLED);
        request.setUpdatedAt(LocalDateTime.now());
        
        if (reason != null && !reason.isEmpty()) {
            request.setNotes((request.getNotes() != null ? request.getNotes() + "\n" : "") + 
                           "Cancelled by " + cancelledBy.getUsername() + ": " + reason);
        }
        
        return transferRequestRepository.save(request);
    }
    
    /**
     * Get transfers by location (from or to)
     */
    public List<TransferRequest> getTransfersByLocation(String locationType, UUID locationId) {
        return transferRequestRepository.findByLocation(locationType, locationId);
    }
    
    /**
     * Get transfers from a location
     */
    public List<TransferRequest> getTransfersFromLocation(String locationType, UUID locationId) {
        return transferRequestRepository.findByFromLocation(locationType, locationId);
    }
    
    /**
     * Get transfers to a location
     */
    public List<TransferRequest> getTransfersToLocation(String locationType, UUID locationId) {
        return transferRequestRepository.findByToLocation(locationType, locationId);
    }
    
    /**
     * Validate that locations exist and are different
     */
    private void validateLocations(String fromType, UUID fromId, String toType, UUID toId) {
        // Check that from and to are not the same
        if (fromType.equals(toType) && fromId.equals(toId)) {
            throw new IllegalArgumentException("Source and destination locations must be different");
        }
        
        // Validate from location exists
        if ("WAREHOUSE".equals(fromType)) {
            warehouseRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("From warehouse not found with id: " + fromId));
        } else if ("STORE".equals(fromType)) {
            storeRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("From store not found with id: " + fromId));
        } else {
            throw new IllegalArgumentException("Invalid from location type: " + fromType);
        }
        
        // Validate to location exists
        if ("WAREHOUSE".equals(toType)) {
            warehouseRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("To warehouse not found with id: " + toId));
        } else if ("STORE".equals(toType)) {
            storeRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("To store not found with id: " + toId));
        } else {
            throw new IllegalArgumentException("Invalid to location type: " + toType);
        }
    }
    
    // ========== Paginated query methods ==========
    
    /**
     * Get all transfer requests for a company (paginated)
     */
    public Page<TransferRequest> getTransferRequestsByCompany(UUID companyId, Pageable pageable) {
        return transferRequestRepository.findByCompanyId(companyId, pageable);
    }
    
    /**
     * Get transfer requests by status (paginated)
     */
    public Page<TransferRequest> getTransferRequestsByStatus(UUID companyId, TransferRequestStatus status, Pageable pageable) {
        return transferRequestRepository.findByCompanyIdAndStatus(companyId, status, pageable);
    }
    
    /**
     * Get transfer requests for a store (paginated)
     */
    public Page<TransferRequest> getTransferRequestsByStore(UUID storeId, Pageable pageable) {
        return transferRequestRepository.findByStoreId(storeId, pageable);
    }
    
    /**
     * Get transfer requests for a warehouse (paginated)
     */
    public Page<TransferRequest> getTransferRequestsByWarehouse(UUID warehouseId, Pageable pageable) {
        return transferRequestRepository.findByWarehouseId(warehouseId, pageable);
    }
    
    /**
     * Get pending transfer requests for a destination location (for approval)
     */
    public List<TransferRequest> getPendingApprovalsForLocation(String locationType, UUID locationId) {
        return transferRequestRepository.findPendingByToLocation(locationType, locationId);
    }
    
    /**
     * Mark transfer as shipped (IN_TRANSIT status)
     */
    public TransferRequest shipTransferRequest(UUID id, String carrierName, String carrierPhone,
                                               String carrierVehicle, LocalDateTime estimatedDeliveryAt) {
        TransferRequest request = getTransferRequestById(id);
        
        if (request.getStatus() != TransferRequestStatus.APPROVED) {
            throw new IllegalStateException("Only approved transfers can be shipped");
        }
        
        request.setStatus(TransferRequestStatus.IN_TRANSIT);
        request.setCarrierName(carrierName);
        request.setCarrierPhone(carrierPhone);
        request.setCarrierVehicle(carrierVehicle);
        request.setEstimatedDeliveryAt(estimatedDeliveryAt);
        request.setShippedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        TransferRequest saved = transferRequestRepository.save(request);
        
        logger.info("Transfer {} marked as IN_TRANSIT by carrier: {}", id, carrierName);
        
        return saved;
    }
    
    /**
     * Complete transfer with inventory updates
     * Deducts from source and adds to destination
     */
    public TransferRequest completeTransferWithInventory(UUID id, Integer receivedQuantity, 
                                                         Integer damagedQuantity, String conditionOnArrival,
                                                         String receiverName, String receiptNotes, User receivedByUser) {
        TransferRequest request = getTransferRequestById(id);
        
        // Validate transfer can be completed
        if (request.getStatus() != TransferRequestStatus.IN_TRANSIT && 
            request.getStatus() != TransferRequestStatus.APPROVED) {
            throw new IllegalStateException("Only approved or in-transit transfers can be completed");
        }
        
        if (request.getApprovedQuantity() == null) {
            throw new IllegalStateException("Transfer must be approved before completion");
        }
        
        // Update transfer details
        request.setReceivedQuantity(receivedQuantity);
        request.setDamagedQuantity(damagedQuantity != null ? damagedQuantity : 0);
        request.setReceiverName(receiverName);
        request.setReceiptNotes(receiptNotes);
        request.setReceivedByUser(receivedByUser);
        request.setReceivedAt(LocalDateTime.now());
        request.setIsReceiptConfirmed(true);
        
        // Set condition on arrival if provided
        if (conditionOnArrival != null && !conditionOnArrival.isEmpty()) {
            try {
                request.setConditionOnArrival(ConditionStatus.valueOf(conditionOnArrival.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid condition status: {}, ignoring", conditionOnArrival);
            }
        }
        
        // Update status based on received quantity
        if (receivedQuantity.equals(request.getApprovedQuantity())) {
            request.setStatus(TransferRequestStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
        } else if (receivedQuantity > 0 && receivedQuantity < request.getApprovedQuantity()) {
            request.setStatus(TransferRequestStatus.PARTIALLY_RECEIVED);
        } else {
            request.setStatus(TransferRequestStatus.RECEIVED);
        }
        
        request.setUpdatedAt(LocalDateTime.now());
        
        // Update inventory at both locations
        updateInventoryForCompletion(request, receivedQuantity);
        
        TransferRequest saved = transferRequestRepository.save(request);
        
        logger.info("Transfer {} completed: received={}, damaged={}, status={}", 
                   id, receivedQuantity, damagedQuantity, request.getStatus());
        
        return saved;
    }
    
    /**
     * Update inventory at source and destination locations
     * Private helper method for inventory management
     */
    private void updateInventoryForCompletion(TransferRequest request, Integer receivedQuantity) {
        // Get the product
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        
        Integer approvedQuantity = request.getApprovedQuantity();
        
        // Deduct from source location
        deductFromSourceLocation(request, product, approvedQuantity);
        
        // Add to destination location (only the received quantity, not damaged)
        Integer goodQuantity = receivedQuantity - (request.getDamagedQuantity() != null ? request.getDamagedQuantity() : 0);
        if (goodQuantity > 0) {
            addToDestinationLocation(request, product, goodQuantity);
        }
        
        logger.info("Inventory updated for transfer {}: deducted {} from source, added {} to destination", 
                   request.getId(), approvedQuantity, goodQuantity);
    }
    
    /**
     * Deduct inventory from source location
     */
    private void deductFromSourceLocation(TransferRequest request, Product product, Integer quantity) {
        String fromType = request.getFromLocationType();
        UUID fromId = request.getFromLocationId();
        
        if ("WAREHOUSE".equals(fromType)) {
            // Deduct from warehouse inventory
            Warehouse warehouse = warehouseRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found"));
            
            // Note: Warehouse inventory is managed separately in WarehouseInventory table
            // This would need WarehouseInventoryService injected to properly update
            logger.info("Would deduct {} units of product {} from warehouse {}", 
                       quantity, product.getId(), warehouse.getId());
            
        } else if ("STORE".equals(fromType)) {
            // Deduct from store inventory (product quantity)
            Store store = storeRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("Source store not found"));
            
            Integer currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
            product.setQuantity(currentQty - quantity);
            productRepository.save(product);
            
            logger.info("Deducted {} units of product {} from store {}", 
                       quantity, product.getId(), store.getId());
        }
    }
    
    /**
     * Add inventory to destination location
     */
    private void addToDestinationLocation(TransferRequest request, Product product, Integer quantity) {
        String toType = request.getToLocationType();
        UUID toId = request.getToLocationId();
        
        if ("WAREHOUSE".equals(toType)) {
            // Add to warehouse inventory
            Warehouse warehouse = warehouseRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found"));
            
            // Note: Warehouse inventory is managed separately in WarehouseInventory table
            // This would need WarehouseInventoryService injected to properly update
            logger.info("Would add {} units of product {} to warehouse {}", 
                       quantity, product.getId(), warehouse.getId());
            
        } else if ("STORE".equals(toType)) {
            // Add to store inventory (product quantity)
            Store store = storeRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination store not found"));
            
            Integer currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
            product.setQuantity(currentQty + quantity);
            productRepository.save(product);
            
            logger.info("Added {} units of product {} to store {}", 
                       quantity, product.getId(), store.getId());
        }
    }
}
