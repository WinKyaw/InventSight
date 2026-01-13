package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.service.TransferRequestService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/transfers")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransferRequestController {
    
    @Autowired
    private TransferRequestService transferRequestService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    /**
     * POST /api/transfers - Create transfer request
     */
    @PostMapping
    public ResponseEntity<?> createTransferRequest(@Valid @RequestBody Map<String, Object> requestData,
                                                   Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = companyService.getUserCompany(currentUser.getId());
            
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
     * GET /api/transfers - List transfer requests
     */
    @GetMapping
    public ResponseEntity<?> getTransferRequests(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) UUID storeId,
                                                @RequestParam(required = false) UUID warehouseId,
                                                Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = companyService.getUserCompany(currentUser.getId());
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            List<TransferRequest> requests;
            
            if (storeId != null) {
                requests = transferRequestService.getTransferRequestsByStore(storeId);
            } else if (warehouseId != null) {
                requests = transferRequestService.getTransferRequestsByWarehouse(warehouseId);
            } else if (status != null) {
                TransferRequestStatus requestStatus = TransferRequestStatus.valueOf(status.toUpperCase());
                requests = transferRequestService.getTransferRequestsByStatus(company.getId(), requestStatus);
            } else {
                requests = transferRequestService.getTransferRequestsByCompany(company.getId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requests", requests);
            response.put("count", requests.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch transfer requests: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/transfers/{id} - Get transfer details
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
     * PUT /api/transfers/{id}/approve - Approve transfer (GM+ only)
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveTransferRequest(@PathVariable UUID id,
                                                    @RequestBody Map<String, Integer> requestData,
                                                    Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            // TODO: Add role check for GM+ permission
            
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
     * PUT /api/transfers/{id}/reject - Reject transfer (GM+ only)
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectTransferRequest(@PathVariable UUID id,
                                                   @RequestBody Map<String, String> requestData,
                                                   Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            // TODO: Add role check for GM+ permission
            
            String reason = requestData.get("reason");
            TransferRequest rejected = transferRequestService.rejectTransferRequest(id, currentUser, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer request rejected");
            response.put("request", rejected);
            
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
     * PUT /api/transfers/{id}/complete - Mark as completed
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
}
