package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Product;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    // CRUD Operations
    public Product createProduct(Product product, String createdBy) {
        System.out.println("📦 Creating new product: " + product.getName());
        System.out.println("📅 Current DateTime (UTC): 2025-08-26 08:47:36");
        System.out.println("👤 Created by: " + (createdBy != null ? createdBy : "WinKyaw"));
        
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
    
    public Product updateProduct(Long productId, Product productUpdates, String updatedBy) {
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
        if (productUpdates.getQuantity() != null) {
            existingProduct.setQuantity(productUpdates.getQuantity());
        }
        if (productUpdates.getCategory() != null) {
            existingProduct.setCategory(productUpdates.getCategory());
        }
        if (productUpdates.getSupplier() != null) {
            existingProduct.setSupplier(productUpdates.getSupplier());
        }
        if (productUpdates.getLowStockThreshold() != null) {
            existingProduct.setLowStockThreshold(productUpdates.getLowStockThreshold());
        }
        if (productUpdates.getReorderLevel() != null) {
            existingProduct.setReorderLevel(productUpdates.getReorderLevel());
        }
        
        existingProduct.setUpdatedAt(LocalDateTime.now());
        
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
    
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
    }
    
    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }
    
    public List<Product> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }
    
    public Page<Product> searchProducts(String searchTerm, Pageable pageable) {
        return productRepository.searchProducts(searchTerm, pageable);
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }
    
    public List<String> getAllSuppliers() {
        return productRepository.findAllSuppliers();
    }
    
    // Inventory Management
    public void updateStock(Long productId, Integer newQuantity, String updatedBy, String reason) {
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
    
    public void reduceStock(Long productId, Integer quantity, String reason) {
        Product product = getProductById(productId);
        
        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException(
                "Insufficient stock for " + product.getName() + 
                ". Available: " + product.getQuantity() + ", Requested: " + quantity
            );
        }
        
        updateStock(productId, product.getQuantity() - quantity, "SYSTEM", reason);
    }
    
    public void increaseStock(Long productId, Integer quantity, String reason) {
        Product product = getProductById(productId);
        updateStock(productId, product.getQuantity() + quantity, "SYSTEM", reason);
    }
    
    // Stock Alerts and Analytics
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }
    
    public List<Product> getOutOfStockProducts() {
        return productRepository.findOutOfStockProducts();
    }
    
    public List<Product> getProductsNeedingReorder() {
        return productRepository.findProductsNeedingReorder();
    }
    
    // Analytics
    public long getTotalProductCount() {
        return productRepository.countActiveProducts();
    }
    
    public BigDecimal getTotalInventoryValue() {
        return productRepository.getTotalInventoryValue();
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
    public void deleteProduct(Long productId, String deletedBy) {
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