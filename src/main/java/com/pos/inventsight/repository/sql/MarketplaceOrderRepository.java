package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.MarketplaceOrder;
import com.pos.inventsight.model.sql.MarketplaceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, UUID> {
    
    /**
     * Find all orders where company is the buyer
     */
    @Query("SELECT mo FROM MarketplaceOrder mo WHERE mo.buyerCompany.id = :companyId ORDER BY mo.orderedAt DESC")
    List<MarketplaceOrder> findByBuyerCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find all orders where company is the seller
     */
    @Query("SELECT mo FROM MarketplaceOrder mo WHERE mo.sellerCompany.id = :companyId ORDER BY mo.orderedAt DESC")
    List<MarketplaceOrder> findBySellerCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find orders by buyer store
     */
    @Query("SELECT mo FROM MarketplaceOrder mo WHERE mo.buyerStore.id = :storeId ORDER BY mo.orderedAt DESC")
    List<MarketplaceOrder> findByBuyerStoreId(@Param("storeId") UUID storeId);
    
    /**
     * Find orders by seller store
     */
    @Query("SELECT mo FROM MarketplaceOrder mo WHERE mo.sellerStore.id = :storeId ORDER BY mo.orderedAt DESC")
    List<MarketplaceOrder> findBySellerStoreId(@Param("storeId") UUID storeId);
    
    /**
     * Find orders by status for buyer
     */
    @Query("SELECT mo FROM MarketplaceOrder mo WHERE mo.buyerCompany.id = :companyId AND mo.status = :status ORDER BY mo.orderedAt DESC")
    List<MarketplaceOrder> findByBuyerCompanyIdAndStatus(@Param("companyId") UUID companyId, @Param("status") MarketplaceOrderStatus status);
    
    /**
     * Find orders by status for seller
     */
    @Query("SELECT mo FROM MarketplaceOrder mo WHERE mo.sellerCompany.id = :companyId AND mo.status = :status ORDER BY mo.orderedAt DESC")
    List<MarketplaceOrder> findBySellerCompanyIdAndStatus(@Param("companyId") UUID companyId, @Param("status") MarketplaceOrderStatus status);
}
