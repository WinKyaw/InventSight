package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WarehouseInventory entities
 */
@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, UUID> {

    /**
     * Find inventory by warehouse and product
     */
    Optional<WarehouseInventory> findByWarehouseAndProduct(Warehouse warehouse, Product product);

    /**
     * Find inventory by warehouse ID and product ID
     */
    Optional<WarehouseInventory> findByWarehouseIdAndProductId(UUID warehouseId, UUID productId);

    /**
     * Find all inventory for a specific warehouse
     */
    List<WarehouseInventory> findByWarehouse(Warehouse warehouse);

    /**
     * Find all inventory for a warehouse by ID
     */
    List<WarehouseInventory> findByWarehouseId(UUID warehouseId);

    /**
     * Find all locations where a product is stored
     */
    List<WarehouseInventory> findByProduct(Product product);

    /**
     * Find all locations where a product is stored by product ID
     */
    List<WarehouseInventory> findByProductId(UUID productId);

    /**
     * Find items with low stock (available quantity <= reorder point)
     */
    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.currentQuantity - wi.reservedQuantity <= wi.reorderPoint")
    List<WarehouseInventory> findLowStockItems();

    /**
     * Find items with low stock in specific warehouse
     */
    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.warehouse.id = :warehouseId AND " +
           "wi.currentQuantity - wi.reservedQuantity <= wi.reorderPoint")
    List<WarehouseInventory> findLowStockItemsByWarehouse(@Param("warehouseId") UUID warehouseId);

    /**
     * Find items with overstock (current quantity > maximum stock level)
     */
    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.maximumStockLevel IS NOT NULL AND " +
           "wi.currentQuantity > wi.maximumStockLevel")
    List<WarehouseInventory> findOverstockItems();

    /**
     * Find items with no stock (current quantity = 0)
     */
    List<WarehouseInventory> findByCurrentQuantity(Integer quantity);

    /**
     * Find items with available stock > 0
     */
    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.currentQuantity - wi.reservedQuantity > 0")
    List<WarehouseInventory> findItemsWithAvailableStock();

    /**
     * Find items with available stock > 0 in specific warehouse
     */
    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.warehouse.id = :warehouseId AND " +
           "wi.currentQuantity - wi.reservedQuantity > 0")
    List<WarehouseInventory> findItemsWithAvailableStockByWarehouse(@Param("warehouseId") UUID warehouseId);

    /**
     * Get total inventory value for a warehouse
     */
    @Query("SELECT SUM(wi.currentQuantity * p.originalPrice) FROM WarehouseInventory wi " +
           "JOIN wi.product p WHERE wi.warehouse.id = :warehouseId")
    Double getTotalInventoryValueByWarehouse(@Param("warehouseId") UUID warehouseId);

    /**
     * Get total quantity of a product across all warehouses
     */
    @Query("SELECT SUM(wi.currentQuantity) FROM WarehouseInventory wi WHERE wi.product.id = :productId")
    Long getTotalQuantityByProduct(@Param("productId") UUID productId);

    /**
     * Get total available quantity of a product across all warehouses
     */
    @Query("SELECT SUM(wi.currentQuantity - wi.reservedQuantity) FROM WarehouseInventory wi " +
           "WHERE wi.product.id = :productId")
    Long getTotalAvailableQuantityByProduct(@Param("productId") UUID productId);

    /**
     * Search inventory by product name or SKU
     */
    @Query("SELECT wi FROM WarehouseInventory wi JOIN wi.product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<WarehouseInventory> searchByProductNameOrSku(@Param("searchTerm") String searchTerm);

    /**
     * Search inventory by product name or SKU in specific warehouse
     */
    @Query("SELECT wi FROM WarehouseInventory wi JOIN wi.product p WHERE wi.warehouse.id = :warehouseId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<WarehouseInventory> searchByProductNameOrSkuInWarehouse(
        @Param("warehouseId") UUID warehouseId, 
        @Param("searchTerm") String searchTerm
    );

    /**
     * Find inventory items by location within warehouse
     */
    List<WarehouseInventory> findByLocationInWarehouseContainingIgnoreCase(String location);

    /**
     * Count total inventory items
     */
    long count();

    /**
     * Count inventory items in specific warehouse
     */
    long countByWarehouseId(UUID warehouseId);
}