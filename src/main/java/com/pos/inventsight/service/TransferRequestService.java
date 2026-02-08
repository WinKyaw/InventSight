package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.TransferRequestRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.WarehouseInventoryRepository;
import com.pos.inventsight.repository.sql.StoreInventoryAdditionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransferRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransferRequestService.class);
    private static final int TRANSFER_ID_PREFIX_LENGTH = 8;
    
    @Autowired
    private TransferRequestRepository transferRequestRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private WarehouseInventoryRepository warehouseInventoryRepository;
    
    @Autowired
    private StoreInventoryAdditionRepository additionRepository;
    
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
        
        // ✅ DEDUCT FROM SOURCE WAREHOUSE/STORE IMMEDIATELY (direct APPROVED → IN_TRANSIT)
        if ("WAREHOUSE".equals(request.getFromLocationType())) {
            deductFromWarehouseInventory(
                request.getFromLocationId(), 
                request.getProductId(), 
                approvedQuantity, 
                request
            );
            logger.info("✅ Deducted {} units from warehouse {} for transfer {} (approve and send)", 
                approvedQuantity, 
                request.getFromLocationId(), 
                id);
        } else if ("STORE".equals(request.getFromLocationType())) {
            deductFromStoreInventory(request.getProductId(), approvedQuantity);
            logger.info("✅ Deducted {} units from store for transfer {} (approve and send)", 
                approvedQuantity, 
                id);
        }
        
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
        if (Objects.equals(receivedQuantity, request.getApprovedQuantity())) {
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
            // TODO: Implement warehouse inventory deduction
            // Deduct from warehouse inventory - requires WarehouseInventoryService injection
            Warehouse warehouse = warehouseRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found"));
            
            // Note: Warehouse inventory is managed separately in WarehouseInventory table
            // This would need WarehouseInventoryService injected to properly update
            logger.warn("Warehouse inventory deduction not fully implemented. Would deduct {} units of product {} from warehouse {}", 
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
            // TODO: Implement warehouse inventory addition
            // Add to warehouse inventory - requires WarehouseInventoryService injection
            Warehouse warehouse = warehouseRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found"));
            
            // Note: Warehouse inventory is managed separately in WarehouseInventory table
            // This would need WarehouseInventoryService injected to properly update
            logger.warn("Warehouse inventory addition not fully implemented. Would add {} units of product {} to warehouse {}", 
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
    
    /**
     * Mark transfer as ready for pickup
     */
    public TransferRequest markAsReady(UUID transferId, String packedBy, String notes, User handler) {
        TransferRequest transfer = getTransferRequestById(transferId);
        
        if (transfer.getStatus() != TransferRequestStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED transfers can be marked as ready");
        }
        
        // Check inventory availability
        boolean inventoryAvailable = checkInventoryAvailability(transfer);
        if (!inventoryAvailable) {
            throw new IllegalStateException("Insufficient inventory available to fulfill this transfer");
        }
        
        transfer.setStatus(TransferRequestStatus.READY);
        transfer.setPackedBy(packedBy != null ? packedBy : handler.getFullName());
        transfer.setPackedAt(LocalDateTime.now());
        
        if (notes != null && !notes.isEmpty()) {
            transfer.setPackingNotes(notes);
        }
        
        // Also set legacy handler fields for backward compatibility
        transfer.setHandlerName(packedBy != null ? packedBy : handler.getFullName());
        transfer.setHandlerUserId(handler.getId());
        transfer.setUpdatedAt(LocalDateTime.now());
        
        TransferRequest saved = transferRequestRepository.save(transfer);
        
        logger.info("Transfer {} marked as READY by {}", transferId, saved.getPackedBy());
        
        return saved;
    }
    
    /**
     * Check if inventory is available for the transfer
     */
    private boolean checkInventoryAvailability(TransferRequest transfer) {
        try {
            // Get available inventory at source location
            if ("WAREHOUSE".equals(transfer.getFromLocationType())) {
                // Check warehouse inventory
                Optional<WarehouseInventory> inventoryOpt = warehouseInventoryRepository
                    .findByWarehouseIdAndProductId(
                        transfer.getFromLocationId(),
                        transfer.getProductId()
                    );
                
                if (!inventoryOpt.isPresent()) {
                    logger.warn("No inventory record found for product {} at warehouse {}", 
                        transfer.getProductId(), transfer.getFromLocationId());
                    return false;
                }
                
                WarehouseInventory inventory = inventoryOpt.get();
                int availableQuantity = inventory.getAvailableQuantity(); // Excludes held, damaged, expired
                return availableQuantity >= transfer.getApprovedQuantity();
                
            } else if ("STORE".equals(transfer.getFromLocationType())) {
                // Check store inventory (stored in Product.quantity)
                Optional<Product> productOpt = productRepository.findById(transfer.getProductId());
                
                if (!productOpt.isPresent()) {
                    logger.warn("Product {} not found", transfer.getProductId());
                    return false;
                }
                
                Product product = productOpt.get();
                
                // Verify product belongs to the source store
                if (product.getStore() == null || 
                    !product.getStore().getId().equals(transfer.getFromLocationId())) {
                    logger.warn("Product {} is not at store {}", 
                        transfer.getProductId(), transfer.getFromLocationId());
                    return false;
                }
                
                Integer availableQuantity = product.getQuantity();
                return availableQuantity != null && availableQuantity >= transfer.getApprovedQuantity();
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error checking inventory availability", e);
            return false;
        }
    }
    
    /**
     * Generate QR code for delivery verification
     */
    public String generateDeliveryQRCode(UUID transferId, User deliveryPerson) {
        try {
            String data = transferId.toString() + ":" + deliveryPerson.getId().toString() + ":" + System.currentTimeMillis();
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    
    /**
     * Pickup and start delivery
     */
    public TransferRequest pickupTransfer(UUID transferId, String carrierName, String carrierPhone,
                                         String carrierVehicle, LocalDateTime estimatedDeliveryAt,
                                         String qrCodeData, User carrier) {
        TransferRequest transfer = getTransferRequestById(transferId);
        
        // Only allow pickup from READY status (enforces proper workflow)
        if (transfer.getStatus() != TransferRequestStatus.READY) {
            throw new IllegalStateException("Only READY transfers can be picked up. Current status: " + transfer.getStatus());
        }
        
        // ✅ DEDUCT FROM SOURCE WAREHOUSE/STORE BEFORE MARKING IN_TRANSIT
        if ("WAREHOUSE".equals(transfer.getFromLocationType())) {
            deductFromWarehouseInventory(
                transfer.getFromLocationId(), 
                transfer.getProductId(), 
                transfer.getApprovedQuantity(), 
                transfer
            );
            logger.info("✅ Deducted {} units from warehouse {} for transfer {}", 
                transfer.getApprovedQuantity(), 
                transfer.getFromLocationId(), 
                transferId);
        } else if ("STORE".equals(transfer.getFromLocationType())) {
            deductFromStoreInventory(transfer.getProductId(), transfer.getApprovedQuantity());
            logger.info("✅ Deducted {} units from store for transfer {}", 
                transfer.getApprovedQuantity(), 
                transferId);
        }
        
        transfer.setStatus(TransferRequestStatus.IN_TRANSIT);
        transfer.setCarrierName(carrierName);
        transfer.setCarrierPhone(carrierPhone);
        transfer.setCarrierVehicle(carrierVehicle);
        transfer.setCarrierUserId(carrier.getId());
        transfer.setEstimatedDeliveryAt(estimatedDeliveryAt);
        transfer.setShippedAt(LocalDateTime.now());
        
        // Store QR code temporarily in proofOfDeliveryUrl field
        // Note: This field serves dual purpose - QR code during transit, proof URL after delivery
        // The QR code is replaced with actual proof URL when markAsDelivered() is called
        transfer.setProofOfDeliveryUrl(qrCodeData);
        transfer.setUpdatedAt(LocalDateTime.now());
        
        TransferRequest saved = transferRequestRepository.save(transfer);
        
        logger.info("Transfer {} picked up by carrier: {} and marked IN_TRANSIT with inventory deducted", 
            transferId, carrierName);
        
        return saved;
    }
    
    /**
     * Mark as delivered
     */
    public TransferRequest markAsDelivered(UUID transferId, String proofUrl, String condition, User carrier) {
        TransferRequest transfer = getTransferRequestById(transferId);
        
        if (transfer.getStatus() != TransferRequestStatus.IN_TRANSIT) {
            throw new IllegalStateException("Only IN_TRANSIT transfers can be marked as delivered");
        }
        
        transfer.setStatus(TransferRequestStatus.DELIVERED);
        
        // Set condition if provided
        if (condition != null && !condition.isEmpty()) {
            try {
                transfer.setConditionOnArrival(ConditionStatus.valueOf(condition.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid condition status: {}, ignoring", condition);
            }
        }
        
        // Update proof of delivery if a new URL is provided
        if (proofUrl != null && !proofUrl.isEmpty()) {
            // Keep the QR code and append proof URL in notes
            String notes = transfer.getNotes() != null ? transfer.getNotes() + "\n" : "";
            notes += "Proof of delivery: " + proofUrl;
            transfer.setNotes(notes);
        }
        
        transfer.setReceivedAt(LocalDateTime.now());
        transfer.setUpdatedAt(LocalDateTime.now());
        
        TransferRequest saved = transferRequestRepository.save(transfer);
        
        logger.info("Transfer {} marked as DELIVERED", transferId);
        
        return saved;
    }
    
    /**
     * Verify delivery QR code
     */
    public boolean verifyDeliveryQRCode(UUID transferId, String qrCode) {
        TransferRequest transfer = getTransferRequestById(transferId);
        String storedQR = transfer.getProofOfDeliveryUrl();
        return storedQR != null && storedQR.equals(qrCode);
    }
    
    /**
     * Receive and complete transfer with QR verification
     */
    public TransferRequest receiveTransfer(UUID transferId, Integer receivedQuantity, Integer damagedQuantity,
                                          String receiverName, String signatureUrl, String notes, User receiver) {
        TransferRequest transfer = getTransferRequestById(transferId);
        
        if (transfer.getStatus() != TransferRequestStatus.DELIVERED) {
            throw new IllegalStateException("Only DELIVERED transfers can be received");
        }
        
        // Validate quantities
        if (receivedQuantity == null || receivedQuantity < 0) {
            throw new IllegalArgumentException("Received quantity must be non-negative");
        }
        
        if (damagedQuantity != null && damagedQuantity < 0) {
            throw new IllegalArgumentException("Damaged quantity must be non-negative");
        }
        
        // Update transfer
        transfer.setStatus(TransferRequestStatus.COMPLETED);
        transfer.setReceivedQuantity(receivedQuantity);
        transfer.setDamagedQuantity(damagedQuantity != null ? damagedQuantity : 0);
        transfer.setReceiverName(receiverName);
        transfer.setReceiverSignatureUrl(signatureUrl);
        transfer.setReceiptNotes(notes);
        transfer.setReceivedByUser(receiver);
        transfer.setReceiverUserId(receiver.getId());
        transfer.setCompletedAt(LocalDateTime.now());
        transfer.setIsReceiptConfirmed(true);
        transfer.setReceivedAt(LocalDateTime.now());
        transfer.setUpdatedAt(LocalDateTime.now());
        
        // Update inventory - use enhanced version with warehouse support
        updateInventoryForTransferCompletion(transfer);
        
        TransferRequest saved = transferRequestRepository.save(transfer);
        
        logger.info("Transfer {} received and completed: received={}, damaged={}", 
                   transferId, receivedQuantity, damagedQuantity);
        
        return saved;
    }
    
    /**
     * Update inventory when transfer is completed - with warehouse inventory support
     */
    private void updateInventoryForTransferCompletion(TransferRequest transfer) {
        Integer receivedQty = transfer.getReceivedQuantity();
        Integer damagedQty = transfer.getDamagedQuantity() != null ? transfer.getDamagedQuantity() : 0;
        Integer goodQuantity = receivedQty - damagedQty;
        
        if (goodQuantity < 0) {
            throw new IllegalStateException("Damaged quantity cannot exceed received quantity");
        }
        
        // ✅ SOURCE DEDUCTION ALREADY HAPPENED AT IN_TRANSIT STATUS
        // We only need to ADD to destination here
        
        // Add to destination location (only good items, excluding damaged)
        if (goodQuantity > 0) {
            if ("WAREHOUSE".equals(transfer.getToLocationType())) {
                addToWarehouseInventory(transfer.getToLocationId(), transfer.getProductId(), goodQuantity);
                logger.info("✅ Added {} units to destination warehouse {}", 
                    goodQuantity, transfer.getToLocationId());
            } else if ("STORE".equals(transfer.getToLocationType())) {
                addToStoreInventory(transfer.getProductId(), goodQuantity, transfer);
                logger.info("✅ Added {} units to destination store with restock record", goodQuantity);
            }
        }
        
        logger.info("Inventory completion for transfer {}: added {} to destination (damaged: {})", 
                   transfer.getId(), goodQuantity, damagedQty);
    }
    
    /**
     * Deduct inventory from warehouse
     */
    private void deductFromWarehouseInventory(UUID warehouseId, UUID productId, Integer quantity, TransferRequest transfer) {
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseIdAndProductId(warehouseId, productId)
            .orElseThrow(() -> new RuntimeException("Source warehouse inventory not found"));
        
        Integer currentQty = inventory.getCurrentQuantity() != null ? inventory.getCurrentQuantity() : 0;
        if (currentQty < quantity) {
            throw new IllegalStateException("Insufficient inventory in warehouse. Available: " + currentQty + ", Required: " + quantity);
        }
        
        inventory.setCurrentQuantity(currentQty - quantity);
        warehouseInventoryRepository.save(inventory);
        
        // Log warehouse shipment
        logWarehouseShipment(warehouseId, productId, quantity, transfer);
        
        logger.info("Deducted {} units from warehouse {} for product {}", quantity, warehouseId, productId);
    }
    
    /**
     * Add inventory to warehouse
     */
    private void addToWarehouseInventory(UUID warehouseId, UUID productId, Integer quantity) {
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseIdAndProductId(warehouseId, productId)
            .orElseGet(() -> {
                // Create new inventory record if it doesn't exist
                WarehouseInventory newInventory = new WarehouseInventory();
                Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new RuntimeException("Warehouse not found"));
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
                newInventory.setWarehouse(warehouse);
                newInventory.setProduct(product);
                newInventory.setCurrentQuantity(0);
                newInventory.setReservedQuantity(0);
                return newInventory;
            });
        
        Integer currentQty = inventory.getCurrentQuantity() != null ? inventory.getCurrentQuantity() : 0;
        inventory.setCurrentQuantity(currentQty + quantity);
        warehouseInventoryRepository.save(inventory);
        
        logger.info("Added {} units to warehouse {} for product {}", quantity, warehouseId, productId);
    }
    
    /**
     * Deduct inventory from store (product quantity)
     */
    private void deductFromStoreInventory(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Integer currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
        if (currentQty < quantity) {
            throw new IllegalStateException("Insufficient product inventory. Available: " + currentQty + ", Required: " + quantity);
        }
        
        product.setQuantity(currentQty - quantity);
        productRepository.save(product);
        
        logger.info("Deducted {} units from store inventory for product {}", quantity, productId);
    }
    
    /**
     * Add inventory to store (product quantity) and create restock history
     */
    private void addToStoreInventory(UUID productId, Integer quantity, TransferRequest transfer) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Get the destination store from the transfer's toLocationId
        // This is the correct source - transfer knows the destination store
        Store destinationStore = storeRepository.findById(transfer.getToLocationId())
            .orElseThrow(() -> new RuntimeException("Destination store not found for transfer to location ID: " + transfer.getToLocationId()));
        
        // Update product quantity
        Integer currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
        product.setQuantity(currentQty + quantity);
        productRepository.save(product);
        
        // Build transfer source description for notes
        String sourceDescription = getTransferSourceDescription(transfer);
        
        // Create restock history record using the destination store from transfer
        StoreInventoryAddition restockRecord = new StoreInventoryAddition(
            destinationStore,
            product,
            quantity
        );
        restockRecord.setTransactionType(StoreInventoryAddition.TransactionType.TRANSFER_IN);
        restockRecord.setReferenceNumber("TRANSFER-" + transfer.getId().toString().substring(0, TRANSFER_ID_PREFIX_LENGTH));
        restockRecord.setNotes("Received from transfer request #" + transfer.getId().toString().substring(0, TRANSFER_ID_PREFIX_LENGTH) + sourceDescription);
        restockRecord.setCreatedBy(transfer.getReceivedByUser() != null ? 
            transfer.getReceivedByUser().getUsername() : "system");
        restockRecord.setReceiptDate(LocalDate.now());
        restockRecord.setStatus(StoreInventoryAddition.TransactionStatus.COMPLETED);
        
        additionRepository.save(restockRecord);
        
        logger.info("✅ Added {} units to store {} inventory for product {} and created restock history record from transfer {}", 
            quantity, destinationStore.getStoreName(), productId, transfer.getId());
    }
    
    /**
     * Get the source location description for transfer notes
     */
    private String getTransferSourceDescription(TransferRequest transfer) {
        if (transfer.getFromWarehouse() != null) {
            return " from warehouse: " + transfer.getFromWarehouse().getName();
        } else if (transfer.getFromStore() != null) {
            return " from store: " + transfer.getFromStore().getStoreName();
        } else {
            return " from Unknown";
        }
    }
    
    /**
     * Log warehouse shipment transaction
     */
    private void logWarehouseShipment(UUID warehouseId, UUID productId, Integer quantity, TransferRequest transfer) {
        // Log warehouse shipment in activity log
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        String activity = String.format(
            "Shipped %d units of %s (SKU: %s) from warehouse %s via transfer #%s",
            quantity,
            product.getName(),
            product.getSku(),
            warehouse.getName(),
            transfer.getId().toString().substring(0, TRANSFER_ID_PREFIX_LENGTH)
        );
        
        // Log to activity log service if available
        logger.info("📦 Warehouse shipment: {}", activity);
    }
    
    /**
     * Get pending approvals for user based on their locations and role
     */
    public List<TransferRequest> getPendingApprovalsForUser(UUID companyId, List<Store> userStores,
                                                            List<Warehouse> userWarehouses, boolean isGM) {
        if (isGM) {
            // GM sees all pending transfers in company
            return transferRequestRepository.findByCompanyIdAndStatus(
                companyId,
                TransferRequestStatus.PENDING
            );
        } else {
            // Regular users see pending transfers to their locations
            List<UUID> storeIds = userStores.stream().map(Store::getId).toList();
            List<UUID> warehouseIds = userWarehouses.stream().map(Warehouse::getId).toList();
            
            if (storeIds.isEmpty() && warehouseIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            
            return transferRequestRepository.findPendingTransfersForLocations(
                storeIds,
                warehouseIds,
                TransferRequestStatus.PENDING
            );
        }
    }
}
