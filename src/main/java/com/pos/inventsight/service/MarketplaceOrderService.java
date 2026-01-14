package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.MarketplaceOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MarketplaceOrderService {
    
    @Autowired
    private MarketplaceOrderRepository marketplaceOrderRepository;
    
    @Autowired
    private ProductAdService productAdService;
    
    /**
     * Create a new marketplace order
     */
    public MarketplaceOrder createOrder(MarketplaceOrder order, Company buyerCompany, Store buyerStore,
                                       User orderedBy, UUID productAdId) {
        // Get the product ad
        ProductAd productAd = productAdService.getAdById(productAdId);
        
        // Validate quantity
        if (order.getQuantity() < productAd.getMinOrderQuantity()) {
            throw new IllegalArgumentException("Order quantity must be at least " + productAd.getMinOrderQuantity());
        }
        
        if (order.getQuantity() > productAd.getAvailableQuantity()) {
            throw new IllegalArgumentException("Insufficient quantity available");
        }
        
        // Set order details
        order.setBuyerCompany(buyerCompany);
        order.setBuyerStore(buyerStore);
        order.setSellerCompany(productAd.getCompany());
        order.setSellerStore(productAd.getStore());
        order.setProductAd(productAd);
        order.setProductName(productAd.getProductName());
        order.setUnitPrice(productAd.getUnitPrice());
        
        // Calculate total price
        BigDecimal totalPrice = productAd.getUnitPrice().multiply(new BigDecimal(order.getQuantity()));
        order.setTotalPrice(totalPrice);
        
        order.setOrderedBy(orderedBy);
        order.setOrderedAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus(MarketplaceOrderStatus.PENDING);
        
        return marketplaceOrderRepository.save(order);
    }
    
    /**
     * Get orders where company is buyer
     */
    public List<MarketplaceOrder> getBuyerOrders(UUID companyId) {
        return marketplaceOrderRepository.findByBuyerCompanyId(companyId);
    }
    
    /**
     * Get orders where company is seller
     */
    public List<MarketplaceOrder> getSellerOrders(UUID companyId) {
        return marketplaceOrderRepository.findBySellerCompanyId(companyId);
    }
    
    /**
     * Get orders by buyer store
     */
    public List<MarketplaceOrder> getOrdersByBuyerStore(UUID storeId) {
        return marketplaceOrderRepository.findByBuyerStoreId(storeId);
    }
    
    /**
     * Get orders by seller store
     */
    public List<MarketplaceOrder> getOrdersBySellerStore(UUID storeId) {
        return marketplaceOrderRepository.findBySellerStoreId(storeId);
    }
    
    /**
     * Get order by ID
     */
    public MarketplaceOrder getOrderById(UUID id) {
        return marketplaceOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }
    
    /**
     * Confirm order (seller action)
     */
    public MarketplaceOrder confirmOrder(UUID id) {
        MarketplaceOrder order = getOrderById(id);
        
        if (order.getStatus() != MarketplaceOrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        
        order.setStatus(MarketplaceOrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        return marketplaceOrderRepository.save(order);
    }
    
    /**
     * Mark order as shipped
     */
    public MarketplaceOrder shipOrder(UUID id) {
        MarketplaceOrder order = getOrderById(id);
        
        if (order.getStatus() != MarketplaceOrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped");
        }
        
        order.setStatus(MarketplaceOrderStatus.SHIPPED);
        order.setUpdatedAt(LocalDateTime.now());
        
        return marketplaceOrderRepository.save(order);
    }
    
    /**
     * Mark order as delivered
     */
    public MarketplaceOrder deliverOrder(UUID id) {
        MarketplaceOrder order = getOrderById(id);
        
        if (order.getStatus() != MarketplaceOrderStatus.SHIPPED) {
            throw new IllegalStateException("Only shipped orders can be marked as delivered");
        }
        
        order.setStatus(MarketplaceOrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        return marketplaceOrderRepository.save(order);
    }
    
    /**
     * Cancel order
     */
    public MarketplaceOrder cancelOrder(UUID id, String reason) {
        MarketplaceOrder order = getOrderById(id);
        
        if (order.getStatus() == MarketplaceOrderStatus.DELIVERED) {
            throw new IllegalStateException("Delivered orders cannot be cancelled");
        }
        
        order.setStatus(MarketplaceOrderStatus.CANCELLED);
        if (reason != null) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") + 
                          "Cancellation reason: " + reason);
        }
        order.setUpdatedAt(LocalDateTime.now());
        
        return marketplaceOrderRepository.save(order);
    }
}
