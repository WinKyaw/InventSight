package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.SaleItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    
    List<SaleItem> findBySaleId(Long saleId);
    
    List<SaleItem> findByProductId(UUID productId);
    
    @Query("SELECT si FROM SaleItem si WHERE si.sale.createdAt >= :startDate AND si.sale.createdAt <= :endDate")
    List<SaleItem> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT si.productName, SUM(si.quantity) as totalSold FROM SaleItem si WHERE si.sale.status = 'COMPLETED' GROUP BY si.productName ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts();
    
    @Query("SELECT SUM(si.quantity) FROM SaleItem si WHERE si.product.id = :productId AND si.sale.status = 'COMPLETED'")
    Integer getTotalQuantitySoldForProduct(@Param("productId") UUID productId);
    
    @Query("SELECT COUNT(si) FROM SaleItem si WHERE si.sale.createdAt >= :startDate AND si.sale.createdAt <= :endDate")
    long countItemsSoldInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find top selling products with sales data
     * Returns: [productName, totalQuantity, totalRevenue, categoryName]
     */
    @Query("""
        SELECT p.name, 
               SUM(si.quantity) as totalQuantity,
               SUM(si.quantity * si.unitPrice) as totalRevenue,
               COALESCE(p.category, 'Uncategorized') as categoryName
        FROM SaleItem si
        JOIN si.product p
        JOIN si.sale s
        WHERE s.status = 'COMPLETED'
        GROUP BY p.id, p.name, p.category
        ORDER BY totalQuantity DESC
        """)
    List<Object[]> findTopSellingProducts(Pageable pageable);
    
    /**
     * Convenience method to get top N selling products
     */
    default List<Object[]> findTopSellingProducts(int limit) {
        return findTopSellingProducts(org.springframework.data.domain.PageRequest.of(0, limit));
    }
}