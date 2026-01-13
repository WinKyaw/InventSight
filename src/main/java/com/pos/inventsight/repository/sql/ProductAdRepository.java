package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.ProductAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAdRepository extends JpaRepository<ProductAd, UUID> {
    
    /**
     * Find all active ads across all companies (marketplace view)
     */
    @Query("SELECT pa FROM ProductAd pa WHERE pa.isActive = true AND (pa.expiresAt IS NULL OR pa.expiresAt > :now)")
    List<ProductAd> findAllActiveAds(@Param("now") LocalDateTime now);
    
    /**
     * Find active ads for a specific company
     */
    @Query("SELECT pa FROM ProductAd pa WHERE pa.company.id = :companyId AND pa.isActive = true")
    List<ProductAd> findByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find active ads by store
     */
    @Query("SELECT pa FROM ProductAd pa WHERE pa.store.id = :storeId AND pa.isActive = true")
    List<ProductAd> findByStoreId(@Param("storeId") UUID storeId);
    
    /**
     * Find ads by product (across all companies)
     */
    @Query("SELECT pa FROM ProductAd pa WHERE pa.productId = :productId AND pa.isActive = true")
    List<ProductAd> findByProductId(@Param("productId") UUID productId);
    
    /**
     * Search ads by product name
     */
    @Query("SELECT pa FROM ProductAd pa WHERE LOWER(pa.productName) LIKE LOWER(CONCAT('%', :productName, '%')) AND pa.isActive = true")
    List<ProductAd> searchByProductName(@Param("productName") String productName);
}
