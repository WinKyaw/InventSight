package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.ProductRequest;
import com.pos.inventsight.dto.ProductResponse;
import com.pos.inventsight.dto.StockUpdateRequest;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.PermissionType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.service.ProductService;
import com.pos.inventsight.service.OneTimePermissionService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
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
import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private OneTimePermissionService permissionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private UserStoreRoleRepository userStoreRoleRepository;
    
    // GET /products - Get all products with pagination
    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📦 InventSight - Fetching products for user: " + username);
            
            // Get current user
            User currentUser = userService.getUserByUsername(username);
            
            // Try CompanyStoreUser first (new multi-tenant system)
            List<CompanyStoreUser> userMemberships = companyStoreUserRepository.findByUserAndIsActiveTrue(currentUser);
            final Set<UUID> userCompanyIds;
            
            if (!userMemberships.isEmpty()) {
                // Get company IDs from CompanyStoreUser
                userCompanyIds = userMemberships.stream()
                    .map(membership -> membership.getCompany().getId())
                    .collect(Collectors.toSet());
                System.out.println("👤 User belongs to " + userCompanyIds.size() + " company(ies) via CompanyStoreUser");
            } else {
                // Fallback: Check UserStoreRole (legacy table)
                List<UserStoreRole> userStoreRoles = userStoreRoleRepository.findByUserAndIsActiveTrue(currentUser);
                
                if (userStoreRoles.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "You must be associated with a company or store to view products"));
                }
                
                // Get company IDs from stores
                userCompanyIds = userStoreRoles.stream()
                    .map(role -> role.getStore())
                    .filter(Objects::nonNull)
                    .map(store -> store.getCompany())
                    .filter(Objects::nonNull)
                    .map(company -> company.getId())
                    .collect(Collectors.toSet());
                System.out.println("👤 User belongs to " + userCompanyIds.size() + " company(ies) via UserStoreRole");
            }
            
            if (userCompanyIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "No valid company associations found"));
            }
            
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<Product> productsPage;
            
            if (search != null && !search.trim().isEmpty()) {
                Page<Product> searchResults = productService.searchProducts(search, pageable);
                List<Product> filtered = filterProductsByCompany(searchResults.getContent(), userCompanyIds);
                productsPage = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            } else if (category != null && !category.trim().isEmpty()) {
                List<Product> categoryProducts = productService.getProductsByCategory(category);
                List<Product> filtered = filterProductsByCompany(categoryProducts, userCompanyIds);
                productsPage = convertListToPage(filtered, pageable);
            } else {
                List<Product> allProducts = productService.getAllActiveProducts();
                List<Product> filtered = filterProductsByCompany(allProducts, userCompanyIds);
                productsPage = convertListToPage(filtered, pageable);
            }
            
            List<ProductResponse> productResponses = productsPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("products", productResponses);
            response.put("currentPage", productsPage.getNumber());
            response.put("totalItems", productsPage.getTotalElements());
            response.put("totalPages", productsPage.getTotalPages());
            response.put("pageSize", productsPage.getSize());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error fetching products: " + e.getMessage());
            System.err.println("❌ Stack trace: " + e.getClass().getName() + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch products: " + e.getMessage()));
        }
    }
    
    // GET /products/count - Get total products count
    @GetMapping("/count")
    public ResponseEntity<?> getProductsCount(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔢 InventSight - Getting products count for user: " + username);
            
            long count = productService.getTotalProductCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Products count: " + count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error getting products count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to get products count: " + e.getMessage()));
        }
    }
    
    // GET /products/low-stock - Get low stock products
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockProducts(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("⚠️ InventSight - Fetching low stock products for user: " + username);
            
            List<Product> lowStockProducts = productService.getLowStockProducts();
            List<ProductResponse> productResponses = lowStockProducts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("products", productResponses);
            response.put("count", productResponses.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Low stock products retrieved: " + productResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching low stock products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch low stock products: " + e.getMessage()));
        }
    }
    
    // GET /products/{id} - Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable UUID id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 InventSight - Fetching product ID: " + id + " for user: " + username);
            
            Product product = productService.getProductById(id);
            ProductResponse productResponse = convertToResponse(product);
            
            Map<String, Object> response = new HashMap<>();
            response.put("product", productResponse);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Product retrieved: " + product.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Product not found: " + e.getMessage()));
        }
    }
    
    // POST /products - Create new product
    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequest productRequest, 
                                         Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("➕ InventSight - Creating product for user: " + username);
            System.out.println("📦 Product name: " + productRequest.getName());
            
            // Check permissions
            User user = userService.getUserByUsername(username);
            
            if (!permissionService.canPerformAction(user, PermissionType.ADD_ITEM)) {
                System.out.println("❌ User lacks permission to add products");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Insufficient permissions to add products. Contact your manager for temporary permission."));
            }
            
            Product product = convertFromRequest(productRequest);
            Product createdProduct = productService.createProduct(product, username);
            
            // Consume one-time permission if used
            try {
                permissionService.consumePermission(user.getId(), PermissionType.ADD_ITEM);
                System.out.println("✅ One-time ADD_ITEM permission consumed");
            } catch (Exception e) {
                // User had manager role, not a one-time permission
                System.out.println("ℹ️ No one-time permission to consume (user has role-based access)");
            }
            
            ProductResponse productResponse = convertToResponse(createdProduct);
            
            Map<String, Object> response = new HashMap<>();
            response.put("product", productResponse);
            response.put("message", "Product created successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Product created successfully: " + createdProduct.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error creating product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to create product: " + e.getMessage()));
        }
    }
    
    // PUT /products/{id} - Update product
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, 
                                         @Valid @RequestBody ProductRequest productRequest, 
                                         Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔄 InventSight - Updating product ID: " + id + " for user: " + username);
            
            // Check permissions
            User user = userService.getUserByUsername(username);
            
            if (!permissionService.canPerformAction(user, PermissionType.EDIT_ITEM)) {
                System.out.println("❌ User lacks permission to edit products");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Insufficient permissions to edit products. Contact your manager for temporary permission."));
            }
            
            Product productUpdates = convertFromRequest(productRequest);
            Product updatedProduct = productService.updateProduct(id, productUpdates, username);
            
            // Consume one-time permission if used
            try {
                permissionService.consumePermission(user.getId(), PermissionType.EDIT_ITEM);
                System.out.println("✅ One-time EDIT_ITEM permission consumed");
            } catch (Exception e) {
                // User had manager role, not a one-time permission
                System.out.println("ℹ️ No one-time permission to consume (user has role-based access)");
            }
            
            ProductResponse productResponse = convertToResponse(updatedProduct);
            
            Map<String, Object> response = new HashMap<>();
            response.put("product", productResponse);
            response.put("message", "Product updated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Product updated successfully: " + updatedProduct.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error updating product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to update product: " + e.getMessage()));
        }
    }
    
    // DELETE /products/{id} - Delete product
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable UUID id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ InventSight - Soft deleting product ID: " + id + " for user: " + username);
            
            // Check permissions
            User user = userService.getUserByUsername(username);
            
            if (!permissionService.canPerformAction(user, PermissionType.DELETE_ITEM)) {
                System.out.println("❌ User lacks permission to delete products");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Insufficient permissions to delete products. Contact your manager for temporary permission."));
            }
            
            Product product = productService.getProductById(id);
            product.setIsActive(false);
            productService.updateProduct(id, product, username);
            
            // Consume one-time permission if used
            try {
                permissionService.consumePermission(user.getId(), PermissionType.DELETE_ITEM);
                System.out.println("✅ One-time DELETE_ITEM permission consumed");
            } catch (Exception e) {
                // User had manager role, not a one-time permission
                System.out.println("ℹ️ No one-time permission to consume (user has role-based access)");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Product deleted successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Product deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error deleting product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to delete product: " + e.getMessage()));
        }
    }
    
    // PUT /products/{id}/stock - Update product stock
    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateProductStock(@PathVariable UUID id, 
                                              @Valid @RequestBody StockUpdateRequest stockRequest, 
                                              Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Updating stock for product ID: " + id + " for user: " + username);
            System.out.println("📦 New quantity: " + stockRequest.getQuantity());
            
            productService.updateStock(id, stockRequest.getQuantity(), username, stockRequest.getReason());
            Product updatedProduct = productService.getProductById(id);
            ProductResponse productResponse = convertToResponse(updatedProduct);
            
            Map<String, Object> response = new HashMap<>();
            response.put("product", productResponse);
            response.put("message", "Stock updated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Stock updated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error updating stock: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to update stock: " + e.getMessage()));
        }
    }
    
    // GET /products/search - Search products
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 InventSight - Searching products for user: " + username);
            System.out.println("🔎 Search query: " + q);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> productsPage = productService.searchProducts(q, pageable);
            
            List<ProductResponse> productResponses = productsPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("products", productResponses);
            response.put("query", q);
            response.put("currentPage", productsPage.getNumber());
            response.put("totalItems", productsPage.getTotalElements());
            response.put("totalPages", productsPage.getTotalPages());
            response.put("pageSize", productsPage.getSize());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Search completed: " + productsPage.getTotalElements() + " results");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error searching products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to search products: " + e.getMessage()));
        }
    }
    
    // GET /products/category/{categoryName} - Get products by category
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable String categoryName, 
                                                 Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🏷️ InventSight - Fetching products by category: " + categoryName + " for user: " + username);
            
            List<Product> products = productService.getProductsByCategory(categoryName);
            List<ProductResponse> productResponses = products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("products", productResponses);
            response.put("category", categoryName);
            response.put("count", productResponses.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Products by category retrieved: " + productResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching products by category: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch products by category: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private ProductResponse convertToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setSku(product.getSku());
        response.setCategory(product.getCategory());
        response.setPrice(product.getPrice());
        response.setCostPrice(product.getCostPrice());
        response.setQuantity(product.getQuantity());
        response.setMaxQuantity(product.getMaxQuantity());
        response.setUnit(product.getUnit());
        response.setSupplier(product.getSupplier());
        response.setLocation(product.getLocation());
        response.setBarcode(product.getBarcode());
        response.setExpiryDate(product.getExpiryDate());
        response.setLowStockThreshold(product.getLowStockThreshold());
        response.setReorderLevel(product.getReorderLevel());
        response.setIsActive(product.getIsActive());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setCreatedBy(product.getCreatedBy());
        response.setUpdatedBy(product.getUpdatedBy());
        
        // Calculate derived fields
        response.setTotalValue(product.getTotalValue());
        response.setLowStock(product.isLowStock());
        response.setOutOfStock(product.isOutOfStock());
        response.setNeedsReorder(product.needsReorder());
        response.setExpired(product.isExpired());
        response.setNearExpiry(product.isNearExpiry(7));
        response.setProfitMargin(product.getProfitMargin());
        
        return response;
    }
    
    private Product convertFromRequest(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        
        // Handle tiered pricing with backward compatibility
        // If new pricing fields are provided, use them
        if (request.getOriginalPrice() != null) {
            product.setOriginalPrice(request.getOriginalPrice());
        } else if (request.getPrice() != null) {
            // Backward compatibility: use legacy price as original price
            product.setOriginalPrice(request.getPrice());
        }
        
        if (request.getOwnerSetSellPrice() != null) {
            product.setOwnerSetSellPrice(request.getOwnerSetSellPrice());
        } else if (request.getPrice() != null) {
            // Backward compatibility: use legacy price as owner set sell price
            product.setOwnerSetSellPrice(request.getPrice());
        }
        
        if (request.getRetailPrice() != null) {
            product.setRetailPrice(request.getRetailPrice());
        } else if (request.getPrice() != null) {
            // Backward compatibility: use legacy price as retail price
            product.setRetailPrice(request.getPrice());
        }
        
        // Set legacy price field for backward compatibility
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        } else if (request.getRetailPrice() != null) {
            product.setPrice(request.getRetailPrice());
        }
        
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
    
    /**
     * Helper method to filter products by user's company IDs
     * Only returns products whose store belongs to one of the user's companies
     */
    private List<Product> filterProductsByCompany(List<Product> products, Set<UUID> userCompanyIds) {
        return products.stream()
            .filter(p -> p.getStore() != null 
                && p.getStore().getCompany() != null 
                && userCompanyIds.contains(p.getStore().getCompany().getId()))
            .collect(Collectors.toList());
    }
    
    private <T> Page<T> convertListToPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        
        List<T> pageContent;
        if (start >= list.size()) {
            pageContent = new ArrayList<>();
        } else {
            pageContent = list.subList(start, end);
        }
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
    }
}