package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.PreexistingItem;
import com.pos.inventsight.model.sql.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreexistingItemRepository extends JpaRepository<PreexistingItem, UUID> {
    
    /**
     * Find all non-deleted items for a specific store
     */
    @Query("SELECT p FROM PreexistingItem p WHERE p.store.id = :storeId AND p.isDeleted = false")
    List<PreexistingItem> findByStoreIdAndIsDeletedFalse(@Param("storeId") UUID storeId);
    
    /**
     * Find all items (including deleted) for a specific store
     */
    @Query("SELECT p FROM PreexistingItem p WHERE p.store.id = :storeId")
    List<PreexistingItem> findByStoreId(@Param("storeId") UUID storeId);
    
    /**
     * Find item by SKU and store
     */
    @Query("SELECT p FROM PreexistingItem p WHERE p.store.id = :storeId AND p.sku = :sku")
    Optional<PreexistingItem> findByStoreIdAndSku(@Param("storeId") UUID storeId, @Param("sku") String sku);
    
    /**
     * Check if SKU exists in store
     */
    @Query("SELECT COUNT(p) > 0 FROM PreexistingItem p WHERE p.store.id = :storeId AND p.sku = :sku AND p.isDeleted = false")
    boolean existsByStoreIdAndSku(@Param("storeId") UUID storeId, @Param("sku") String sku);
    
    /**
     * Find items by category
     */
    @Query("SELECT p FROM PreexistingItem p WHERE p.store.id = :storeId AND p.category = :category AND p.isDeleted = false")
    List<PreexistingItem> findByStoreIdAndCategory(@Param("storeId") UUID storeId, @Param("category") String category);
    
    /**
     * Search items by name
     */
    @Query("SELECT p FROM PreexistingItem p WHERE p.store.id = :storeId AND LOWER(p.itemName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND p.isDeleted = false")
    List<PreexistingItem> searchByItemName(@Param("storeId") UUID storeId, @Param("searchTerm") String searchTerm);
    
    /**
     * Count non-deleted items for a store
     */
    @Query("SELECT COUNT(p) FROM PreexistingItem p WHERE p.store.id = :storeId AND p.isDeleted = false")
    long countByStoreId(@Param("storeId") UUID storeId);
}
