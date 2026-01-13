package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Merchant;
import com.pos.inventsight.repository.sql.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MerchantService {
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    /**
     * Create a new merchant
     */
    public Merchant createMerchant(Merchant merchant, Company company, String createdBy) {
        merchant.setCompany(company);
        merchant.setCreatedBy(createdBy);
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchant.setIsActive(true);
        
        return merchantRepository.save(merchant);
    }
    
    /**
     * Get all merchants for a company
     */
    public List<Merchant> getMerchantsByCompany(UUID companyId) {
        return merchantRepository.findByCompanyId(companyId);
    }
    
    /**
     * Get active merchants for a company
     */
    public List<Merchant> getActiveMerchantsByCompany(UUID companyId) {
        return merchantRepository.findActiveByCompanyId(companyId);
    }
    
    /**
     * Get merchant by ID
     */
    public Merchant getMerchantById(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));
    }
    
    /**
     * Update merchant
     */
    public Merchant updateMerchant(UUID id, Merchant merchantDetails, String updatedBy) {
        Merchant merchant = getMerchantById(id);
        
        if (merchantDetails.getName() != null) {
            merchant.setName(merchantDetails.getName());
        }
        if (merchantDetails.getContactPerson() != null) {
            merchant.setContactPerson(merchantDetails.getContactPerson());
        }
        if (merchantDetails.getPhone() != null) {
            merchant.setPhone(merchantDetails.getPhone());
        }
        if (merchantDetails.getEmail() != null) {
            merchant.setEmail(merchantDetails.getEmail());
        }
        if (merchantDetails.getAddress() != null) {
            merchant.setAddress(merchantDetails.getAddress());
        }
        if (merchantDetails.getLocation() != null) {
            merchant.setLocation(merchantDetails.getLocation());
        }
        if (merchantDetails.getNotes() != null) {
            merchant.setNotes(merchantDetails.getNotes());
        }
        
        merchant.setUpdatedBy(updatedBy);
        merchant.setUpdatedAt(LocalDateTime.now());
        
        return merchantRepository.save(merchant);
    }
    
    /**
     * Deactivate merchant
     */
    public void deactivateMerchant(UUID id, String updatedBy) {
        Merchant merchant = getMerchantById(id);
        merchant.setIsActive(false);
        merchant.setUpdatedBy(updatedBy);
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantRepository.save(merchant);
    }
    
    /**
     * Delete merchant permanently
     */
    public void deleteMerchant(UUID id) {
        merchantRepository.deleteById(id);
    }
}
