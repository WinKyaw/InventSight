package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    
    List<SaleItem> findBySaleId(Long saleId);
    
    List<SaleItem> findByProductId(Long productId);
    
    @Query("SELECT si FROM SaleItem si WHERE si.sale.createdAt >= :startDate AND si.sale.createdAt <= :endDate")
    List<SaleItem> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT si.productName, SUM(si.quantity) as totalSold FROM SaleItem si WHERE si.sale.status = 'COMPLETED' GROUP BY si.productName ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts();
    
    @Query("SELECT SUM(si.quantity) FROM SaleItem si WHERE si.product.id = :productId AND si.sale.status = 'COMPLETED'")
    Integer getTotalQuantitySoldForProduct(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(si) FROM SaleItem si WHERE si.sale.createdAt >= :startDate AND si.sale.createdAt <= :endDate")
    long countItemsSoldInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}