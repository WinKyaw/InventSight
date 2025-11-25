package com.pos.inventsight.service;

import com.pos.inventsight.dto.SubscriptionInfoResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.SubscriptionLevel;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SubscriptionService {
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Get subscription information for a user
     */
    public SubscriptionInfoResponse getSubscriptionInfo(User user) {
        SubscriptionLevel subscriptionLevel = user.getSubscriptionLevel();
        if (subscriptionLevel == null) {
            subscriptionLevel = SubscriptionLevel.FREE;
        }
        
        long currentUsage = companyStoreUserRepository.countCompaniesByFounder(user);
        
        Integer maxCompanies;
        Integer remaining;
        
        if (subscriptionLevel.isUnlimited()) {
            maxCompanies = null; // null represents unlimited
            remaining = null;
        } else {
            maxCompanies = subscriptionLevel.getMaxCompanies();
            remaining = Math.max(0, maxCompanies - (int) currentUsage);
        }
        
        return new SubscriptionInfoResponse(
            subscriptionLevel.name(),
            maxCompanies,
            currentUsage,
            remaining
        );
    }
    
    /**
     * Update user's subscription level
     */
    public User updateSubscription(Long userId, String subscriptionLevelName) {
        User user = userService.getUserById(userId);
        
        try {
            SubscriptionLevel newLevel = SubscriptionLevel.valueOf(subscriptionLevelName.toUpperCase());
            user.setSubscriptionLevel(newLevel);
            return userService.saveUser(user);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid subscription level: " + subscriptionLevelName);
        }
    }
}
