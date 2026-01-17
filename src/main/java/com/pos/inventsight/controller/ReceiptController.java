package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.CashierStatsDTO;
import com.pos.inventsight.dto.SaleRequest;
import com.pos.inventsight.dto.SaleResponse;
import com.pos.inventsight.model.sql.PaymentMethod;
import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.SaleItem;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.SaleRepository;
import com.pos.inventsight.service.SaleService;
import com.pos.inventsight.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/receipts")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("isAuthenticated()")
public class ReceiptController {
    
    @Autowired
    private SaleService saleService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SaleRepository saleRepository;
    
    // GET /receipts - Get all receipts with optional cashier filter
    @GetMapping
    public ResponseEntity<?> getAllReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) UUID cashierId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📄 InventSight - Getting receipts for user: " + username);
            System.out.println("📄 Params - Page: " + page + ", Size: " + size + ", CashierId: " + (cashierId != null ? cashierId : "All"));
            
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Sale> receipts;
            
            // Filter by cashier if provided
            if (cashierId != null) {
                System.out.println("🔍 Filtering receipts by cashier/employee: " + cashierId);
                receipts = saleService.getSalesByCashier(cashierId, pageable);
            } else {
                System.out.println("📋 Getting all receipts (no cashier filter)");
                receipts = saleService.getAllSales(pageable);
            }
            
            // Convert entities to DTOs
            List<SaleResponse> receiptDTOs = new java.util.ArrayList<>();
            for (Sale sale : receipts.getContent()) {
                receiptDTOs.add(saleService.toSaleResponse(sale));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("receipts", receiptDTOs);
            response.put("currentPage", receipts.getNumber());
            response.put("totalItems", receipts.getTotalElements());
            response.put("totalPages", receipts.getTotalPages());
            response.put("pageSize", receipts.getSize());
            
            System.out.println("✅ Returning " + receipts.getTotalElements() + " total receipts, " 
                + receipts.getNumberOfElements() + " in this page");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting receipts: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error getting receipts: " + e.getMessage()));
        }
    }
    
    // GET /receipts/{id} - Get specific receipt by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getReceiptById(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("🧾 InventSight - Fetching receipt ID: " + id + " for user: " + username);
            
            Sale receipt = saleService.getSaleById(id);
            
            // Check if user owns this receipt (unless admin)
            if (!user.getRole().name().equals("ADMIN") && !receipt.getProcessedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied: You can only view your own receipts"));
            }
            
            // Convert to DTO
            SaleResponse receiptDTO = saleService.toSaleResponse(receipt);
            
            System.out.println("✅ Retrieved receipt: " + receipt.getReceiptNumber());
            return ResponseEntity.ok(receiptDTO);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Receipt not found with ID: " + id));
        }
    }
    
    // POST /receipts - Create new receipt
    @PostMapping
    public ResponseEntity<?> createReceipt(@Valid @RequestBody SaleRequest request, 
                                         Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("🧾 InventSight - Creating new receipt for user: " + username);
            System.out.println("📊 Receipt items count: " + request.getItems().size());
            
            // ✅ FIX: Validate payment method based on status
            if (request.requiresPaymentMethod()) {
                if (request.getPaymentMethod() == null) {
                    return ResponseEntity.badRequest().body(new ApiResponse(
                        false, 
                        "Payment method is required for completed receipts"
                    ));
                }
                System.out.println("✅ Creating COMPLETED receipt with payment: " + request.getPaymentMethod());
            } else {
                System.out.println("📝 Creating PENDING receipt (no payment required)");
            }
            
            SaleResponse receipt = saleService.createSale(request, user.getId());
            
            System.out.println("✅ Created receipt: " + receipt.getReceiptNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
            
        } catch (Exception e) {
            System.err.println("❌ Error creating receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error creating receipt: " + e.getMessage()));
        }
    }
    
    // PUT /receipts/{id} - Update existing receipt
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReceipt(@PathVariable Long id, 
                                         @Valid @RequestBody SaleRequest request,
                                         Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("🧾 InventSight - Updating receipt ID: " + id + " for user: " + username);
            
            Sale existingReceipt = saleService.getSaleById(id);
            
            // Check if user owns this receipt (unless admin)
            if (!user.getRole().name().equals("ADMIN") && 
                !existingReceipt.getProcessedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied: You can only update your own receipts"));
            }
            
            Sale updatedReceipt = saleService.updateSale(id, request);
            
            // Convert to DTO
            SaleResponse receiptDTO = saleService.toSaleResponse(updatedReceipt);
            
            System.out.println("✅ Updated receipt: " + updatedReceipt.getReceiptNumber());
            return ResponseEntity.ok(receiptDTO);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error updating receipt: " + e.getMessage()));
        }
    }
    
    // DELETE /receipts/{id} - Delete receipt
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @saleService.getSaleById(#id).processedBy.username == authentication.name")
    public ResponseEntity<?> deleteReceipt(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🧾 InventSight - Deleting receipt ID: " + id + " by user: " + username);
            
            Sale receipt = saleService.getSaleById(id);
            saleService.deleteSale(id);
            
            System.out.println("✅ Deleted receipt: " + receipt.getReceiptNumber());
            return ResponseEntity.ok(new ApiResponse(true, "Receipt deleted successfully"));
            
        } catch (Exception e) {
            System.err.println("❌ Error deleting receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Receipt not found with ID: " + id));
        }
    }
    
    // POST /receipts/{id}/items - Add items to receipt
    @PostMapping("/{id}/items")
    public ResponseEntity<?> addItemsToReceipt(@PathVariable Long id,
                                             @Valid @RequestBody List<SaleRequest.ItemRequest> items,
                                             Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("🧾 InventSight - Adding " + items.size() + " items to receipt ID: " + id);
            
            Sale receipt = saleService.getSaleById(id);
            
            // Check if user owns this receipt (unless admin)
            if (!user.getRole().name().equals("ADMIN") && 
                !receipt.getProcessedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied: You can only modify your own receipts"));
            }
            
            Sale updatedReceipt = saleService.addItemsToSale(id, items);
            
            // Convert to DTO
            SaleResponse receiptDTO = saleService.toSaleResponse(updatedReceipt);
            
            System.out.println("✅ Added items to receipt: " + updatedReceipt.getReceiptNumber());
            return ResponseEntity.ok(receiptDTO);
            
        } catch (Exception e) {
            System.err.println("❌ Error adding items to receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error adding items to receipt: " + e.getMessage()));
        }
    }
    
    // GET /receipts/search - Search receipts by date range, vendor, etc.
    @GetMapping("/search")
    public ResponseEntity<?> searchReceipts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String receiptNumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("🔍 InventSight - Searching receipts for user: " + username);
            System.out.println("🗓️ Date range: " + startDate + " to " + endDate);
            System.out.println("👤 Customer: " + customerName + " | Email: " + customerEmail);
            
            List<Sale> receipts = saleService.searchReceipts(user.getId(), startDate, endDate, 
                customerName, customerEmail, receiptNumber, status);
            
            // Convert entities to DTOs
            List<SaleResponse> receiptDTOs = new java.util.ArrayList<>();
            for (Sale sale : receipts) {
                receiptDTOs.add(saleService.toSaleResponse(sale));
            }
            
            // Apply pagination manually if needed
            int start = page * size;
            int end = Math.min((start + size), receiptDTOs.size());
            List<SaleResponse> paginatedReceipts = receiptDTOs.subList(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("receipts", paginatedReceipts);
            response.put("currentPage", page);
            response.put("totalItems", receiptDTOs.size());
            response.put("totalPages", (int) Math.ceil((double) receiptDTOs.size() / size));
            response.put("pageSize", size);
            
            System.out.println("✅ Found " + receipts.size() + " receipts matching search criteria");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error searching receipts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error searching receipts: " + e.getMessage()));
        }
    }
    
    /**
     * GET /receipts/employee/{employeeId} - Get receipts by employee for a specific date
     * GM+ only
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getReceiptsByEmployee(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) String date,  // Format: YYYY-MM-DD
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // Check if user is GM+
            if (!isGMPlus(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied. GM+ role required."));
            }
            
            System.out.println("📊 Getting receipts for employee: " + employeeId);
            System.out.println("📅 Date filter: " + (date != null ? date : "all time"));
            
            List<Sale> receipts;
            
            if (date != null) {
                // Parse date and get receipts for that day
                LocalDate queryDate = LocalDate.parse(date);
                LocalDateTime startOfDay = queryDate.atStartOfDay();
                LocalDateTime endOfDay = queryDate.atTime(23, 59, 59);
                
                receipts = saleRepository.findByProcessedByIdAndCreatedAtBetween(
                    employeeId, startOfDay, endOfDay
                );
                
                System.out.println("✅ Found " + receipts.size() + " receipts for " + date);
            } else {
                // Get all receipts for employee
                receipts = saleRepository.findByProcessedById(employeeId);
                
                System.out.println("✅ Found " + receipts.size() + " total receipts");
            }
            
            // Convert to DTOs
            List<SaleResponse> response = receipts.stream()
                .map(sale -> saleService.toSaleResponse(sale))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))  // Newest first
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error fetching employee receipts: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching receipts: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/receipts/cashiers - Get cashier statistics
     * Returns list of cashiers with their receipt counts
     * Only accessible by GM+ users (OWNER, FOUNDER, CEO, GENERAL_MANAGER, ADMIN)
     */
    @GetMapping("/cashiers")
    @PreAuthorize("hasAnyAuthority('OWNER', 'FOUNDER', 'CEO', 'GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getCashierStats(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Getting cashier stats for GM+ user: " + username);
            
            List<CashierStatsDTO> stats;
            
            if (storeId != null) {
                System.out.println("📊 Filtering by store: " + storeId);
                stats = saleService.getCashierStatsByStore(storeId);
            } else if (startDate != null && endDate != null) {
                System.out.println("📊 Filtering by date range: " + startDate + " to " + endDate);
                stats = saleService.getCashierStatsByDateRange(startDate, endDate);
            } else {
                System.out.println("📊 Getting all cashier stats");
                stats = saleService.getCashierStats();
            }
            
            System.out.println("✅ Returning " + stats.size() + " cashier stats");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting cashier stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error getting cashier stats: " + e.getMessage()));
        }
    }
    
    /**
     * Check if user has GM+ privileges
     */
    private boolean isGMPlus(User user) {
        UserRole role = user.getRole();
        return role == UserRole.MANAGER ||
               role == UserRole.OWNER ||
               role == UserRole.FOUNDER ||
               role == UserRole.CO_OWNER ||
               role == UserRole.ADMIN;
    }
    
    /**
     * PUT /receipts/{id}/complete - Complete a pending receipt with payment method
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeReceipt(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // Get payment method from request
            String paymentMethodStr = (String) request.get("paymentMethod");
            if (paymentMethodStr == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(
                    false,
                    "Payment method is required to complete receipt"
                ));
            }
            
            PaymentMethod paymentMethod;
            try {
                paymentMethod = PaymentMethod.valueOf(paymentMethodStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(
                    false,
                    "Invalid payment method: " + paymentMethodStr
                ));
            }
            
            System.out.println("💳 Completing receipt: " + id + " with payment: " + paymentMethod);
            
            // Use the existing service method to complete receipt
            SaleResponse completedReceipt = saleService.completeReceipt(id, paymentMethod, user.getId());
            
            System.out.println("✅ Receipt completed: " + id);
            return ResponseEntity.ok(completedReceipt);
            
        } catch (Exception e) {
            System.err.println("❌ Error completing receipt: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error completing receipt: " + e.getMessage()));
        }
    }
}