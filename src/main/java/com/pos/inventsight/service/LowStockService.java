package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LowStockService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    @Autowired
    private ProductAdRepository productAdRepository;
    
    @Autowired
    private TransferRequestRepository transferRequestRepository;
    
    /**
     * Get low stock items for a specific store
     */
    public List<Map<String, Object>> getLowStockItemsForStore(UUID storeId, UUID companyId) {
        List<Map<String, Object>> lowStockItems = new ArrayList<>();
        
        // Find products where quantity < low_stock_threshold
        List<Product> products = productRepository.findByStoreId(storeId);
        
        for (Product product : products) {
            if (product.getQuantity() != null && 
                product.getLowStockThreshold() != null &&
                product.getQuantity() < product.getLowStockThreshold()) {
                
                Map<String, Object> item = new HashMap<>();
                item.put("productId", product.getId());
                item.put("name", product.getName());
                item.put("sku", product.getSku());
                item.put("currentStock", product.getQuantity());
                item.put("lowStockThreshold", product.getLowStockThreshold());
                item.put("reorderLevel", product.getReorderLevel());
                
                // Get warehouse availability
                item.put("warehouseAvailability", getWarehouseAvailability(product, companyId));
                
                // Get merchant suggestions
                item.put("merchantSuggestions", getMerchantSuggestions(companyId));
                
                // Get marketplace listings
                item.put("marketplaceListings", getMarketplaceListings(product));
                
                // Get transfer request history
                item.put("transferRequests", getTransferRequestHistory(product.getId(), companyId));
                
                lowStockItems.add(item);
            }
        }
        
        return lowStockItems;
    }
    
    /**
     * Get low stock items for a specific warehouse
     */
    public List<Map<String, Object>> getLowStockItemsForWarehouse(UUID warehouseId, UUID companyId) {
        List<Map<String, Object>> lowStockItems = new ArrayList<>();
        
        // Find products in warehouse where quantity < low_stock_threshold
        List<Product> products = productRepository.findByWarehouseId(warehouseId);
        
        for (Product product : products) {
            if (product.getQuantity() != null && 
                product.getLowStockThreshold() != null &&
                product.getQuantity() < product.getLowStockThreshold()) {
                
                Map<String, Object> item = new HashMap<>();
                item.put("productId", product.getId());
                item.put("name", product.getName());
                item.put("sku", product.getSku());
                item.put("currentStock", product.getQuantity());
                item.put("lowStockThreshold", product.getLowStockThreshold());
                item.put("reorderLevel", product.getReorderLevel());
                
                // Get merchant suggestions
                item.put("merchantSuggestions", getMerchantSuggestions(companyId));
                
                // Get marketplace listings
                item.put("marketplaceListings", getMarketplaceListings(product));
                
                lowStockItems.add(item);
            }
        }
        
        return lowStockItems;
    }
    
    /**
     * Get warehouse availability for a product
     */
    private List<Map<String, Object>> getWarehouseAvailability(Product product, UUID companyId) {
        List<Map<String, Object>> availability = new ArrayList<>();
        
        // Find all products with same SKU in warehouses of the same company
        List<Product> warehouseProducts = productRepository.findBySku(product.getSku())
            .stream()
            .filter(p -> p.getWarehouse() != null && 
                        p.getCompany() != null && 
                        p.getCompany().getId().equals(companyId))
            .collect(Collectors.toList());
        
        for (Product warehouseProduct : warehouseProducts) {
            if (warehouseProduct.getQuantity() != null && warehouseProduct.getQuantity() > 0) {
                Map<String, Object> warehouseInfo = new HashMap<>();
                warehouseInfo.put("warehouseId", warehouseProduct.getWarehouse().getId());
                warehouseInfo.put("name", warehouseProduct.getWarehouse().getName());
                warehouseInfo.put("quantity", warehouseProduct.getQuantity());
                availability.add(warehouseInfo);
            }
        }
        
        return availability;
    }
    
    /**
     * Get merchant suggestions for a company
     */
    private List<Map<String, Object>> getMerchantSuggestions(UUID companyId) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        List<Merchant> merchants = merchantRepository.findActiveByCompanyId(companyId);
        
        for (Merchant merchant : merchants) {
            Map<String, Object> merchantInfo = new HashMap<>();
            merchantInfo.put("merchantId", merchant.getId());
            merchantInfo.put("name", merchant.getName());
            merchantInfo.put("contactPerson", merchant.getContactPerson());
            merchantInfo.put("phone", merchant.getPhone());
            merchantInfo.put("email", merchant.getEmail());
            merchantInfo.put("location", merchant.getLocation());
            suggestions.add(merchantInfo);
        }
        
        return suggestions;
    }
    
    /**
     * Get marketplace listings for a product
     */
    private List<Map<String, Object>> getMarketplaceListings(Product product) {
        List<Map<String, Object>> listings = new ArrayList<>();
        
        // Search for ads by product name or SKU
        List<ProductAd> ads = productAdRepository.searchByProductName(product.getName());
        
        for (ProductAd ad : ads) {
            Map<String, Object> listing = new HashMap<>();
            listing.put("adId", ad.getId());
            listing.put("sellerCompany", ad.getCompany() != null ? ad.getCompany().getName() : null);
            listing.put("sellerStore", ad.getStore() != null ? ad.getStore().getStoreName() : null);
            listing.put("productName", ad.getProductName());
            listing.put("price", ad.getUnitPrice());
            listing.put("quantity", ad.getAvailableQuantity());
            listing.put("minOrderQuantity", ad.getMinOrderQuantity());
            listings.add(listing);
        }
        
        return listings;
    }
    
    /**
     * Get transfer request history for a product
     */
    private List<Map<String, Object>> getTransferRequestHistory(UUID productId, UUID companyId) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        List<TransferRequest> requests = transferRequestRepository.findByProductIdAndCompanyId(productId, companyId);
        
        for (TransferRequest request : requests) {
            Map<String, Object> requestInfo = new HashMap<>();
            requestInfo.put("id", request.getId());
            requestInfo.put("status", request.getStatus());
            requestInfo.put("requestedQuantity", request.getRequestedQuantity());
            requestInfo.put("approvedQuantity", request.getApprovedQuantity());
            requestInfo.put("priority", request.getPriority());
            requestInfo.put("requestedAt", request.getRequestedAt());
            requestInfo.put("fromWarehouse", request.getFromWarehouse() != null ? 
                           request.getFromWarehouse().getName() : null);
            requestInfo.put("toStore", request.getToStore() != null ? 
                           request.getToStore().getStoreName() : null);
            history.add(requestInfo);
        }
        
        return history;
    }
}
