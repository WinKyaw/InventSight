package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.PredefinedItem;
import com.pos.inventsight.model.sql.PredefinedItemStore;
import com.pos.inventsight.model.sql.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PredefinedItemStoreRepository extends JpaRepository<PredefinedItemStore, UUID> {
    
    /**
     * Find all store associations for a predefined item
     */
    List<PredefinedItemStore> findByPredefinedItem(PredefinedItem predefinedItem);
    
    /**
     * Find all predefined items for a store
     */
    List<PredefinedItemStore> findByStore(Store store);
    
    /**
     * Find specific association
     */
    Optional<PredefinedItemStore> findByPredefinedItemAndStore(PredefinedItem predefinedItem, Store store);
    
    /**
     * Check if association exists
     */
    boolean existsByPredefinedItemAndStore(PredefinedItem predefinedItem, Store store);
    
    /**
     * Delete association
     */
    void deleteByPredefinedItemAndStore(PredefinedItem predefinedItem, Store store);
    
    /**
     * Delete all associations for a predefined item
     */
    void deleteByPredefinedItem(PredefinedItem predefinedItem);
    
    /**
     * Get all stores for a predefined item
     */
    @Query("SELECT pis.store FROM PredefinedItemStore pis WHERE pis.predefinedItem = :item")
    List<Store> findStoresByPredefinedItem(@Param("item") PredefinedItem item);
}
