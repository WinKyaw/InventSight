package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.PredefinedItem;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.Warehouse;
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
    
    /**
     * @deprecated Use {@link #findBySkuAndStoreId(String, UUID)} or {@link #findBySkuAndWarehouseId(String, UUID)} instead.
     * Since SKU is no longer globally unique, this method may return unexpected results.
     * It will return the first product found with the given SKU, which may not be the one you want.
     */
    @Deprecated
    Optional<Product> findBySku(String sku);
    Optional<Product> findByBarcode(String barcode);
    
    /**
     * Find product by SKU within a specific store
     * Since SKU is only unique per location, we need to specify which store
     */
    @Query("SELECT p FROM Product p WHERE p.sku = :sku AND p.store.id = :storeId")
    Optional<Product> findBySkuAndStoreId(@Param("sku") String sku, @Param("storeId") UUID storeId);

    /**
     * Find product by SKU within a specific warehouse
     * Since SKU is only unique per location, we need to specify which warehouse
     */
    @Query("SELECT p FROM Product p WHERE p.sku = :sku AND p.warehouse.id = :warehouseId")
    Optional<Product> findBySkuAndWarehouseId(@Param("sku") String sku, @Param("warehouseId") UUID warehouseId);

    /**
     * Find all products with a specific SKU across all locations
     * This can return multiple products (same SKU in different stores/warehouses)
     */
    @Query("SELECT p FROM Product p WHERE p.sku = :sku ORDER BY p.createdAt DESC")
    List<Product> findAllBySku(@Param("sku") String sku);
    
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
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = false")
    long countInactiveProducts();
    
    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.isActive = true AND p.category IS NOT NULL GROUP BY p.category ORDER BY COUNT(p) DESC")
    List<Object[]> getTopCategoriesByProductCount();
    
    // Find product by predefined item and store
    Optional<Product> findByPredefinedItemAndStore(PredefinedItem predefinedItem, Store store);
    
    // Find product by predefined item and warehouse
    Optional<Product> findByPredefinedItemAndWarehouse(PredefinedItem predefinedItem, Warehouse warehouse);
    
    // Find product by predefined item ID and store ID (UUID-based alternative)
    Optional<Product> findByPredefinedItemIdAndStoreId(UUID predefinedItemId, UUID storeId);
    
    // Find low stock products by company (quantity <= threshold)
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.lowStockThreshold AND p.company = :company")
    List<Product> findLowStockProductsByCompany(@Param("company") Company company);
    
    /**
     * Find all products associated with a specific warehouse
     * These are products created from predefined items assigned to the warehouse
     */
    @Query("SELECT p FROM Product p WHERE p.warehouse.id = :warehouseId AND p.isActive = true ORDER BY p.name ASC")
    List<Product> findByWarehouseId(@Param("warehouseId") UUID warehouseId);
    
    // Store-based filtering with company isolation
    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.company.id IN :companyIds AND p.isActive = true")
    Page<Product> findByStoreIdAndCompanyIdIn(@Param("storeId") UUID storeId, @Param("companyIds") Set<UUID> companyIds, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.company.id IN :companyIds AND p.category = :category AND p.isActive = true")
    Page<Product> findByStoreIdAndCompanyIdInAndCategory(@Param("storeId") UUID storeId, @Param("companyIds") Set<UUID> companyIds, @Param("category") String category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.company.id IN :companyIds AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    Page<Product> findByStoreIdAndCompanyIdInAndNameContainingIgnoreCase(@Param("storeId") UUID storeId, @Param("companyIds") Set<UUID> companyIds, @Param("name") String name, Pageable pageable);
}