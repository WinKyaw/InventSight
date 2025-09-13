package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    Optional<Product> findBySku(String sku);
    Optional<Product> findByBarcode(String barcode);
    
    // Multi-tenant aware queries
    Optional<Product> findBySkuAndStore(String sku, Store store);
    Optional<Product> findByBarcodeAndStore(String barcode, Store store);
    
    List<Product> findByStore(Store store);
    List<Product> findByStoreAndIsActiveTrue(Store store);
    List<Product> findByStoreAndCategory(Store store, String category);
    List<Product> findByStoreAndSupplier(Store store, String supplier);
    
    List<Product> findByCategory(String category);
    List<Product> findByIsActiveTrue();
    List<Product> findBySupplier(String supplier);
    
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.isActive = true")
    List<String> findAllCategories();
    
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.store = :store AND p.isActive = true")
    List<String> findAllCategoriesByStore(@Param("store") Store store);
    
    @Query("SELECT DISTINCT p.supplier FROM Product p WHERE p.isActive = true AND p.supplier IS NOT NULL")
    List<String> findAllSuppliers();
    
    @Query("SELECT DISTINCT p.supplier FROM Product p WHERE p.store = :store AND p.isActive = true AND p.supplier IS NOT NULL")
    List<String> findAllSuppliersByStore(@Param("store") Store store);
    
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.lowStockThreshold AND p.isActive = true")
    List<Product> findLowStockProducts();
    
    @Query("SELECT p FROM Product p WHERE p.store = :store AND p.quantity <= p.lowStockThreshold AND p.isActive = true")
    List<Product> findLowStockProductsByStore(@Param("store") Store store);
    
    @Query("SELECT p FROM Product p WHERE p.quantity = 0 AND p.isActive = true")
    List<Product> findOutOfStockProducts();
    
    @Query("SELECT p FROM Product p WHERE p.store = :store AND p.quantity = 0 AND p.isActive = true")
    List<Product> findOutOfStockProductsByStore(@Param("store") Store store);
    
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderLevel AND p.isActive = true")
    List<Product> findProductsNeedingReorder();
    
    @Query("SELECT p FROM Product p WHERE p.store = :store AND p.quantity <= p.reorderLevel AND p.isActive = true")
    List<Product> findProductsNeedingReorderByStore(@Param("store") Store store);
    
    @Query("SELECT p FROM Product p WHERE (p.name LIKE %:searchTerm% OR p.description LIKE %:searchTerm% OR p.category LIKE %:searchTerm% OR p.sku LIKE %:searchTerm%) AND p.isActive = true")
    Page<Product> searchProducts(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.store = :store AND (p.name LIKE %:searchTerm% OR p.description LIKE %:searchTerm% OR p.category LIKE %:searchTerm% OR p.sku LIKE %:searchTerm%) AND p.isActive = true")
    Page<Product> searchProductsByStore(@Param("store") Store store, @Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.isActive = true")
    Page<Product> findByCategoryWithPaging(@Param("category") String category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.store = :store AND p.category = :category AND p.isActive = true")
    Page<Product> findByCategoryWithPagingByStore(@Param("store") Store store, @Param("category") String category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT p FROM Product p WHERE p.store = :store AND p.retailPrice BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    List<Product> findByRetailPriceRangeByStore(@Param("store") Store store, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    long countActiveProducts();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.store = :store AND p.isActive = true")
    long countActiveProductsByStore(@Param("store") Store store);
    
    @Query("SELECT SUM(p.quantity * p.price) FROM Product p WHERE p.isActive = true")
    BigDecimal getTotalInventoryValue();
    
    @Query("SELECT SUM(p.quantity * p.retailPrice) FROM Product p WHERE p.store = :store AND p.isActive = true")
    BigDecimal getTotalInventoryValueByStore(@Param("store") Store store);
    
    @Query("SELECT p FROM Product p WHERE p.createdBy = :createdBy AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findByCreatedBy(@Param("createdBy") String createdBy);
    
    @Query("SELECT p FROM Product p WHERE p.store = :store AND p.createdBy = :createdBy AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findByStoreAndCreatedBy(@Param("store") Store store, @Param("createdBy") String createdBy);
}