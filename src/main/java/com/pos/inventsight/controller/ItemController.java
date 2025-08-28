package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.service.ProductService;
import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.ProductRequest;
import com.pos.inventsight.dto.ProductResponse;
import com.pos.inventsight.dto.ProductSearchRequest;
import com.pos.inventsight.dto.StockUpdateRequest;
import com.pos.inventsight.dto.BulkProductRequest;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateSkuException;
import com.pos.inventsight.exception.InsufficientStockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/items")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ItemController {

    @Autowired
    private ProductService productService;

    // GET /items - Get all items with pagination, sorting, filtering
    @GetMapping
    public ResponseEntity<?> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) Boolean active) {
        
        try {
            System.out.println("📦 InventSight - Fetching all items");
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Product> products;
            
            if (category != null) {
                products = productService.searchProducts(category, pageable);
            } else {
                // Since getAllActiveProducts returns List, I'll create a page from it
                List<Product> allProducts = productService.getAllActiveProducts();
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), allProducts.size());
                List<Product> pageContent = allProducts.subList(start, end);
                products = new PageImpl<>(pageContent, pageable, allProducts.size());
            }

            Page<ProductResponse> response = products.map(ProductResponse::new);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error fetching items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching items: " + e.getMessage()));
        }
    }

    // GET /items/{id} - Get item by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getItemById(@PathVariable Long id) {
        try {
            System.out.println("🔍 InventSight - Fetching item with ID: " + id);
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            Product product = productService.getProductById(id);
            ProductResponse response = new ProductResponse(product);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Item not found with ID: " + id));
        } catch (Exception e) {
            System.err.println("❌ Error fetching item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching item: " + e.getMessage()));
        }
    }

    // POST /items - Create new item
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody ProductRequest request, 
                                       Authentication authentication) {
        try {
            String currentUser = authentication != null ? authentication.getName() : "WinKyaw";
            System.out.println("➕ InventSight - Creating new item: " + request.getName());
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: " + currentUser);

            Product product = convertToProduct(request);
            Product savedProduct = productService.createProduct(product, currentUser);
            ProductResponse response = new ProductResponse(savedProduct);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DuplicateSkuException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(false, "SKU already exists: " + request.getSku()));
        } catch (Exception e) {
            System.err.println("❌ Error creating item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error creating item: " + e.getMessage()));
        }
    }

    // PUT /items/{id} - Update existing item
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, 
                                       @Valid @RequestBody ProductRequest request, 
                                       Authentication authentication) {
        try {
            String currentUser = authentication != null ? authentication.getName() : "WinKyaw";
            System.out.println("✏️ InventSight - Updating item with ID: " + id);
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: " + currentUser);

            Product productUpdates = convertToProduct(request);
            Product updatedProduct = productService.updateProduct(id, productUpdates, currentUser);
            ProductResponse response = new ProductResponse(updatedProduct);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Item not found with ID: " + id));
        } catch (DuplicateSkuException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(false, "SKU already exists: " + request.getSku()));
        } catch (Exception e) {
            System.err.println("❌ Error updating item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error updating item: " + e.getMessage()));
        }
    }

    // DELETE /items/{id} - Soft delete item
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id, Authentication authentication) {
        try {
            String currentUser = authentication != null ? authentication.getName() : "WinKyaw";
            System.out.println("🗑️ InventSight - Soft deleting item with ID: " + id);
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: " + currentUser);

            // Soft delete by setting isActive to false
            Product product = productService.getProductById(id);
            product.setIsActive(false);
            product.setUpdatedAt(java.time.LocalDateTime.now());
            product.setUpdatedBy(currentUser);
            productService.updateProduct(id, product, currentUser);
            
            return ResponseEntity.ok(new ApiResponse(true, "Item deleted successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Item not found with ID: " + id));
        } catch (Exception e) {
            System.err.println("❌ Error deleting item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error deleting item: " + e.getMessage()));
        }
    }

    // GET /items/search?query={query} - Search items by name/description
    @GetMapping("/search")
    public ResponseEntity<?> searchItems(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            System.out.println("🔍 InventSight - Searching items with query: " + query);
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Product> products = productService.searchProducts(query, pageable);
            Page<ProductResponse> response = products.map(ProductResponse::new);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error searching items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error searching items: " + e.getMessage()));
        }
    }

    // GET /items/category/{category} - Get items by category
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getItemsByCategory(@PathVariable String category,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        try {
            System.out.println("📂 InventSight - Fetching items by category: " + category);
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            List<Product> products = productService.getProductsByCategory(category);
            List<ProductResponse> response = products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error fetching items by category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching items by category: " + e.getMessage()));
        }
    }

    // GET /items/sku/{sku} - Get item by SKU
    @GetMapping("/sku/{sku}")
    public ResponseEntity<?> getItemBySku(@PathVariable String sku) {
        try {
            System.out.println("🏷️ InventSight - Fetching item by SKU: " + sku);
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            Product product = productService.getProductBySku(sku);
            ProductResponse response = new ProductResponse(product);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Item not found with SKU: " + sku));
        } catch (Exception e) {
            System.err.println("❌ Error fetching item by SKU: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching item by SKU: " + e.getMessage()));
        }
    }

    // GET /items/low-stock - Get items with low stock
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockItems() {
        try {
            System.out.println("⚠️ InventSight - Fetching low stock items");
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            List<Product> products = productService.getLowStockProducts();
            List<ProductResponse> response = products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error fetching low stock items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching low stock items: " + e.getMessage()));
        }
    }

    // POST /items/{id}/stock/add - Add stock quantity
    @PostMapping("/{id}/stock/add")
    public ResponseEntity<?> addStock(@PathVariable Long id, 
                                     @Valid @RequestBody StockUpdateRequest request,
                                     Authentication authentication) {
        try {
            String currentUser = authentication != null ? authentication.getName() : "WinKyaw";
            System.out.println("➕ InventSight - Adding stock to item ID: " + id + ", Quantity: " + request.getQuantity());
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: " + currentUser);

            productService.increaseStock(id, request.getQuantity(), 
                request.getReason() != null ? request.getReason() : "Stock added via API");
            
            Product updatedProduct = productService.getProductById(id);
            ProductResponse response = new ProductResponse(updatedProduct);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Item not found with ID: " + id));
        } catch (Exception e) {
            System.err.println("❌ Error adding stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error adding stock: " + e.getMessage()));
        }
    }

    // POST /items/{id}/stock/reduce - Reduce stock quantity
    @PostMapping("/{id}/stock/reduce")
    public ResponseEntity<?> reduceStock(@PathVariable Long id, 
                                        @Valid @RequestBody StockUpdateRequest request,
                                        Authentication authentication) {
        try {
            String currentUser = authentication != null ? authentication.getName() : "WinKyaw";
            System.out.println("➖ InventSight - Reducing stock for item ID: " + id + ", Quantity: " + request.getQuantity());
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: " + currentUser);

            productService.reduceStock(id, request.getQuantity(), 
                request.getReason() != null ? request.getReason() : "Stock reduced via API");
            
            Product updatedProduct = productService.getProductById(id);
            ProductResponse response = new ProductResponse(updatedProduct);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Item not found with ID: " + id));
        } catch (InsufficientStockException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Insufficient stock: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error reducing stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error reducing stock: " + e.getMessage()));
        }
    }

    // PUT /items/{id}/stock - Update stock quantity directly
    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable Long id, 
                                        @Valid @RequestBody StockUpdateRequest request,
                                        Authentication authentication) {
        try {
            String currentUser = authentication != null ? authentication.getName() : "WinKyaw";
            System.out.println("🔄 InventSight - Updating stock for item ID: " + id + " to: " + request.getQuantity());
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: " + currentUser);

            productService.updateStock(id, request.getQuantity(), currentUser,
                request.getReason() != null ? request.getReason() : "Stock updated via API");
            
            Product updatedProduct = productService.getProductById(id);
            ProductResponse response = new ProductResponse(updatedProduct);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Item not found with ID: " + id));
        } catch (Exception e) {
            System.err.println("❌ Error updating stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error updating stock: " + e.getMessage()));
        }
    }

    // GET /items/analytics/valuation - Get total inventory valuation
    @GetMapping("/analytics/valuation")
    public ResponseEntity<?> getInventoryValuation() {
        try {
            System.out.println("💰 InventSight - Calculating inventory valuation");
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            BigDecimal totalValue = productService.getTotalInventoryValue();
            long totalCount = productService.getTotalProductCount();
            
            Map<String, Object> valuation = new HashMap<>();
            valuation.put("totalValue", totalValue);
            valuation.put("totalProducts", totalCount);
            valuation.put("averageValue", totalCount > 0 ? 
                totalValue.divide(new BigDecimal(totalCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            valuation.put("calculatedAt", LocalDateTime.now());
            valuation.put("calculatedBy", "WinKyaw");
            
            return ResponseEntity.ok(valuation);
        } catch (Exception e) {
            System.err.println("❌ Error calculating inventory valuation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error calculating inventory valuation: " + e.getMessage()));
        }
    }

    // GET /items/analytics/turnover - Get inventory turnover data
    @GetMapping("/analytics/turnover")
    public ResponseEntity<?> getInventoryTurnover() {
        try {
            System.out.println("📊 InventSight - Calculating inventory turnover");
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            Map<String, Long> categoryDistribution = productService.getCategoryDistribution();
            Map<String, Long> supplierDistribution = productService.getSupplierDistribution();
            List<Product> lowStockProducts = productService.getLowStockProducts();
            List<Product> outOfStockProducts = productService.getOutOfStockProducts();
            
            Map<String, Object> turnover = new HashMap<>();
            turnover.put("categoryDistribution", categoryDistribution);
            turnover.put("supplierDistribution", supplierDistribution);
            turnover.put("lowStockCount", lowStockProducts.size());
            turnover.put("outOfStockCount", outOfStockProducts.size());
            turnover.put("calculatedAt", LocalDateTime.now());
            turnover.put("calculatedBy", "WinKyaw");
            
            return ResponseEntity.ok(turnover);
        } catch (Exception e) {
            System.err.println("❌ Error calculating inventory turnover: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error calculating inventory turnover: " + e.getMessage()));
        }
    }

    // POST /items/import - Import items from CSV/Excel (basic implementation)
    @PostMapping("/import")
    public ResponseEntity<?> importItems(@Valid @RequestBody BulkProductRequest request,
                                        Authentication authentication) {
        try {
            String currentUser = authentication != null ? authentication.getName() : "WinKyaw";
            System.out.println("📥 InventSight - Importing " + request.getProducts().size() + " items");
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: " + currentUser);

            List<ProductResponse> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (ProductRequest productRequest : request.getProducts()) {
                try {
                    Product product = convertToProduct(productRequest);
                    Product savedProduct = productService.createProduct(product, currentUser);
                    results.add(new ProductResponse(savedProduct));
                } catch (DuplicateSkuException e) {
                    if (request.isSkipDuplicates()) {
                        errors.add("Skipped duplicate SKU: " + productRequest.getSku());
                    } else {
                        errors.add("Duplicate SKU: " + productRequest.getSku());
                    }
                } catch (Exception e) {
                    errors.add("Error importing item " + productRequest.getName() + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> importResult = new HashMap<>();
            importResult.put("successCount", results.size());
            importResult.put("errorCount", errors.size());
            importResult.put("items", results);
            importResult.put("errors", errors);
            importResult.put("importedAt", LocalDateTime.now());
            importResult.put("importedBy", currentUser);
            
            return ResponseEntity.ok(importResult);
        } catch (Exception e) {
            System.err.println("❌ Error importing items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error importing items: " + e.getMessage()));
        }
    }

    // GET /items/export - Export items to CSV (basic implementation)
    @GetMapping("/export")
    public ResponseEntity<?> exportItems(@RequestParam(required = false) String category,
                                        @RequestParam(required = false) String supplier,
                                        @RequestParam(defaultValue = "false") boolean activeOnly) {
        try {
            System.out.println("📤 InventSight - Exporting items");
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            List<Product> products;
            if (activeOnly) {
                products = productService.getAllActiveProducts();
            } else {
                // For now, use active products - would need to modify service for all products
                products = productService.getAllActiveProducts();
            }
            
            // Filter by category and supplier if specified
            if (category != null) {
                products = products.stream()
                    .filter(p -> category.equals(p.getCategory()))
                    .collect(Collectors.toList());
            }
            if (supplier != null) {
                products = products.stream()
                    .filter(p -> supplier.equals(p.getSupplier()))
                    .collect(Collectors.toList());
            }
            
            List<ProductResponse> response = products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
            
            Map<String, Object> exportResult = new HashMap<>();
            exportResult.put("items", response);
            exportResult.put("totalCount", response.size());
            exportResult.put("exportedAt", LocalDateTime.now());
            exportResult.put("exportedBy", "WinKyaw");
            exportResult.put("filters", Map.of(
                "category", category != null ? category : "all",
                "supplier", supplier != null ? supplier : "all",
                "activeOnly", activeOnly
            ));
            
            return ResponseEntity.ok(exportResult);
        } catch (Exception e) {
            System.err.println("❌ Error exporting items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error exporting items: " + e.getMessage()));
        }
    }

    // GET /items/statistics - Get item statistics
    @GetMapping("/statistics")
    public ResponseEntity<ItemStatistics> getItemStatistics() {
        try {
            System.out.println("📊 InventSight - Calculating item statistics");
            System.out.println("📅 Current Date and Time (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User's Login: WinKyaw");

            long totalItems = productService.getTotalProductCount();
            long activeItems = totalItems; // All products are active in our current implementation
            List<Product> lowStockProducts = productService.getLowStockProducts();
            List<Product> outOfStockProducts = productService.getOutOfStockProducts();
            BigDecimal totalValue = productService.getTotalInventoryValue();
            List<String> categories = productService.getAllCategories();
            List<String> suppliers = productService.getAllSuppliers();
            
            ItemStatistics statistics = new ItemStatistics(
                totalItems,
                activeItems,
                lowStockProducts.size(),
                outOfStockProducts.size(),
                totalValue,
                categories,
                suppliers,
                LocalDateTime.now().toString(),
                "WinKyaw"
            );
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            System.err.println("❌ Error calculating item statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper method to convert ProductRequest to Product
    private Product convertToProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice());
        product.setQuantity(request.getQuantity());
        product.setMaxQuantity(request.getMaxQuantity());
        product.setUnit(request.getUnit());
        product.setSupplier(request.getSupplier());
        product.setLocation(request.getLocation());
        product.setBarcode(request.getBarcode());
        product.setExpiryDate(request.getExpiryDate());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setReorderLevel(request.getReorderLevel());
        return product;
    }

    // Item Statistics DTO
    public static class ItemStatistics {
        private long totalItems;
        private long activeItems;
        private long lowStockItems;
        private long outOfStockItems;
        private BigDecimal totalValue;
        private List<String> categories;
        private List<String> suppliers;
        private String currentDateTime;
        private String currentUser;
        
        public ItemStatistics(long totalItems, long activeItems, long lowStockItems, 
                             long outOfStockItems, BigDecimal totalValue, List<String> categories,
                             List<String> suppliers, String currentDateTime, String currentUser) {
            this.totalItems = totalItems;
            this.activeItems = activeItems;
            this.lowStockItems = lowStockItems;
            this.outOfStockItems = outOfStockItems;
            this.totalValue = totalValue;
            this.categories = categories;
            this.suppliers = suppliers;
            this.currentDateTime = currentDateTime;
            this.currentUser = currentUser;
        }
        
        // Getters
        public long getTotalItems() { return totalItems; }
        public long getActiveItems() { return activeItems; }
        public long getLowStockItems() { return lowStockItems; }
        public long getOutOfStockItems() { return outOfStockItems; }
        public BigDecimal getTotalValue() { return totalValue; }
        public List<String> getCategories() { return categories; }
        public List<String> getSuppliers() { return suppliers; }
        public String getCurrentDateTime() { return currentDateTime; }
        public String getCurrentUser() { return currentUser; }
    }
}