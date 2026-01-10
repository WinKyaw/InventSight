package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.PredefinedItem;
import com.pos.inventsight.model.sql.PredefinedItemWarehouse;
import com.pos.inventsight.model.sql.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PredefinedItemWarehouseRepository extends JpaRepository<PredefinedItemWarehouse, UUID> {
    
    /**
     * Find all warehouse associations for a predefined item
     */
    List<PredefinedItemWarehouse> findByPredefinedItem(PredefinedItem predefinedItem);
    
    /**
     * Find all predefined items for a warehouse
     */
    List<PredefinedItemWarehouse> findByWarehouse(Warehouse warehouse);
    
    /**
     * Find specific association
     */
    Optional<PredefinedItemWarehouse> findByPredefinedItemAndWarehouse(PredefinedItem predefinedItem, Warehouse warehouse);
    
    /**
     * Check if association exists
     */
    boolean existsByPredefinedItemAndWarehouse(PredefinedItem predefinedItem, Warehouse warehouse);
    
    /**
     * Delete association
     */
    void deleteByPredefinedItemAndWarehouse(PredefinedItem predefinedItem, Warehouse warehouse);
    
    /**
     * Delete all associations for a predefined item
     */
    void deleteByPredefinedItem(PredefinedItem predefinedItem);
    
    /**
     * Get all warehouses for a predefined item
     */
    @Query("SELECT piw.warehouse FROM PredefinedItemWarehouse piw WHERE piw.predefinedItem = :item")
    List<Warehouse> findWarehousesByPredefinedItem(@Param("item") PredefinedItem item);
    
    /**
     * Find association between predefined item and warehouse by IDs
     * Used to check if association already exists before creating
     */
    Optional<PredefinedItemWarehouse> findByPredefinedItemIdAndWarehouseId(UUID predefinedItemId, UUID warehouseId);
}
