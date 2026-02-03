package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.exception.UnauthorizedException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.service.TransferRequestService;
import com.pos.inventsight.service.TransferPermissionService;
import com.pos.inventsight.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/transfers")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransferRequestController {
    
    @Autowired
    private TransferRequestService transferRequestService;
    
    @Autowired
    private TransferPermissionService transferPermissionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    /**
     * Helper method to get user's company
     */
    private Company getUserCompany(User user) {
        List<Company> companies = companyStoreUserRepository.findCompaniesByUser(user);
        if (companies.isEmpty()) {
            return null;
        }
        return companies.get(0); // Return first company
    }
    
    /**
     * POST /transfers/request - Create enhanced transfer request
     */
    @PostMapping("/request")
    public ResponseEntity<?> createEnhancedTransferRequest(@Valid @RequestBody CreateTransferRequestDTO requestData,
                                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            // Create transfer request with enhanced fields
            TransferRequest request = new TransferRequest();
            request.setFromLocationType(requestData.getFromLocationType());
            request.setFromLocationId(requestData.getFromLocationId());
            request.setToLocationType(requestData.getToLocationType());
            request.setToLocationId(requestData.getToLocationId());
            request.setProductId(requestData.getProductId());
            request.setRequestedQuantity(requestData.getRequestedQuantity());
            request.setItemName(requestData.getItemName());
            request.setItemSku(requestData.getItemSku());
            request.setReason(requestData.getReason());
            request.setNotes(requestData.getNotes());
            
            // Set priority
            if (requestData.getPriority() != null) {
                try {
                    request.setPriority(TransferRequestPriority.valueOf(requestData.getPriority().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    request.setPriority(TransferRequestPriority.MEDIUM);
                }
            } else {
                request.setPriority(TransferRequestPriority.MEDIUM);
            }
            
            TransferRequest created = transferRequestService.createEnhancedTransferRequest(request, company, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request created");
            response.put("transferId", created.getId());
            response.put("request", created);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to create transfer request: " + e.getMessage()));
        }
    }
    
    /**
     * POST /transfers - Create transfer request (legacy endpoint for backward compatibility)
     */
    @PostMapping
    public ResponseEntity<?> createTransferRequest(@Valid @RequestBody Map<String, Object> requestData,
                                                   Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            // Extract data from request
            UUID productId = UUID.fromString((String) requestData.get("productId"));
            UUID warehouseId = UUID.fromString((String) requestData.get("fromWarehouseId"));
            UUID storeId = UUID.fromString((String) requestData.get("toStoreId"));
            Integer requestedQuantity = (Integer) requestData.get("requestedQuantity");
            String priority = (String) requestData.getOrDefault("priority", "MEDIUM");
            String reason = (String) requestData.get("reason");
            
            // Get warehouse and store
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
            
            // Create transfer request
            TransferRequest request = new TransferRequest();
            request.setProductId(productId);
            request.setRequestedQuantity(requestedQuantity);
            request.setPriority(TransferRequestPriority.valueOf(priority));
            request.setReason(reason);
            
            TransferRequest created = transferRequestService.createTransferRequest(
                request, company, warehouse, store, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request created");
            response.put("transferId", created.getId());
            response.put("request", created);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to create transfer request: " + e.getMessage()));
        }
    }
    
    /**
     * GET /transfers - List transfer requests with pagination
     */
    @GetMapping
    public ResponseEntity<?> getTransferRequests(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) UUID storeId,
                                                @RequestParam(required = false) UUID warehouseId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            // Create Pageable object with sorting by createdAt DESC (most recent first)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            Page<TransferRequest> requestsPage;
            
            if (storeId != null) {
                requestsPage = transferRequestService.getTransferRequestsByStore(storeId, pageable);
            } else if (warehouseId != null) {
                requestsPage = transferRequestService.getTransferRequestsByWarehouse(warehouseId, pageable);
            } else if (status != null) {
                TransferRequestStatus requestStatus = TransferRequestStatus.valueOf(status.toUpperCase());
                requestsPage = transferRequestService.getTransferRequestsByStatus(company.getId(), requestStatus, pageable);
            } else {
                requestsPage = transferRequestService.getTransferRequestsByCompany(company.getId(), pageable);
            }
            
            // Add available actions to each transfer
            List<Map<String, Object>> requestsWithActions = requestsPage.getContent().stream()
                .map(transfer -> {
                    Map<String, Object> transferMap = new HashMap<>();
                    transferMap.put("transfer", transfer);
                    
                    // Calculate available actions for this user
                    List<String> actions = transferPermissionService.getAvailableActions(transfer, currentUser);
                    transferMap.put("availableActions", actions);
                    
                    return transferMap;
                })
                .collect(java.util.stream.Collectors.toList());
            
            // Build response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requests", requestsWithActions);
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("currentPage", requestsPage.getNumber());
            pagination.put("pageSize", requestsPage.getSize());
            pagination.put("totalElements", requestsPage.getTotalElements());
            pagination.put("totalPages", requestsPage.getTotalPages());
            pagination.put("hasNext", requestsPage.hasNext());
            pagination.put("hasPrevious", requestsPage.hasPrevious());
            
            response.put("pagination", pagination);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch transfer requests: " + e.getMessage()));
        }
    }
    
    /**
     * GET /transfers/{id} - Get transfer details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransferRequestById(@PathVariable UUID id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest request = transferRequestService.getTransferRequestById(id);
            
            // Calculate available actions for this user
            List<String> availableActions = transferPermissionService.getAvailableActions(request, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", request);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch transfer request: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/approve - Approve transfer (GM+ only)
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveTransferRequest(@PathVariable UUID id,
                                                    @Valid @RequestBody TransferApprovalRequest requestData,
                                                    Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission using TransferPermissionService
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "approve")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to approve this transfer"));
            }
            
            TransferRequest approved = transferRequestService.approveTransferRequest(
                id, requestData.getApprovedQuantity(), currentUser);
            
            // Add notes if provided
            if (requestData.getNotes() != null && !requestData.getNotes().isEmpty()) {
                transferRequestService.addNotes(id, requestData.getNotes());
            }
            
            // Calculate new available actions after approval
            List<String> availableActions = transferPermissionService.getAvailableActions(approved, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request approved");
            response.put("request", approved);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to approve transfer request: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/reject - Reject transfer (GM+ only)
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectTransferRequest(@PathVariable UUID id,
                                                   @Valid @RequestBody TransferRejectionRequest requestData,
                                                   Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission using TransferPermissionService
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "reject")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to reject this transfer"));
            }
            
            TransferRequest rejected = transferRequestService.rejectTransferRequest(
                id, currentUser, requestData.getReason());
            
            // Calculate new available actions after rejection
            List<String> availableActions = transferPermissionService.getAvailableActions(rejected, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request rejected");
            response.put("request", rejected);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to reject transfer request: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/complete - Mark as completed with inventory updates
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeTransferRequest(@PathVariable UUID id,
                                                     @Valid @RequestBody TransferCompletionRequest requestData,
                                                     Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest completed = transferRequestService.completeTransferWithInventory(
                id,
                requestData.getReceivedQuantity(),
                requestData.getDamagedQuantity(),
                requestData.getConditionOnArrival(),
                requestData.getReceiverName(),
                requestData.getReceiptNotes(),
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(completed, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request completed and inventory updated");
            response.put("request", completed);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to complete transfer request: " + e.getMessage()));
        }
    }
    
    /**
     * POST /transfers/{id}/send - Approve and send items with carrier details (GM+ only)
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<?> approveAndSend(@PathVariable UUID id,
                                           @Valid @RequestBody SendTransferRequestDTO sendRequest,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission - this combines approve + send
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "approve")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to approve and send this transfer"));
            }
            
            TransferRequest sent = transferRequestService.approveAndSend(
                id,
                sendRequest.getApprovedQuantity(),
                sendRequest.getCarrierName(),
                sendRequest.getCarrierPhone(),
                sendRequest.getCarrierVehicle(),
                sendRequest.getEstimatedDeliveryAt(),
                sendRequest.getNotes(),
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(sent, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer approved and sent");
            response.put("request", sent);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to approve and send transfer: " + e.getMessage()));
        }
    }
    
    /**
     * POST /transfers/{id}/receive - Confirm receipt of transfer
     */
    @PostMapping("/{id}/receive")
    public ResponseEntity<?> confirmReceipt(@PathVariable UUID id,
                                           @Valid @RequestBody ReceiveTransferDTO receiveRequest,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "receive")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to receive this transfer"));
            }
            
            TransferRequest received = transferRequestService.confirmReceipt(
                id,
                receiveRequest.getReceivedQuantity(),
                receiveRequest.getReceiverName(),
                receiveRequest.getReceiptNotes(),
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(received, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer receipt confirmed");
            response.put("request", received);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to confirm receipt: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/ready - Mark transfer as ready for pickup
     */
    @PutMapping("/{id}/ready")
    public ResponseEntity<?> markAsReady(
        @PathVariable UUID id,
        @Valid @RequestBody MarkReadyDTO readyData,
        Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "markReady")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to mark this transfer as ready"));
            }
            
            TransferRequest ready = transferRequestService.markAsReady(
                id,
                readyData.getPackedBy(),
                readyData.getNotes(),
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(ready, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer marked as ready for pickup");
            response.put("request", ready);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to mark transfer as ready: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/pickup - Pickup and start delivery
     */
    @PutMapping("/{id}/pickup")
    public ResponseEntity<?> pickupTransfer(
        @PathVariable UUID id,
        @Valid @RequestBody PickupTransferDTO pickupData,
        Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "startDelivery")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to start delivery for this transfer"));
            }
            
            // Generate QR code for delivery verification
            String qrCodeData = transferRequestService.generateDeliveryQRCode(id, currentUser);
            
            TransferRequest pickedUp = transferRequestService.pickupTransfer(
                id,
                pickupData.getCarrierName(),
                pickupData.getCarrierPhone(),
                pickupData.getCarrierVehicle(),
                pickupData.getEstimatedDeliveryAt(),
                qrCodeData,
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(pickedUp, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer picked up and in transit");
            response.put("request", pickedUp);
            response.put("deliveryQRCode", qrCodeData);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to pickup transfer: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/deliver - Mark as delivered
     */
    @PutMapping("/{id}/deliver")
    public ResponseEntity<?> markAsDelivered(
        @PathVariable UUID id,
        @Valid @RequestBody DeliverTransferDTO deliveryData,
        Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "markDelivered")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to mark this transfer as delivered"));
            }
            
            TransferRequest delivered = transferRequestService.markAsDelivered(
                id,
                deliveryData.getProofOfDeliveryUrl(),
                deliveryData.getConditionOnArrival(),
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(delivered, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer marked as delivered");
            response.put("request", delivered);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to mark as delivered: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/receive - Confirm receipt and complete transfer
     */
    @PutMapping("/{id}/receive")
    public ResponseEntity<?> receiveTransfer(
        @PathVariable UUID id,
        @Valid @RequestBody ReceiveTransferDTO receiptData,
        Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "receive")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to receive this transfer"));
            }
            
            // Verify QR code if provided
            if (receiptData.getDeliveryQRCode() != null && !receiptData.getDeliveryQRCode().isEmpty()) {
                boolean isValid = transferRequestService.verifyDeliveryQRCode(
                    id, 
                    receiptData.getDeliveryQRCode()
                );
                
                if (!isValid) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Invalid delivery QR code"));
                }
            }
            
            TransferRequest received = transferRequestService.receiveTransfer(
                id,
                receiptData.getReceivedQuantity(),
                receiptData.getDamagedQuantity(),
                receiptData.getReceiverName(),
                receiptData.getReceiverSignatureUrl(),
                receiptData.getReceiptNotes(),
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(received, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer completed and inventory updated");
            response.put("request", received);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to complete transfer: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/cancel - Cancel transfer request (enhanced with DTO)
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTransfer(@PathVariable UUID id,
                                           @Valid @RequestBody CancelTransferDTO cancellationData,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            TransferRequest transfer = transferRequestService.getTransferRequestById(id);
            
            // Validate permission
            if (!transferPermissionService.canPerformAction(transfer, currentUser, "cancel")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You do not have permission to cancel this transfer"));
            }
            
            TransferRequest cancelled = transferRequestService.cancelTransfer(
                id, 
                cancellationData.getReason(), 
                currentUser
            );
            
            // Calculate new available actions
            List<String> availableActions = transferPermissionService.getAvailableActions(cancelled, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request cancelled");
            response.put("request", cancelled);
            response.put("availableActions", availableActions);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to cancel transfer: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /transfers/{id}/ship - Mark transfer as shipped (IN_TRANSIT)
     */
    @PutMapping("/{id}/ship")
    public ResponseEntity<?> shipTransferRequest(@PathVariable UUID id,
                                                 @Valid @RequestBody TransferShipmentRequest requestData,
                                                 Authentication authentication) {
        try {
            TransferRequest shipped = transferRequestService.shipTransferRequest(
                id,
                requestData.getCarrierName(),
                requestData.getCarrierPhone(),
                requestData.getCarrierVehicle(),
                requestData.getEstimatedDeliveryAt()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer marked as shipped");
            response.put("request", shipped);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to ship transfer: " + e.getMessage()));
        }
    }
    
    /**
     * GET /transfers/pending-approval - Get pending transfers for current user's location
     */
    @GetMapping("/pending-approval")
    public ResponseEntity<?> getPendingApprovals(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            // Check if user is GM or Admin - they see all pending transfers
            boolean isGM = currentUser.getRole() != null && 
                          (currentUser.getRole().name().equals("ADMIN") || 
                           currentUser.getRole().name().equals("GM"));
            
            // Get all stores and warehouses for the user's company only
            // Note: In a production system, this should be filtered by user permissions
            // For now, we filter by company to prevent cross-company data exposure
            List<Store> companyStores = storeRepository.findAll().stream()
                .filter(store -> store.getCompany() != null && store.getCompany().getId().equals(company.getId()))
                .toList();
            
            List<Warehouse> companyWarehouses = warehouseRepository.findAll().stream()
                .filter(warehouse -> warehouse.getCompany() != null && warehouse.getCompany().getId().equals(company.getId()))
                .toList();
            
            List<TransferRequest> pendingTransfers = transferRequestService.getPendingApprovalsForUser(
                company.getId(),
                companyStores,
                companyWarehouses,
                isGM
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requests", pendingTransfers);
            response.put("count", pendingTransfers.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch pending approvals: " + e.getMessage()));
        }
    }
    
    /**
     * GET /transfers/history - Get transfer history with filtering
     */
    @GetMapping("/history")
    public ResponseEntity<?> getTransferHistory(@RequestParam(required = false) UUID locationId,
                                               @RequestParam(required = false) String locationType,
                                               @RequestParam(required = false) String status,
                                               Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            List<TransferRequest> transfers;
            
            if (locationId != null && locationType != null) {
                transfers = transferRequestService.getTransfersByLocation(locationType, locationId);
            } else if (status != null) {
                TransferRequestStatus requestStatus = TransferRequestStatus.valueOf(status.toUpperCase());
                transfers = transferRequestService.getTransferRequestsByStatus(company.getId(), requestStatus);
            } else {
                transfers = transferRequestService.getTransferRequestsByCompany(company.getId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transfers", transfers);
            response.put("count", transfers.size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Invalid status value"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch transfer history: " + e.getMessage()));
        }
    }
}
