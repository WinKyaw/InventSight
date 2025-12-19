package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.SaleRequest;
import com.pos.inventsight.dto.SaleResponse;
import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.SaleItem;
import com.pos.inventsight.model.sql.User;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/receipts")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("isAuthenticated()")
public class ReceiptController {
    
    @Autowired
    private SaleService saleService;
    
    @Autowired
    private UserService userService;
    
    // GET /receipts - Get all receipts for authenticated user
    @GetMapping
    public ResponseEntity<?> getAllReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🧾 InventSight - Fetching receipts for user: " + username);
            
            User user = userService.getUserByUsername(username);
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Sale> receipts = saleService.getSalesByUserId(user.getId(), pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("receipts", receipts.getContent());
            response.put("currentPage", receipts.getNumber());
            response.put("totalItems", receipts.getTotalElements());
            response.put("totalPages", receipts.getTotalPages());
            response.put("pageSize", receipts.getSize());
            
            System.out.println("✅ Retrieved " + receipts.getTotalElements() + " receipts for user: " + username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching receipts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching receipts: " + e.getMessage()));
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
            
            System.out.println("✅ Retrieved receipt: " + receipt.getReceiptNumber());
            return ResponseEntity.ok(receipt);
            
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
            
            System.out.println("✅ Updated receipt: " + updatedReceipt.getReceiptNumber());
            return ResponseEntity.ok(updatedReceipt);
            
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
            
            System.out.println("✅ Added items to receipt: " + updatedReceipt.getReceiptNumber());
            return ResponseEntity.ok(updatedReceipt);
            
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
            
            // Apply pagination manually if needed
            int start = page * size;
            int end = Math.min((start + size), receipts.size());
            List<Sale> paginatedReceipts = receipts.subList(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("receipts", paginatedReceipts);
            response.put("currentPage", page);
            response.put("totalItems", receipts.size());
            response.put("totalPages", (int) Math.ceil((double) receipts.size() / size));
            response.put("pageSize", size);
            
            System.out.println("✅ Found " + receipts.size() + " receipts matching search criteria");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error searching receipts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error searching receipts: " + e.getMessage()));
        }
    }
}