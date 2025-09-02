package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.ProductRequest;
import com.pos.inventsight.dto.ProductResponse;
import com.pos.inventsight.dto.StockUpdateRequest;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.service.ProductService;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
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
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<Product> productsPage;
            
            if (search != null && !search.trim().isEmpty()) {
                productsPage = productService.searchProducts(search, pageable);
                System.out.println("🔍 InventSight - Search results: " + productsPage.getTotalElements() + " products found");
            } else if (category != null && !category.trim().isEmpty()) {
                List<Product> filteredProducts = productService.getProductsByCategory(category);
                productsPage = convertListToPage(filteredProducts, pageable);
                System.out.println("🏷️ InventSight - Category filter: " + productsPage.getTotalElements() + " products found");
            } else {
                List<Product> allProducts = productService.getAllActiveProducts();
                productsPage = convertListToPage(allProducts, pageable);
                System.out.println("📋 InventSight - Retrieved " + productsPage.getTotalElements() + " active products");
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
            System.out.println("❌ InventSight - Error fetching products: " + e.getMessage());
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
    public ResponseEntity<?> getProductById(@PathVariable Long id, Authentication authentication) {
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
            
            Product product = convertFromRequest(productRequest);
            Product createdProduct = productService.createProduct(product, username);
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
    public ResponseEntity<?> updateProduct(@PathVariable Long id, 
                                         @Valid @RequestBody ProductRequest productRequest, 
                                         Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔄 InventSight - Updating product ID: " + id + " for user: " + username);
            
            Product productUpdates = convertFromRequest(productRequest);
            Product updatedProduct = productService.updateProduct(id, productUpdates, username);
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
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ InventSight - Soft deleting product ID: " + id + " for user: " + username);
            
            Product product = productService.getProductById(id);
            product.setIsActive(false);
            productService.updateProduct(id, product, username);
            
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
    public ResponseEntity<?> updateProductStock(@PathVariable Long id, 
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
    
    private <T> Page<T> convertListToPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        
        List<T> pageContent = list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
    }
}