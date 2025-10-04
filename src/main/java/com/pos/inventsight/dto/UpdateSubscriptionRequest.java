package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateSubscriptionRequest {
    @NotBlank(message = "Subscription level is required")
    private String subscriptionLevel;
    
    public UpdateSubscriptionRequest() {
    }
    
    public UpdateSubscriptionRequest(String subscriptionLevel) {
        this.subscriptionLevel = subscriptionLevel;
    }
    
    public String getSubscriptionLevel() {
        return subscriptionLevel;
    }
    
    public void setSubscriptionLevel(String subscriptionLevel) {
        this.subscriptionLevel = subscriptionLevel;
    }
}
