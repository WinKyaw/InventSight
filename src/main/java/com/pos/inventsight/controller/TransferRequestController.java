package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.exception.UnauthorizedException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.service.TransferRequestService;
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
            
            // Build response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requests", requestsPage.getContent());
            
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
    public ResponseEntity<?> getTransferRequestById(@PathVariable UUID id) {
        try {
            TransferRequest request = transferRequestService.getTransferRequestById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", request);
            
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
                                                    @RequestBody Map<String, Integer> requestData,
                                                    Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            // Check if user has GM+ permission
            Optional<CompanyRole> roleOpt = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
            if (roleOpt.isEmpty() || !roleOpt.get().canManageWarehouses()) {
                throw new UnauthorizedException("Only General Manager and above can approve transfer requests");
            }
            
            Integer approvedQuantity = requestData.get("approvedQuantity");
            if (approvedQuantity == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Approved quantity is required"));
            }
            
            TransferRequest approved = transferRequestService.approveTransferRequest(id, approvedQuantity, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request approved");
            response.put("request", approved);
            
            return ResponseEntity.ok(response);
            
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, e.getMessage()));
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
                                                   @RequestBody Map<String, String> requestData,
                                                   Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            // Check if user has GM+ permission
            Optional<CompanyRole> roleOpt = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
            if (roleOpt.isEmpty() || !roleOpt.get().canManageWarehouses()) {
                throw new UnauthorizedException("Only General Manager and above can reject transfer requests");
            }
            
            String reason = requestData.get("reason");
            TransferRequest rejected = transferRequestService.rejectTransferRequest(id, currentUser, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request rejected");
            response.put("request", rejected);
            
            return ResponseEntity.ok(response);
            
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, e.getMessage()));
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
     * PUT /transfers/{id}/complete - Mark as completed
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeTransferRequest(@PathVariable UUID id) {
        try {
            TransferRequest completed = transferRequestService.completeTransferRequest(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request completed");
            response.put("request", completed);
            
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
            Company company = getUserCompany(currentUser);
            
            // Check if user has GM+ permission
            Optional<CompanyRole> roleOpt = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
            if (roleOpt.isEmpty() || !roleOpt.get().canManageWarehouses()) {
                throw new UnauthorizedException("Only General Manager and above can approve and send transfer requests");
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer approved and sent");
            response.put("request", sent);
            
            return ResponseEntity.ok(response);
            
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, e.getMessage()));
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
            
            TransferRequest received = transferRequestService.confirmReceipt(
                id,
                receiveRequest.getReceivedQuantity(),
                receiveRequest.getReceiverName(),
                receiveRequest.getReceiptNotes(),
                currentUser
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer receipt confirmed");
            response.put("request", received);
            
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
     * PUT /transfers/{id}/cancel - Cancel transfer request
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTransfer(@PathVariable UUID id,
                                           @RequestBody Map<String, String> requestData,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            String reason = requestData.get("reason");
            TransferRequest cancelled = transferRequestService.cancelTransfer(id, reason, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request cancelled");
            response.put("request", cancelled);
            
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
