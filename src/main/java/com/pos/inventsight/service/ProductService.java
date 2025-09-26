package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.InsufficientStockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    // CRUD Operations
    public Product createProduct(Product product, String createdBy) {
        System.out.println("📦 Creating new product: " + product.getName());
        System.out.println("📅 Current DateTime (UTC): 2025-08-26 08:47:36");
        System.out.println("👤 Created by: " + (createdBy != null ? createdBy : "WinKyaw"));
        
        // Ensure product is associated with current user's store
        if (product.getStore() == null) {
            Store currentStore = userService.getCurrentUserStore();
            if (currentStore == null) {
                throw new IllegalStateException("Cannot create product: No active store found for current user");
            }
            product.setStore(currentStore);
            System.out.println("🏪 Associated product with store: " + currentStore.getStoreName());
        }
        
        // Generate SKU if not provided
        if (product.getSku() == null || product.getSku().isEmpty()) {
            product.setSku(generateSku(product.getName()));
        }
        
        product.setCreatedBy(createdBy != null ? createdBy : "WinKyaw");
        product.setCreatedAt(LocalDateTime.now());
        
        Product savedProduct = productRepository.save(product);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            createdBy != null ? createdBy : "WinKyaw", 
            "PRODUCT_CREATED", 
            "PRODUCT", 
            "New product added: " + product.getName() + " (Qty: " + product.getQuantity() + ")"
        );
        
        System.out.println("✅ Product created successfully: " + savedProduct.getName() + " - SKU: " + savedProduct.getSku());
        return savedProduct;
    }
    
    public Product updateProduct(UUID productId, Product productUpdates, String updatedBy) {
        Product existingProduct = getProductById(productId);
        
        // Store old values for logging
        String oldName = existingProduct.getName();
        Integer oldQuantity = existingProduct.getQuantity();
        BigDecimal oldPrice = existingProduct.getPrice();
        
        // Update fields
        if (productUpdates.getName() != null) {
            existingProduct.setName(productUpdates.getName());
        }
        if (productUpdates.getDescription() != null) {
            existingProduct.setDescription(productUpdates.getDescription());
        }
        if (productUpdates.getPrice() != null) {
            existingProduct.setPrice(productUpdates.getPrice());
        }
        if (productUpdates.getCostPrice() != null) {
            existingProduct.setCostPrice(productUpdates.getCostPrice());
        }
        if (productUpdates.getQuantity() != null) {
            existingProduct.setQuantity(productUpdates.getQuantity());
        }
        if (productUpdates.getMaxQuantity() != null) {
            existingProduct.setMaxQuantity(productUpdates.getMaxQuantity());
        }
        if (productUpdates.getUnit() != null) {
            existingProduct.setUnit(productUpdates.getUnit());
        }
        if (productUpdates.getLocation() != null) {
            existingProduct.setLocation(productUpdates.getLocation());
        }
        if (productUpdates.getExpiryDate() != null) {
            existingProduct.setExpiryDate(productUpdates.getExpiryDate());
        }
        if (productUpdates.getCategory() != null) {
            existingProduct.setCategory(productUpdates.getCategory());
        }
        if (productUpdates.getSupplier() != null) {
            existingProduct.setSupplier(productUpdates.getSupplier());
        }
        if (productUpdates.getBarcode() != null) {
            existingProduct.setBarcode(productUpdates.getBarcode());
        }
        if (productUpdates.getLowStockThreshold() != null) {
            existingProduct.setLowStockThreshold(productUpdates.getLowStockThreshold());
        }
        if (productUpdates.getReorderLevel() != null) {
            existingProduct.setReorderLevel(productUpdates.getReorderLevel());
        }
        
        existingProduct.setUpdatedAt(LocalDateTime.now());
        existingProduct.setUpdatedBy(updatedBy != null ? updatedBy : "WinKyaw");
        
        Product updatedProduct = productRepository.save(existingProduct);
        
        // Log activity
        String changeDescription = buildChangeDescription(oldName, oldQuantity, oldPrice, updatedProduct);
        activityLogService.logActivity(
            null, 
            updatedBy != null ? updatedBy : "WinKyaw", 
            "PRODUCT_UPDATED", 
            "PRODUCT", 
            changeDescription
        );
        
        System.out.println("📝 Product updated: " + updatedProduct.getName());
        return updatedProduct;
    }
    
    public Product getProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
    }
    
    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }
    
    public List<Product> getAllActiveProducts() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            // Fallback to global query for default tenant
            return productRepository.findByIsActiveTrue();
        }
        // Use tenant-aware query
        return productRepository.findByStoreAndIsActiveTrue(currentStore);
    }
    
    public Page<Product> searchProducts(String searchTerm, Pageable pageable) {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            // Fallback to global query for default tenant
            return productRepository.searchProducts(searchTerm, pageable);
        }
        // Use tenant-aware query
        return productRepository.searchProductsByStore(currentStore, searchTerm, pageable);
    }
    
    public List<Product> getProductsByCategory(String category) {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            // Fallback to global query for default tenant
            return productRepository.findByCategory(category);
        }
        // Use tenant-aware query
        return productRepository.findByStoreAndCategory(currentStore, category);
    }
    
    public List<String> getAllCategories() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            // Fallback to global query for default tenant
            return productRepository.findAllCategories();
        }
        // Use tenant-aware query
        return productRepository.findAllCategoriesByStore(currentStore);
    }
    
    public List<String> getAllSuppliers() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            // Fallback to global query for default tenant
            return productRepository.findAllSuppliers();
        }
        // Use tenant-aware query
        return productRepository.findAllSuppliersByStore(currentStore);
    }
    
    // Inventory Management
    public void updateStock(UUID productId, Integer newQuantity, String updatedBy, String reason) {
        Product product = getProductById(productId);
        Integer oldQuantity = product.getQuantity();
        
        product.setQuantity(newQuantity);
        product.setUpdatedAt(LocalDateTime.now());
        
        productRepository.save(product);
        
        // Log stock change
        activityLogService.logActivity(
            null, 
            updatedBy != null ? updatedBy : "WinKyaw", 
            "STOCK_UPDATED", 
            "PRODUCT", 
            String.format("Stock updated for %s: %d → %d (%s)", 
                product.getName(), oldQuantity, newQuantity, reason)
        );
        
        // Check for alerts
        if (product.isLowStock()) {
            System.out.println("⚠️ Low stock alert: " + product.getName() + " (Qty: " + product.getQuantity() + ")");
        }
        if (product.needsReorder()) {
            System.out.println("🔄 Reorder recommendation: " + product.getName() + " (Qty: " + product.getQuantity() + ")");
        }
    }
    
    public void reduceStock(UUID productId, Integer quantity, String reason) {
        Product product = getProductById(productId);
        
        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException(
                "Insufficient stock for " + product.getName() + 
                ". Available: " + product.getQuantity() + ", Requested: " + quantity
            );
        }
        
        updateStock(productId, product.getQuantity() - quantity, "SYSTEM", reason);
    }
    
    public void increaseStock(UUID productId, Integer quantity, String reason) {
        Product product = getProductById(productId);
        updateStock(productId, product.getQuantity() + quantity, "SYSTEM", reason);
    }
    
    // Stock Alerts and Analytics
    public List<Product> getLowStockProducts() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            return productRepository.findLowStockProducts();
        }
        return productRepository.findLowStockProductsByStore(currentStore);
    }
    
    public List<Product> getOutOfStockProducts() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            return productRepository.findOutOfStockProducts();
        }
        return productRepository.findOutOfStockProductsByStore(currentStore);
    }
    
    public List<Product> getProductsNeedingReorder() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            return productRepository.findProductsNeedingReorder();
        }
        return productRepository.findProductsNeedingReorderByStore(currentStore);
    }
    
    // Analytics
    public long getTotalProductCount() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            return productRepository.countActiveProducts();
        }
        return productRepository.countActiveProductsByStore(currentStore);
    }
    
    public BigDecimal getTotalInventoryValue() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            return productRepository.getTotalInventoryValue();
        }
        return productRepository.getTotalInventoryValueByStore(currentStore);
    }
    
    public Map<String, Long> getCategoryDistribution() {
        return getAllActiveProducts().stream()
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));
    }
    
    public Map<String, Long> getSupplierDistribution() {
        return getAllActiveProducts().stream()
                .filter(p -> p.getSupplier() != null)
                .collect(Collectors.groupingBy(Product::getSupplier, Collectors.counting()));
    }
    
    // Business Logic
    public void deleteProduct(UUID productId, String deletedBy) {
        Product product = getProductById(productId);
        product.setIsActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        
        productRepository.save(product);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            deletedBy != null ? deletedBy : "WinKyaw", 
            "PRODUCT_DELETED", 
            "PRODUCT", 
            "Product deactivated: " + product.getName()
        );
    }
    
    // Helper Methods
    private String generateSku(String productName) {
        String prefix = productName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.length() > 3) {
            prefix = prefix.substring(0, 3);
        }
        return prefix + "-" + System.currentTimeMillis();
    }
    
    private String buildChangeDescription(String oldName, Integer oldQuantity, BigDecimal oldPrice, Product newProduct) {
        StringBuilder description = new StringBuilder();
        description.append("Updated ").append(newProduct.getName()).append(": ");
        
        if (!oldName.equals(newProduct.getName())) {
            description.append("Name: ").append(oldName).append(" → ").append(newProduct.getName()).append("; ");
        }
        if (!oldQuantity.equals(newProduct.getQuantity())) {
            description.append("Qty: ").append(oldQuantity).append(" → ").append(newProduct.getQuantity()).append("; ");
        }
        if (oldPrice.compareTo(newProduct.getPrice()) != 0) {
            description.append("Price: $").append(oldPrice).append(" → $").append(newProduct.getPrice()).append("; ");
        }
        
        return description.toString();
    }
}