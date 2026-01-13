package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.ProductAdRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductAdService {
    
    @Autowired
    private ProductAdRepository productAdRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ImageService imageService;
    
    /**
     * Create a new product ad
     */
    public ProductAd createProductAd(ProductAd productAd, Company company, Store store, String createdBy) {
        productAd.setCompany(company);
        productAd.setStore(store);
        productAd.setCreatedBy(createdBy);
        productAd.setCreatedAt(LocalDateTime.now());
        productAd.setIsActive(true);
        
        return productAdRepository.save(productAd);
    }
    
    /**
     * Get all active ads (marketplace view)
     */
    public List<ProductAd> getAllActiveAds() {
        return productAdRepository.findAllActiveAds(LocalDateTime.now());
    }
    
    /**
     * Get ads for a specific company
     */
    public List<ProductAd> getAdsByCompany(UUID companyId) {
        return productAdRepository.findByCompanyId(companyId);
    }
    
    /**
     * Get ads for a specific store
     */
    public List<ProductAd> getAdsByStore(UUID storeId) {
        return productAdRepository.findByStoreId(storeId);
    }
    
    /**
     * Get ad by ID
     */
    public ProductAd getAdById(UUID id) {
        return productAdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product ad not found with id: " + id));
    }
    
    /**
     * Search ads by product name
     */
    public List<ProductAd> searchAdsByProductName(String productName) {
        return productAdRepository.searchByProductName(productName);
    }
    
    /**
     * Update product ad
     */
    public ProductAd updateProductAd(UUID id, ProductAd adDetails) {
        ProductAd ad = getAdById(id);
        
        if (adDetails.getProductName() != null) {
            ad.setProductName(adDetails.getProductName());
        }
        if (adDetails.getDescription() != null) {
            ad.setDescription(adDetails.getDescription());
        }
        if (adDetails.getUnitPrice() != null) {
            ad.setUnitPrice(adDetails.getUnitPrice());
        }
        if (adDetails.getAvailableQuantity() != null) {
            ad.setAvailableQuantity(adDetails.getAvailableQuantity());
        }
        if (adDetails.getMinOrderQuantity() != null) {
            ad.setMinOrderQuantity(adDetails.getMinOrderQuantity());
        }
        if (adDetails.getExpiresAt() != null) {
            ad.setExpiresAt(adDetails.getExpiresAt());
        }
        
        return productAdRepository.save(ad);
    }
    
    /**
     * Update product ad image
     */
    public ProductAd updateProductAdImage(UUID id, String imageUrl) {
        ProductAd ad = getAdById(id);
        
        // Delete old image if exists
        if (ad.getImageUrl() != null) {
            imageService.deleteImage(ad.getImageUrl());
        }
        
        ad.setImageUrl(imageUrl);
        return productAdRepository.save(ad);
    }
    
    /**
     * Deactivate product ad
     */
    public void deactivateProductAd(UUID id) {
        ProductAd ad = getAdById(id);
        ad.setIsActive(false);
        productAdRepository.save(ad);
    }
    
    /**
     * Delete product ad
     */
    public void deleteProductAd(UUID id) {
        ProductAd ad = getAdById(id);
        
        // Delete image if exists
        if (ad.getImageUrl() != null) {
            imageService.deleteImage(ad.getImageUrl());
        }
        
        productAdRepository.deleteById(id);
    }
}
